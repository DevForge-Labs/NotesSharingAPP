import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getFunctions } from "firebase-admin/functions";
import { logger } from "firebase-functions";
import { generateThumbnailForDocument } from "../thumbnails.js";
import { SearchService } from "../search/SearchService.js";
import { algoliaAdminApiKey } from "../search/SearchConfig.js";

/**
 * Checks if a resource document is an unconverted presentation requiring background PDF conversion.
 */
function isPresentationResource(data: any): boolean {
  if (!data) return false;
  if (data.processingStatus === "PROCESSING") return true;

  const ext = (data.fileExtension || data.originalFileExtension || "").toLowerCase();
  if (ext === "ppt" || ext === "pptx") return true;

  const paths = [
    data.storagePath,
    data.originalStoragePath,
    ...(Array.isArray(data.storagePaths) ? data.storagePaths : []),
    ...(Array.isArray(data.originalStoragePaths) ? data.originalStoragePaths : []),
  ];

  return paths.some((p) => typeof p === "string" && (p.endsWith(".ppt") || p.endsWith(".pptx")));
}

/**
 * Centralized resource creation handler.
 * Dispatches PPT/PPTX to Cloud Tasks conversion worker; runs fast-path thumbnails + search for PDF/images.
 */
async function handleResourceCreation(collectionName: string, docId: string, data: any) {
  if (isPresentationResource(data)) {
    logger.info(`Detected PPT/PPTX presentation for ${collectionName}/${docId}. Enqueuing conversion task...`);
    try {
      const queue = getFunctions().taskQueue("documentConversionTask");
      await queue.enqueue({ collectionName, docId });
      logger.info(`Successfully enqueued documentConversionTask for ${collectionName}/${docId}.`);
    } catch (enqueueError) {
      logger.error(`Failed to enqueue documentConversionTask for ${collectionName}/${docId}:`, enqueueError);
    }
  } else {
    // Existing Fast Path for PDFs and Images (100% behaviorally unchanged)
    await generateThumbnailForDocument(collectionName, docId, data);
    await SearchService.indexResource(collectionName, docId, data);
  }
}

// Notes trigger
export const onNotesCreated = onDocumentCreated({
  document: "notes/{docId}",
  memory: "1GiB",
  secrets: [algoliaAdminApiKey],
}, async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await handleResourceCreation("notes", docId, data);
});

// PYQs trigger
export const onPyqsCreated = onDocumentCreated({
  document: "pyqs/{docId}",
  memory: "512MiB",
  secrets: [algoliaAdminApiKey],
}, async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await handleResourceCreation("pyqs", docId, data);
});

// Assignments trigger
export const onAssignmentsCreated = onDocumentCreated({
  document: "assignments/{docId}",
  memory: "512MiB",
  secrets: [algoliaAdminApiKey],
}, async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await handleResourceCreation("assignments", docId, data);
});

// Cheatsheets trigger
export const onCheatsheetsCreated = onDocumentCreated({
  document: "cheatsheets/{docId}",
  memory: "512MiB",
  secrets: [algoliaAdminApiKey],
}, async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await handleResourceCreation("cheatsheets", docId, data);
});

