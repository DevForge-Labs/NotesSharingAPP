import { onTaskDispatched } from "firebase-functions/v2/tasks";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { logger } from "firebase-functions";
import * as crypto from "crypto";
import * as pdfjsLib from "pdfjs-dist/legacy/build/pdf.mjs";
import { algoliaAdminApiKey } from "../search/SearchConfig.js";
import { generateThumbnailForDocument } from "../thumbnails.js";
import { SearchService } from "../search/SearchService.js";
import { downloadFile } from "../storageUtils.js";

const DEFAULT_GOTENBERG_URL = process.env.GOTENBERG_URL || "http://localhost:3000";

/**
 * Validates that a Buffer is a valid, parseable PDF with at least 1 page.
 */
async function validatePdfBuffer(pdfBuffer: Buffer): Promise<number> {
  if (!pdfBuffer || pdfBuffer.length < 100) {
    throw new Error("Generated PDF is empty or smaller than minimum valid header size.");
  }

  // 1. Check PDF Magic Header (%PDF-)
  const header = pdfBuffer.subarray(0, 5).toString("utf-8");
  if (header !== "%PDF-") {
    throw new Error(`Invalid PDF header: expected '%PDF-', got '${header}'`);
  }

  // 2. Parse PDF with pdfjs-dist using a dedicated buffer copy so internal detachment does not mutate the source
  const bufferCopy = Buffer.from(pdfBuffer);
  const uint8Array = new Uint8Array(bufferCopy.buffer, bufferCopy.byteOffset, bufferCopy.byteLength);
  const loadingTask = pdfjsLib.getDocument({ data: uint8Array });
  const pdfDocument = await loadingTask.promise;

  try {
    const numPages = pdfDocument.numPages;
    if (numPages < 1) {
      throw new Error("Parsed PDF document contains 0 pages.");
    }
    return numPages;
  } finally {
    await pdfDocument.destroy().catch(() => {});
  }
}

/**
 * Converts a PPT/PPTX file buffer to PDF using Gotenberg LibreOffice conversion endpoint.
 */
