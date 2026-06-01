import { onDocumentCreated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const db = getFirestore();
const collections = ["documents", "notes", "pyqs", "assignments", "cheatsheets"];

/**
 * Searches the note collections to find which one contains the note with the given documentId.
 */
async function findCollectionPath(documentId: string): Promise<string | null> {
  const results = await Promise.all(
    collections.map(async (col) => {
      const docRef = db.collection(col).doc(documentId);
      const snap = await docRef.get();
      return { col, exists: snap.exists };
    })
  );
  const found = results.find((r) => r.exists);
  return found ? found.col : null;
}

export const onBookmarkCreated = onDocumentCreated("bookmarks/{bookmarkId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  const documentId = data.documentId;
  if (!documentId) return;

  const colName = await findCollectionPath(documentId);
  if (!colName) {
    console.error(`Document with ID ${documentId} not found in any notes collections.`);
    return;
  }

  const docRef = db.collection(colName).doc(documentId);
  await docRef.update({
    bookmarks: FieldValue.increment(1),
  });
});

export const onBookmarkDeleted = onDocumentDeleted("bookmarks/{bookmarkId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  const documentId = data.documentId;
  if (!documentId) return;

  const colName = await findCollectionPath(documentId);
  if (!colName) {
    console.error(`Document with ID ${documentId} not found in any notes collections.`);
    return;
  }

  const docRef = db.collection(colName).doc(documentId);
  await db.runTransaction(async (transaction) => {
    const snap = await transaction.get(docRef);
    if (snap.exists) {
      const currentBookmarks = snap.data()?.bookmarks || 0;
      const newBookmarks = Math.max(0, currentBookmarks - 1);
      transaction.update(docRef, { bookmarks: newBookmarks });
    }
  });
});
