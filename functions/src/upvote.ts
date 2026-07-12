import { onCall, HttpsError } from "firebase-functions/v2/https";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

/**
 * Callable function to toggle the upvote status of a document.
 * Expects { documentId, collectionName } in the request data.
 */
export const upvote = onCall({
  memory: "128MiB",
}, async (request) => {
  // 1. Ensure user is authenticated
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated to upvote.");
  }

  const userId = request.auth.uid;
  const { documentId, collectionName } = request.data;

  if (!documentId) {
    throw new HttpsError("invalid-argument", "documentId must be provided.");
  }

  const db = getFirestore();
  const upvoteDocRef = db.collection("upvotes").doc(`${userId}_${documentId}`);

  // Resolve the document reference by searching allowed collections or directly checking the provided one
  let targetDocRef = null;
  const collections = collectionName 
    ? [collectionName] 
    : ["documents", "notes", "pyqs", "assignments", "cheatsheets", "videos"];

  for (const col of collections) {
    const docRef = db.collection(col).doc(documentId);
    const docSnap = await docRef.get();
    if (docSnap.exists) {
      targetDocRef = docRef;
      break;
    }
  }

  if (!targetDocRef) {
    throw new HttpsError("not-found", `Document with ID ${documentId} not found in any collection.`);
  }

  // Use transaction to toggle upvote state and update document's aggregate count safely
  const result = await db.runTransaction(async (transaction) => {
    const upvoteSnap = await transaction.get(upvoteDocRef);
    const targetSnap = await transaction.get(targetDocRef);

    if (!targetSnap.exists) {
      throw new Error("Target document does not exist during upvote transaction.");
    }

    const currentUpvotes = targetSnap.data()?.upvotes || 0;
    const hasUpvoted = upvoteSnap.exists;

    if (hasUpvoted) {
      // User already upvoted -> Remove upvote
      const nextUpvotes = Math.max(0, currentUpvotes - 1);
      transaction.delete(upvoteDocRef);
      transaction.update(targetDocRef, { upvotes: nextUpvotes });
      return { isUpvoted: false, upvotes: nextUpvotes };
    } else {
      // User has not upvoted yet -> Add upvote
      const nextUpvotes = currentUpvotes + 1;
      transaction.set(upvoteDocRef, {
        userId,
        documentId,
        upvotedAt: FieldValue.serverTimestamp(),
      });
      transaction.update(targetDocRef, { upvotes: nextUpvotes });
      return { isUpvoted: true, upvotes: nextUpvotes };
    }
  });

  return result;
});
