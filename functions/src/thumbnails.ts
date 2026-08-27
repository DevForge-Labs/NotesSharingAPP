import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { downloadFile, uploadThumbnail, getFileSize, downloadFileToPath } from "./storageUtils.js";
import { convertPdfPageToImage } from "./pdfUtils.js";
import { resizeAndCompressImageToJpeg, resizeAndCompressImageToWebp } from "./imageUtils.js";
import * as fs from "fs";
import * as path from "path";
import * as os from "os";

const LARGE_FILE_THRESHOLD_BYTES = 100 * 1024 * 1024; // 100 MB

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

      let tempFilePath: string | null = null;
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

        const fileSize = await getFileSize(cleanPath);
        logger.info(`Attachment ${index} size: ${fileSize} bytes`);

        let originalBuffer: Buffer | null = null;
        const isLargePdf = isPdf && (fileSize > LARGE_FILE_THRESHOLD_BYTES);

        if (isLargePdf) {
          tempFilePath = path.join(os.tmpdir(), `pdf_${docId}_${index}.pdf`);
          logger.info(`Downloading large PDF to temporary file: ${tempFilePath}`);
          await downloadFileToPath(cleanPath, tempFilePath);
        } else {
          originalBuffer = await downloadFile(cleanPath);
          logger.info(`Downloaded attachment ${index} of size ${originalBuffer.length} bytes`);
        }

        let thumbnailBuffer: Buffer | null = null;
        let thumbnailStoragePath: string;
        let contentType: string;

        const parts = cleanPath.split("/");
        const originalFileName = parts.pop();
        const parentFolder = parts.join("/");

        if (isPdf) {
          logger.info(`Processing PDF attachment ${index}: extracting page 1...`);
          const pdfSource = isLargePdf ? tempFilePath! : originalBuffer!;
          const page1ImageBuffer = await convertPdfPageToImage(pdfSource, docId);
          
          // Release original buffer as early as possible
          originalBuffer = null;

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
          thumbnailBuffer = await resizeAndCompressImageToJpeg(originalBuffer!);
          
          // Release original buffer as early as possible
          originalBuffer = null;
          
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
        const thumbnailUrl = await uploadThumbnail(thumbnailStoragePath, thumbnailBuffer!, contentType);
        logger.info(`Uploaded thumbnail for attachment ${index}. URL: ${thumbnailUrl}`);

        thumbnailUrls.push(thumbnailUrl);
        
        // Release thumbnail buffer
        thumbnailBuffer = null;
      } catch (err) {
        logger.error(`Error generating thumbnail for attachment ${index} (${storagePath}):`, err);
        thumbnailUrls.push("");
      } finally {
        if (tempFilePath && fs.existsSync(tempFilePath)) {
          try {
            fs.unlinkSync(tempFilePath);
            logger.info(`Deleted temporary file: ${tempFilePath}`);
          } catch (cleanupErr) {
            logger.error(`Failed to delete temporary file ${tempFilePath}:`, cleanupErr);
          }
        }
      }
    }

    const hasValidThumbnail = thumbnailUrls.some(url => Boolean(url && url.trim().length > 0));
    if (!hasValidThumbnail) {
      logger.warn(`No valid thumbnails were generated for document ${docId}.`);
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
