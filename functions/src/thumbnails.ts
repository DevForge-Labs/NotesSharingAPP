import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { downloadFile, uploadThumbnail } from "./storageUtils.js";
import { convertPdfPageToImage } from "./pdfUtils.js";
import { resizeAndCompressImageToJpeg, resizeAndCompressImageToWebp } from "./imageUtils.js";

/**
 * Main orchestrator to generate a thumbnail for a Firestore document.
 * @param collectionName The target Firestore collection (e.g. "notes").
 * @param docId The ID of the document.
 * @param data The data of the created document.
 */
export async function generateThumbnailForDocument(
  collectionName: string,
  docId: string,
  data: any
) {
  // 2. Resolve storage paths
  let storagePaths: string[] = [];
  if (data.storagePaths && Array.isArray(data.storagePaths) && data.storagePaths.length > 0) {
    storagePaths = data.storagePaths;
  } else if (data.storagePath) {
    storagePaths = [data.storagePath];
  } else if (data.fileUrl) {
    storagePaths = [data.fileUrl];
  } else if (data.fileUrls && Array.isArray(data.fileUrls) && data.fileUrls.length > 0) {
    storagePaths = data.fileUrls;
  }

  if (storagePaths.length === 0) {
    logger.warn(`Document ${docId} has no storagePath, fileUrl, fileUrls, or storagePaths. Cannot generate thumbnail.`);
    return;
  }

  // 1. Skip if thumbnail is already generated to prevent loop/duplicate execution
  if (data.thumbnailGenerated === true) {
    const fileCount = data.fileUrls && Array.isArray(data.fileUrls) ? data.fileUrls.length : storagePaths.length;
    const hasAllThumbnails = data.thumbnailUrls && Array.isArray(data.thumbnailUrls) && data.thumbnailUrls.length >= fileCount;
    if (hasAllThumbnails || fileCount <= 1) {
      logger.info(`Document ${docId} already has all thumbnails generated. Skipping.`);
      return;
    }
  }

  logger.info(`Generating thumbnails for document ${docId} in collection ${collectionName} with storagePaths:`, storagePaths);

  try {
    const thumbnailUrls: string[] = [];
    let firstThumbnailType: string | null = null;

    for (let index = 0; index < storagePaths.length; index++) {
      const storagePath = storagePaths[index];
      const fileExtension = (storagePath.split("?")[0].split(".").pop() || "").toLowerCase();
      const isPdf = fileExtension === "pdf";
      const isImage = ["jpg", "jpeg", "png", "webp", "gif"].includes(fileExtension);

      if (!isPdf && !isImage) {
        logger.info(`Attachment at index ${index} (${storagePath}) has unsupported type. Skipping.`);
        thumbnailUrls.push("");
        continue;
      }

      try {
        let cleanPath = storagePath;
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
          if (storagePath.includes("/o/")) {
            const encodedPath = storagePath.split("/o/")[1].split("?")[0];
            cleanPath = decodeURIComponent(encodedPath);
          } else {
            logger.warn(`Skipping HTTP URL download via Storage bucket: ${storagePath}`);
            thumbnailUrls.push("");
            continue;
          }
        }

        const originalBuffer = await downloadFile(cleanPath);
        logger.info(`Downloaded attachment ${index} of size ${originalBuffer.length} bytes`);

        let thumbnailBuffer: Buffer;
        let thumbnailStoragePath: string;
        let contentType: string;

        const parts = cleanPath.split("/");
        const originalFileName = parts.pop();
        const parentFolder = parts.join("/");

        if (isPdf) {
          logger.info(`Processing PDF attachment ${index}: extracting page 1...`);
          const page1ImageBuffer = await convertPdfPageToImage(originalBuffer, docId);
          logger.info(`Compressing PDF preview ${index} to WebP...`);
          thumbnailBuffer = await resizeAndCompressImageToWebp(page1ImageBuffer);
          contentType = "image/webp";

          const fileNameWithoutExt = originalFileName && originalFileName.includes(".")
            ? originalFileName.substring(0, originalFileName.lastIndexOf("."))
            : `file_${index}`;

          if (collectionName === "pyqs") {
            thumbnailStoragePath = `${parentFolder}/thumbnails/${fileNameWithoutExt}.webp`;
          } else {
            thumbnailStoragePath = `${parentFolder}/thumbnails/${fileNameWithoutExt}_thumb.webp`;
          }

          if (index === 0) {
            firstThumbnailType = "PDF";
          }
        } else {
          logger.info(`Processing image attachment ${index}: compressing to JPEG...`);
          thumbnailBuffer = await resizeAndCompressImageToJpeg(originalBuffer);
          contentType = "image/jpeg";

          const fileNameWithoutExt = originalFileName && originalFileName.includes(".")
            ? originalFileName.substring(0, originalFileName.lastIndexOf("."))
            : `file_${index}`;

          if (collectionName === "pyqs") {
            thumbnailStoragePath = `${parentFolder}/thumbnails/${fileNameWithoutExt}.jpg`;
          } else {
            thumbnailStoragePath = `${parentFolder}/thumbnails/${fileNameWithoutExt}_thumb.jpg`;
          }

          if (index === 0) {
            firstThumbnailType = "IMAGE";
          }
        }

        logger.info(`Uploading thumbnail for attachment ${index} to ${thumbnailStoragePath}...`);
        const thumbnailUrl = await uploadThumbnail(thumbnailStoragePath, thumbnailBuffer, contentType);
        logger.info(`Uploaded thumbnail for attachment ${index}. URL: ${thumbnailUrl}`);

        thumbnailUrls.push(thumbnailUrl);
      } catch (err) {
        logger.error(`Error generating thumbnail for attachment ${index} (${storagePath}):`, err);
        thumbnailUrls.push("");
      }
    }

    if (thumbnailUrls.length === 0) {
      logger.warn(`No thumbnails were generated for document ${docId}.`);
      return;
    }

    // 6. Update Firestore document with thumbnail metadata
    const db = getFirestore();
    const docRef = db.collection(collectionName).doc(docId);

    const updateData: any = {
      thumbnailUrl: thumbnailUrls[0] || "",
      thumbnailUrls: thumbnailUrls,
      thumbnailGenerated: true,
      previewGeneratedAt: Timestamp.now(),
    };

    if (firstThumbnailType) {
      updateData.thumbnailType = firstThumbnailType;
      updateData.previewAttachmentType = firstThumbnailType;
    }

    await docRef.update(updateData);
    logger.info(`Successfully updated document ${docId} in ${collectionName} with multiple thumbnail metadata.`);
  } catch (error) {
    logger.error(`Error generating thumbnail for document ${docId} in collection ${collectionName}:`, error);
  }
}
