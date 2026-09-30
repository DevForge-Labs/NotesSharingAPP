import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { getFunctions } from "firebase-admin/functions";
import { logger } from "firebase-functions";
import { generateThumbnailForDocument } from "../thumbnails.js";
import { SearchService } from "../search/SearchService.js";
import { algoliaAdminApiKey } from "../search/SearchConfig.js";
import { handleSoftDeletion } from "../services/softDeletionService.js";

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
 * Centralized resource update handler.
 * Handles soft deletion, presentation conversion / thumbnail generation for updated attachments, and search reindexing.
 */
async function handleResourceUpdate(collectionName: string, docId: string, beforeData: any, afterData: any) {
  await handleSoftDeletion(collectionName, docId, beforeData, afterData);

  // If attachments were updated or thumbnails are pending generation on an active document
  if (!afterData.isDeleted && afterData.thumbnailGenerated === false) {
    if (isPresentationResource(afterData)) {
      logger.info(`Detected PPT/PPTX presentation for updated ${collectionName}/${docId}. Enqueuing conversion task...`);
      try {
        const queue = getFunctions().taskQueue("documentConversionTask");
        await queue.enqueue({ collectionName, docId });
        logger.info(`Successfully enqueued documentConversionTask for ${collectionName}/${docId}.`);
      } catch (enqueueError) {
        logger.error(`Failed to enqueue documentConversionTask for ${collectionName}/${docId}:`, enqueueError);
      }
    } else {
      await generateThumbnailForDocument(collectionName, docId, afterData);
    }
  }

  if (SearchService.shouldReindex(beforeData, afterData)) {
    await SearchService.indexResource(collectionName, docId, afterData);
  }
}

// Notes trigger
export const onNotesUpdated = onDocumentUpdated({
  document: "notes/{docId}",
  secrets: [algoliaAdminApiKey],
  memory: "1GiB",
}, async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleResourceUpdate("notes", docId, beforeData, afterData);
});

// PYQs trigger
export const onPyqsUpdated = onDocumentUpdated({
  document: "pyqs/{docId}",
  secrets: [algoliaAdminApiKey],
  memory: "512MiB",
}, async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleResourceUpdate("pyqs", docId, beforeData, afterData);
});

// Assignments trigger
export const onAssignmentsUpdated = onDocumentUpdated({
  document: "assignments/{docId}",
  secrets: [algoliaAdminApiKey],
  memory: "512MiB",
}, async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleResourceUpdate("assignments", docId, beforeData, afterData);
});

// Cheatsheets trigger
export const onCheatsheetsUpdated = onDocumentUpdated({
  document: "cheatsheets/{docId}",
  secrets: [algoliaAdminApiKey],
  memory: "512MiB",
}, async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleResourceUpdate("cheatsheets", docId, beforeData, afterData);
});

// Videos trigger
export const onVideosUpdated = onDocumentUpdated({
  document: "videos/{docId}",
  secrets: [algoliaAdminApiKey],
  memory: "512MiB",
}, async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("videos", docId, beforeData, afterData);
  if (SearchService.shouldReindex(beforeData, afterData)) {
    await SearchService.indexResource("videos", docId, afterData);
  }
});
