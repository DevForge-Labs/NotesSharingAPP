import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { generateThumbnailForDocument } from "../thumbnails.js";
import { SearchService } from "../search/SearchService.js";
import { algoliaAdminApiKey } from "../search/SearchConfig.js";

// Notes trigger
export const onNotesCreated = onDocumentCreated({
  document: "notes/{docId}",
  memory: "1GiB",
  secrets: [algoliaAdminApiKey],
}, async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await generateThumbnailForDocument("notes", docId, data);
  await SearchService.indexResource("notes", docId, data);
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
  await generateThumbnailForDocument("pyqs", docId, data);
  await SearchService.indexResource("pyqs", docId, data);
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
  await generateThumbnailForDocument("assignments", docId, data);
  await SearchService.indexResource("assignments", docId, data);
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
  await generateThumbnailForDocument("cheatsheets", docId, data);
  await SearchService.indexResource("cheatsheets", docId, data);
});
