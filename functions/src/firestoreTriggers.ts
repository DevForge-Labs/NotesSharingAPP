import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { generateThumbnailForDocument } from "./thumbnails.js";

// Notes trigger
export const onNotesCreated = onDocumentCreated({
  document: "notes/{docId}",
  memory: "1GiB",
}, async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await generateThumbnailForDocument("notes", docId, data);
});

// PYQs trigger
export const onPyqsCreated = onDocumentCreated("pyqs/{docId}", async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await generateThumbnailForDocument("pyqs", docId, data);
});

// Assignments trigger
export const onAssignmentsCreated = onDocumentCreated("assignments/{docId}", async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await generateThumbnailForDocument("assignments", docId, data);
});

// Cheatsheets trigger
export const onCheatsheetsCreated = onDocumentCreated("cheatsheets/{docId}", async (event) => {
  const docId = event.params.docId;
  const data = event.data?.data();
  if (!data) return;
  await generateThumbnailForDocument("cheatsheets", docId, data);
});
