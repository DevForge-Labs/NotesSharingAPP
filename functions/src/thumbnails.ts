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
  // 1. Skip if thumbnail is already generated to prevent loop/duplicate execution
  if (data.thumbnailGenerated === true) {
    logger.info(`Document ${docId} already has thumbnail generated. Skipping.`);
    return;
  }

  // 2. Resolve storage path
  const storagePath = data.storagePath || data.fileUrl;
  if (!storagePath) {
    logger.warn(`Document ${docId} has no storagePath or fileUrl. Cannot generate thumbnail.`);
    return;
  }

  logger.info(`Generating thumbnail for document ${docId} in collection ${collectionName} with storagePath ${storagePath}`);

  try {
    // 3. Determine file type (PDF vs Image)
    const fileExtension = (data.fileExtension || storagePath.split(".").pop() || "").toLowerCase();
    const isPdf = fileExtension === "pdf" || data.fileType === "pdf";

    // 4. Download original file bytes
    const originalBuffer = await downloadFile(storagePath);
    logger.info(`Downloaded file of size ${originalBuffer.length} bytes`);

    let thumbnailBuffer: Buffer;
    let thumbnailStoragePath: string;
    let contentType: string;

    const parts = storagePath.split("/");
    const originalFileName = parts.pop();
    const parentFolder = parts.join("/");

    if (isPdf) {
      // PDF Flow: Pure JS conversion to WebP thumbnail
      logger.info("Processing PDF document: extracting page 1 using pdfjs-dist...");
      const page1ImageBuffer = await convertPdfPageToImage(originalBuffer, docId);
      logger.info("Compressing extracted PDF preview to WebP...");
      thumbnailBuffer = await resizeAndCompressImageToWebp(page1ImageBuffer);
      contentType = "image/webp";

      // Naming format for PDFs:
      // pyqs: pyqs/Semester 4/thumbnails/coa-pyq-uuid.webp (to avoid collision in shared parent folder)
      // others: notes/doc-folder/thumbnail.webp (directly under doc-folder)
      if (collectionName === "pyqs" && originalFileName) {
        const fileNameWithoutExt = originalFileName.includes(".")
          ? originalFileName.substring(0, originalFileName.lastIndexOf("."))
          : originalFileName;
        thumbnailStoragePath = `${parentFolder}/thumbnails/${fileNameWithoutExt}.webp`;
      } else {
        thumbnailStoragePath = `${parentFolder}/thumbnail.webp`;
      }
    } else {
      // Image Flow: Unchanged flow to progressive JPEG thumbnail
      logger.info("Processing image document: compressing image to JPEG...");
      thumbnailBuffer = await resizeAndCompressImageToJpeg(originalBuffer);
      contentType = "image/jpeg";

      if (collectionName === "pyqs" && originalFileName) {
        const fileNameWithoutExt = originalFileName.includes(".")
          ? originalFileName.substring(0, originalFileName.lastIndexOf("."))
          : originalFileName;
        thumbnailStoragePath = `${parentFolder}/thumbnails/${fileNameWithoutExt}.jpg`;
      } else {
        thumbnailStoragePath = `${parentFolder}/thumbnails/preview.jpg`;
      }
    }

    // 5. Upload thumbnail and get the public download URL
    logger.info(`Uploading thumbnail to ${thumbnailStoragePath} with content type ${contentType}...`);
    const thumbnailUrl = await uploadThumbnail(thumbnailStoragePath, thumbnailBuffer, contentType);
    logger.info(`Uploaded thumbnail. URL: ${thumbnailUrl}`);

    // 6. Update Firestore document with thumbnail metadata
    const db = getFirestore();
    const docRef = db.collection(collectionName).doc(docId);

    await docRef.update({
      thumbnailUrl: thumbnailUrl,
      thumbnailGenerated: true,
      thumbnailType: isPdf ? "PDF" : "IMAGE",
      previewAttachmentType: isPdf ? "PDF" : "IMAGE",
      previewGeneratedAt: Timestamp.now(),
    });

    logger.info(`Successfully updated document ${docId} in ${collectionName} with thumbnail metadata.`);
  } catch (error) {
    logger.error(`Error generating thumbnail for document ${docId} in collection ${collectionName}:`, error);
  }
}
