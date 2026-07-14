import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import * as crypto from "crypto";

export async function processBookmarks(
  resourceId: string | undefined,
  resourceTitle: string,
  resourceType: string | undefined,
  logId: string,
  deletionReason: string
) {
  if (!resourceId) return;
  const db = getFirestore();

  // 4. Find all bookmark users
  const bookmarksSnap = await db.collection("bookmarks")
    .where("documentId", "==", resourceId)
    .get();

  const bookmarkDocsToDelete: FirebaseFirestore.DocumentSnapshot[] = [];
  const bookmarkUsers = new Set<string>();

  bookmarksSnap.forEach((doc) => {
    bookmarkDocsToDelete.push(doc);
    const bData = doc.data();
    if (bData.userId) {
      bookmarkUsers.add(bData.userId);
    }
  });

  logger.info(`Found ${bookmarkDocsToDelete.length} bookmark references across ${bookmarkUsers.size} users.`);

  if (bookmarkUsers.size > 0) {
    // 5. Notify bookmark users
    const bookmarkUsersArray = Array.from(bookmarkUsers);
    const userDocs = await Promise.all(
      bookmarkUsersArray.map(userId => db.collection("users").doc(userId).get())
    );

    const notifyBody = `A resource you bookmarked has been removed by an administrator.\n\nResource:\n${resourceTitle}\n\nReason:\n${deletionReason}`;
    const notifyTitle = "Bookmarked Resource Removed";
    const baseUniqueId = crypto.createHash("md5")
      .update(`bookmark_${logId}_${resourceId}`)
      .digest("hex");

    const bookmarkNotificationBatch = db.batch();
    let notifyCount = 0;

    userDocs.forEach((uDoc, idx) => {
      const userId = bookmarkUsersArray[idx];
      if (uDoc.exists) {
        const uData = uDoc.data();
        const masterEnabled = uData?.notifications_enabled !== false;
        const personalEnabled = uData?.pref_notifications_personal !== false;

        if (masterEnabled && personalEnabled) {
          const uniqueNotificationId = `${userId}_${baseUniqueId}`;
          const notificationRef = db.collection("users").doc(userId).collection("notifications").doc(uniqueNotificationId);
          
          bookmarkNotificationBatch.set(notificationRef, {
            title: notifyTitle,
            body: notifyBody,
            message: notifyBody,
            type: "resource_deleted",
            read: false,
            createdAt: FieldValue.serverTimestamp(),
            targetId: null,
            targetType: null,
            resourceTitle: resourceTitle,
            resourceId: resourceId,
            resourceType: resourceType || null,
          }, { merge: true });
          
          notifyCount++;
        }
      }
    });

    if (notifyCount > 0) {
      await bookmarkNotificationBatch.commit();
      logger.info(`Created notifications for ${notifyCount} bookmark users.`);
    }

    // 6. Delete bookmark documents & decrement bookmark counts in chunks
    const batchSize = 250;
    for (let i = 0; i < bookmarkDocsToDelete.length; i += batchSize) {
      const chunk = bookmarkDocsToDelete.slice(i, i + batchSize);
      const batch = db.batch();

      for (const doc of chunk) {
        // Delete bookmark document
        batch.delete(doc.ref);

        // Decrement user's bookmark count
        const bData = doc.data();
        const userId = bData?.userId;
        if (userId) {
          const userRef = db.collection("users").doc(userId);
          const userDoc = userDocs.find(ud => ud.id === userId);
          const currentBookmarks = userDoc?.exists ? (userDoc.data()?.bookmarks || 0) : 0;
          const newBookmarks = Math.max(0, currentBookmarks - 1);
          batch.update(userRef, { bookmarks: newBookmarks });
        }
      }

      await batch.commit();
      logger.info(`Batch deleted ${chunk.length} bookmark documents and updated counts.`);
    }
  }
}