async function convertPptxWithGotenberg(
  fileBuffer: Buffer,
  fileName: string,
  gotenbergUrl: string = DEFAULT_GOTENBERG_URL
): Promise<Buffer> {
  const endpoint = `${gotenbergUrl.replace(/\/+$/, "")}/forms/libreoffice/convert`;
  
  const formData = new FormData();
  const fileBlob = new Blob([fileBuffer as unknown as BlobPart], {
    type: fileName.endsWith(".ppt")
      ? "application/vnd.ms-powerpoint"
      : "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  });
  
  formData.append("files", fileBlob, fileName);

  logger.info(`Sending conversion request to Gotenberg at ${endpoint} for ${fileName} (${fileBuffer.length} bytes)...`);

  const response = await fetch(endpoint, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => "");
    throw new Error(`Gotenberg conversion failed with HTTP ${response.status}: ${errorText || response.statusText}`);
  }

  const arrayBuffer = await response.arrayBuffer();
  const convertedBuffer = Buffer.from(arrayBuffer);
  if (!convertedBuffer || convertedBuffer.length < 100) {
    throw new Error(`Gotenberg returned an invalid/empty PDF buffer (${convertedBuffer?.length || 0} bytes).`);
  }
  return convertedBuffer;
}

/**
 * Uploads a converted PDF buffer to Firebase Storage with a persistent download token.
 */
async function uploadConvertedPdfToStorage(
  storagePath: string,
  buffer: Buffer
): Promise<string> {
  if (!buffer || buffer.length < 100) {
    throw new Error(`Refusing to upload invalid/empty PDF buffer (${buffer?.length || 0} bytes) to ${storagePath}.`);
  }

  const bucket = getStorage().bucket();
  const file = bucket.file(storagePath);
  const downloadToken = crypto.randomUUID();

  await file.save(buffer, {
    metadata: {
      contentType: "application/pdf",
      metadata: {
        firebaseStorageDownloadTokens: downloadToken,
      },
    },
  });

  return `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(storagePath)}?alt=media&token=${downloadToken}`;
}

/**
 * Cloud Task worker function to convert PPT/PPTX presentations into canonical PDFs.
 */
export const documentConversionTask = onTaskDispatched({
  memory: "1GiB",
  timeoutSeconds: 300,
  secrets: [algoliaAdminApiKey],
  retryConfig: {
    maxAttempts: 3,
    minBackoffSeconds: 10,
    maxBackoffSeconds: 120,
  },
}, async (request) => {
  const { collectionName, docId } = request.data || {};
  if (!collectionName || !docId) {
    logger.error("Missing collectionName or docId in task payload.", request.data);
    return;
  }

  logger.info(`[documentConversionTask] Starting conversion for ${collectionName}/${docId}...`);

  const db = getFirestore();
  const docRef = db.collection(collectionName).doc(docId);
  const docSnap = await docRef.get();

  if (!docSnap.exists) {
    logger.warn(`[documentConversionTask] Document ${collectionName}/${docId} does not exist. Aborting.`);
    return;
  }

  const data = docSnap.data();
  if (!data || data.isDeleted === true) {
    logger.warn(`[documentConversionTask] Document ${collectionName}/${docId} is marked deleted. Aborting.`);
    return;
  }

  // Idempotency: If already READY with valid PDF, do not re-convert
  if (data.processingStatus === "READY" && data.fileExtension === "pdf") {
    logger.info(`[documentConversionTask] Document ${collectionName}/${docId} is already READY. Skipping.`);
    return;
  }

  try {
    // 1. Resolve storage paths (prioritize original presentation paths)
    let storagePaths: string[] = [];
    if (data.originalStoragePaths && Array.isArray(data.originalStoragePaths) && data.originalStoragePaths.length > 0) {
      storagePaths = [...data.originalStoragePaths];
    } else if (data.originalStoragePath) {
      storagePaths = [data.originalStoragePath];
    } else if (data.storagePaths && Array.isArray(data.storagePaths) && data.storagePaths.length > 0) {
      storagePaths = [...data.storagePaths];
    } else if (data.storagePath) {
      storagePaths = [data.storagePath];
    }

    let fileUrls: string[] = [];
    if (data.originalFileUrls && Array.isArray(data.originalFileUrls) && data.originalFileUrls.length > 0) {
      fileUrls = [...data.originalFileUrls];
    } else if (data.originalFileUrl) {
      fileUrls = [data.originalFileUrl];
    } else if (data.fileUrls && Array.isArray(data.fileUrls) && data.fileUrls.length > 0) {
      fileUrls = [...data.fileUrls];
    } else if (data.fileUrl) {
      fileUrls = [data.fileUrl];
    }

    if (storagePaths.length === 0) {
      throw new Error("No storage paths found for presentation document.");
    }

    const updatedStoragePaths: string[] = [...storagePaths];
    const updatedFileUrls: string[] = [...fileUrls];
    let totalPdfSize = 0;

    // 2. Iterate through all attachments and convert PPT/PPTX
    for (let index = 0; index < storagePaths.length; index++) {
      const currentStoragePath = storagePaths[index];
      const cleanPath = currentStoragePath.startsWith("http") && currentStoragePath.includes("/o/")
        ? decodeURIComponent(currentStoragePath.split("/o/")[1].split("?")[0])
        : currentStoragePath;

      const ext = (cleanPath.split("?")[0].split(".").pop() || "").toLowerCase();
      const isPresentation = ext === "ppt" || ext === "pptx";

      if (!isPresentation) {
        logger.info(`Attachment ${index} (${cleanPath}) is not PPT/PPTX. Leaving unchanged.`);
        continue;
      }

      logger.info(`Processing PPT/PPTX attachment ${index}: ${cleanPath}...`);

      // Download original presentation buffer
      const originalBuffer = await downloadFile(cleanPath);
      logger.info(`Downloaded original presentation ${cleanPath} (${originalBuffer.length} bytes).`);

      const parts = cleanPath.split("/");
      const originalFileName = parts.pop() || `presentation.${ext}`;
      const parentFolder = parts.join("/").replace(/\/original$/, ""); // remove /original subfolder if present

      const baseName = originalFileName.includes(".")
        ? originalFileName.substring(0, originalFileName.lastIndexOf("."))
        : originalFileName;

      const canonicalPdfStoragePath = `${parentFolder}/${baseName}.pdf`;

      // Convert using Gotenberg
      const pdfBuffer = await convertPptxWithGotenberg(originalBuffer, originalFileName);
      logger.info(`Gotenberg returned PDF buffer of size ${pdfBuffer.length} bytes for ${originalFileName}.`);

      // Validate converted PDF
      const pageCount = await validatePdfBuffer(pdfBuffer);
      logger.info(`PDF validation succeeded for ${originalFileName}: ${pageCount} pages, ${pdfBuffer.length} bytes.`);

      totalPdfSize += pdfBuffer.length;

      // Upload canonical PDF to Storage
      const canonicalPdfUrl = await uploadConvertedPdfToStorage(canonicalPdfStoragePath, pdfBuffer);
      logger.info(`Uploaded canonical PDF to ${canonicalPdfStoragePath}. URL: ${canonicalPdfUrl}`);

      // Update aligned arrays
      updatedStoragePaths[index] = canonicalPdfStoragePath;
      if (index < updatedFileUrls.length) {
        updatedFileUrls[index] = canonicalPdfUrl;
      } else {
        updatedFileUrls.push(canonicalPdfUrl);
      }
    }

    // 3. Atomically update Firestore document to READY
    const updatePayload: Record<string, any> = {
      processingStatus: "READY",
      fileType: "pdf",
      fileExtension: "pdf",
      mimeType: "application/pdf",
      fileUrl: updatedFileUrls[0] || "",
      downloadUrl: updatedFileUrls[0] || "",
      fileUrls: updatedFileUrls,
      storagePath: updatedStoragePaths[0] || "",
      storagePaths: updatedStoragePaths,
      fileSize: totalPdfSize > 0 ? totalPdfSize : (data.fileSize || 0),
      processedAt: Timestamp.now(),
      processingError: null,
    };

    await docRef.update(updatePayload);
    logger.info(`[documentConversionTask] Updated Firestore document ${collectionName}/${docId} to READY.`);

    // 4. Retrieve refreshed snapshot to run existing downstream pipelines
    const updatedSnap = await docRef.get();
    const updatedData = updatedSnap.data() || { ...data, ...updatePayload };

    // 5. Explicitly run existing thumbnail generation
    try {
      logger.info(`[documentConversionTask] Running thumbnail generation for ${collectionName}/${docId}...`);
      await generateThumbnailForDocument(collectionName, docId, updatedData);
      logger.info(`[documentConversionTask] Thumbnail generation completed for ${collectionName}/${docId}.`);
    } catch (thumbError) {
      logger.error(`[documentConversionTask] Thumbnail generation failed for ${collectionName}/${docId}:`, thumbError);
    }

    // 6. Explicitly run existing Algolia search indexing
    try {
      logger.info(`[documentConversionTask] Running search indexing for ${collectionName}/${docId}...`);
      await SearchService.indexResource(collectionName, docId, updatedData);
      logger.info(`[documentConversionTask] Search indexing completed for ${collectionName}/${docId}.`);
    } catch (searchError) {
      logger.error(`[documentConversionTask] Search indexing failed for ${collectionName}/${docId}:`, searchError);
    }

    logger.info(`[documentConversionTask] Successfully completed all conversion steps for ${collectionName}/${docId}.`);
  } catch (error: any) {
    logger.error(`[documentConversionTask] Conversion failed for ${collectionName}/${docId}:`, error);

    // Update document state to FAILED with error details
    try {
      await docRef.update({
        processingStatus: "FAILED",
        processingError: error?.message || "Presentation conversion failed.",
      });
    } catch (dbError) {
      logger.error(`Failed to update FAILED status on ${collectionName}/${docId}:`, dbError);
    }
  }
});
