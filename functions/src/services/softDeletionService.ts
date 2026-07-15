import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { chunkArray } from "../utils/chunkArray.js";
import { updateUploaderStats } from "./uploaderService.js";

export async function handleSoftDeletion(
  collectionName: string,
  docId: string,
  beforeData: any,
  afterData: any
) {
  const wasDeleted = beforeData?.isDeleted === true;
  const isDeleted = afterData?.isDeleted === true;

  // Only trigger once when the document transitions to isDeleted: true
  if (wasDeleted || !isDeleted) {
    return;
  }

  logger.info(`Handling soft deletion for resource in ${collectionName}/${docId}`);

  const db = getFirestore();
  const title = afterData?.title || "Bookmarked Resource";
  const uploaderId = afterData?.uploaderId;

  try {
    // Decrement uploader statistics
    if (uploaderId) {
      await updateUploaderStats(uploaderId, collectionName, docId);
    }

    // 1. Query bookmarks
    const bookmarksSnap = await db.collection("bookmarks")
      .where("documentId", "==", docId)
      .get();

    if (bookmarksSnap.empty) {
      logger.info(`No bookmarks found for document ${docId}.`);
      return;
    }

    // 2. Extract unique user IDs, excluding the uploader
    const userIds = new Set<string>();
    bookmarksSnap.forEach((doc) => {
      const bookmarkData = doc.data();
      const userId = bookmarkData.userId;
      if (userId && userId !== uploaderId) {
        userIds.add(userId);
      }
    });

    if (userIds.size === 0) {
      logger.info(`No bookmark owners to notify for document ${docId}.`);
      return;
    }

    logger.info(`Resolving preferences and notifying ${userIds.size} bookmark owners for document ${docId}`);

    // 3. Fetch user documents in parallel
    const userIdsArray = Array.from(userIds);
    const userDocs = await Promise.all(
      userIdsArray.map(userId => db.collection("users").doc(userId).get())
    );

    // 4. Filter users by notification preference and prepare payloads
    const activeNotificationRequests: { userId: string }[] = [];
    userDocs.forEach((userDoc, index) => {
      const userId = userIdsArray[index];
      if (!userDoc.exists) {
        logger.warn(`User ${userId} not found in Firestore. Skipping.`);
        return;
      }

      const userData = userDoc.data();
      const masterEnabled = userData?.notifications_enabled !== false;
      const personalEnabled = userData?.pref_notifications_personal !== false;

      if (!masterEnabled || !personalEnabled) {
        logger.info(`User ${userId} has disabled notifications (master=${masterEnabled}, personal=${personalEnabled}). Skipping.`);
        return;
      }

      activeNotificationRequests.push({ userId });
    });

    if (activeNotificationRequests.length === 0) {
      logger.info(`No users with enabled notification settings to notify.`);
      return;
    }

    // 5. Batch write notifications (limit of 500 per batch)
    const chunks = chunkArray(activeNotificationRequests, 500);
    for (const chunk of chunks) {
      const batch = db.batch();
      for (const request of chunk) {
        const notificationRef = db.collection("users").doc(request.userId).collection("notifications").doc();
        batch.set(notificationRef, {
          title: "Bookmarked Resource Removed",
          body: `"${title}" was removed by its uploader and is no longer available.`,
          message: `"${title}" was removed by its uploader and is no longer available.`,
          type: "resource_deleted",
          read: false,
          createdAt: FieldValue.serverTimestamp(),
          targetId: null,
          targetType: null,
          // Resource Metadata
          resourceTitle: title,
          resourceId: docId,
          resourceType: collectionName,
        });
      }
      await batch.commit();
      logger.info(`Batch-created notification documents for ${chunk.length} bookmark owners.`);
    }
  } catch (error) {
    logger.error(`Failed to handle soft deletion for document ${docId}:`, error);
  }
}
