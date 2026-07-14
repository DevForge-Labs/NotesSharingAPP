import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { SearchService } from "../search/SearchService.js";
import { algoliaAdminApiKey } from "../search/SearchConfig.js";
import { handleSoftDeletion } from "../services/softDeletionService.js";

// Notes trigger
export const onNotesUpdated = onDocumentUpdated({
  document: "notes/{docId}",
  secrets: [algoliaAdminApiKey],
  memory: "512MiB",
}, async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("notes", docId, beforeData, afterData);
  if (SearchService.shouldReindex(beforeData, afterData)) {
    await SearchService.indexResource("notes", docId, afterData);
  }
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
  await handleSoftDeletion("pyqs", docId, beforeData, afterData);
  if (SearchService.shouldReindex(beforeData, afterData)) {
    await SearchService.indexResource("pyqs", docId, afterData);
  }
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
  await handleSoftDeletion("assignments", docId, beforeData, afterData);
  if (SearchService.shouldReindex(beforeData, afterData)) {
    await SearchService.indexResource("assignments", docId, afterData);
  }
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
  await handleSoftDeletion("cheatsheets", docId, beforeData, afterData);
  if (SearchService.shouldReindex(beforeData, afterData)) {
    await SearchService.indexResource("cheatsheets", docId, afterData);
  }
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
