import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions";
import * as crypto from "crypto";
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

// Centralized FCM delivery trigger
export const onNotificationCreated = onDocumentCreated({
  document: "users/{userId}/notifications/{notificationId}",
  memory: "512MiB",
}, async (event) => {
  const userId = event.params.userId;
  const notificationId = event.params.notificationId;
  const data = event.data?.data();
  if (!data) return;

  const db = getFirestore();
  try {
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      logger.warn(`User ${userId} not found in Firestore. Skipping push notification.`);
      return;
    }

    const token = userDoc.data()?.fcmToken;
    if (!token) {
      logger.info(`User ${userId} does not have an fcmToken. Skipping push notification.`);
      return;
    }

    const title = data.title || "NotesSharing Update";
    const body = data.body || data.message || "";
    const payloadData = data.data || {};
    
    // Ensure all values are strings for FCM
    const sanitizedData: Record<string, string> = {
      type: data.type || "general",
      notificationId: notificationId,
    };
    for (const [key, value] of Object.entries(payloadData)) {
      if (value !== undefined && value !== null) {
        sanitizedData[key] = typeof value === "object" ? JSON.stringify(value) : String(value);
      }
    }
    if (data.deepLink) {
      sanitizedData.deepLink = data.deepLink;
    }

    const message = {
      token,
      notification: {
        title,
        body,
      },
      data: sanitizedData,
    };

    const response = await getMessaging().send(message);
    logger.info(`FCM notification successfully sent to user ${userId}. MessageID: ${response}`);
  } catch (error) {
    logger.error(`Failed to deliver push notification for notification ${notificationId} to user ${userId}:`, error);
  }
});

// Helper to chunk array for batches
function chunkArray<T>(array: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
}

async function handleSoftDeletion(
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

// Notes trigger
export const onNotesUpdated = onDocumentUpdated("notes/{docId}", async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("notes", docId, beforeData, afterData);
});

// PYQs trigger
export const onPyqsUpdated = onDocumentUpdated("pyqs/{docId}", async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("pyqs", docId, beforeData, afterData);
});

// Assignments trigger
export const onAssignmentsUpdated = onDocumentUpdated("assignments/{docId}", async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("assignments", docId, beforeData, afterData);
});

// Cheatsheets trigger
export const onCheatsheetsUpdated = onDocumentUpdated("cheatsheets/{docId}", async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("cheatsheets", docId, beforeData, afterData);
});

// Videos trigger
export const onVideosUpdated = onDocumentUpdated("videos/{docId}", async (event) => {
  const docId = event.params.docId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();
  if (!beforeData || !afterData) return;
  await handleSoftDeletion("videos", docId, beforeData, afterData);
});

// Administrative Deletion Cascade Trigger
export const onAdminDeletionLogCreated = onDocumentCreated({
  document: "admin_deletion_logs/{logId}",
  memory: "512MiB",
}, async (event) => {
  const logId = event.params.logId;
  const logData = event.data?.data();
  if (!logData) return;

  // 1. Check if already processed
  if (logData.notificationSent === true) {
    logger.info(`Admin deletion log ${logId} already processed. Skipping.`);
    return;
  }

  const db = getFirestore();
  const logRef = db.collection("admin_deletion_logs").doc(logId);

  const resourceId = logData.resourceId;
  const resourceTitle = logData.resourceTitle || "Unknown Resource";
  const resourceType = logData.resourceType || "";
  const deletionReason = logData.deletionReason || "Administrative Action";
  const uploaderUid = logData.uploaderUid;

  logger.info(`Processing administrative deletion cascade for log ${logId}. Resource: ${resourceTitle} (${resourceType}, ${resourceId})`);

  try {
    // 2. Notify the uploader
    if (uploaderUid) {
      const uploaderDoc = await db.collection("users").doc(uploaderUid).get();
      if (uploaderDoc.exists) {
        const uploaderData = uploaderDoc.data();
        const masterEnabled = uploaderData?.notifications_enabled !== false;
        const personalEnabled = uploaderData?.pref_notifications_personal !== false;

        if (masterEnabled && personalEnabled) {
          logger.info(`Notifying uploader ${uploaderUid} of resource removal.`);
          const uploaderNotificationBody = `Your resource "${resourceTitle}" was removed by an administrator.\n\nReason:\n${deletionReason}`;
          
          // Use a stable, unique notification ID to avoid duplication on retries
          const notificationId = crypto.createHash("md5")
            .update(`uploader_${logId}_${uploaderUid}`)
            .digest("hex");

          await db.collection("users").doc(uploaderUid).collection("notifications").doc(notificationId).set({
            title: "Resource Removed",
            body: uploaderNotificationBody,
            message: uploaderNotificationBody,
            type: "resource_deleted",
            read: false,
            createdAt: FieldValue.serverTimestamp(),
            targetId: null,
            targetType: null,
            resourceTitle: resourceTitle,
            resourceId: resourceId,
            resourceType: resourceType,
          }, { merge: true });
        } else {
          logger.info(`Uploader ${uploaderUid} has notifications disabled. Skipping notification.`);
        }
      }
    }

    // 3. Update uploader upload counts and contributor level
    if (uploaderUid) {
      const uploaderRef = db.collection("users").doc(uploaderUid);
      await db.runTransaction(async (transaction) => {
        const uploaderSnap = await transaction.get(uploaderRef);
        if (uploaderSnap.exists) {
          const uploaderData = uploaderSnap.data();
          
          // Map resource type to profile fields
          const typeStr = resourceType.toLowerCase().replace(/[\s_-]/g, "");
          const fieldsToDecrement: string[] = [];
          if (typeStr === "notes") {
            fieldsToDecrement.push("notesUploads", "notesUploaded");
          } else if (typeStr === "pyq" || typeStr === "pyqs") {
            fieldsToDecrement.push("pyqUploads");
          } else if (typeStr === "assignment" || typeStr === "assignments") {
            fieldsToDecrement.push("assignmentUploads");
          } else if (typeStr === "cheatsheet" || typeStr === "cheatsheets") {
            fieldsToDecrement.push("cheatSheetUploads");
          } else if (typeStr === "video" || typeStr === "videos" || typeStr === "youtube" || typeStr === "youtuberesource") {
            fieldsToDecrement.push("youtubeUploads");
          }

          const currentUploads = uploaderData?.uploads || 0;
          const newUploads = Math.max(0, currentUploads - 1);

          const updates: Record<string, any> = {
            uploads: newUploads,
          };

          for (const field of fieldsToDecrement) {
            const currentVal = uploaderData?.[field] || 0;
            updates[field] = Math.max(0, currentVal - 1);
          }

          // Recalculate level
          let newLevel = 1;
          if (newUploads >= 50) {
            newLevel = 5;
          } else if (newUploads >= 30) {
            newLevel = 4;
          } else if (newUploads >= 15) {
            newLevel = 3;
          } else if (newUploads >= 5) {
            newLevel = 2;
          }
          updates.contributorLevel = newLevel;

          transaction.update(uploaderRef, updates);
          logger.info(`Decremented uploader ${uploaderUid} uploads to ${newUploads}. Level: ${newLevel}. Fields: ${fieldsToDecrement.join(", ")}`);
        }
      });
    }

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
              resourceType: resourceType,
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

    // 7. Mark deletion log processed
    await logRef.update({ notificationSent: true });
    logger.info(`Successfully completed administrative deletion cascade for log ${logId}.`);
  } catch (error) {
    logger.error(`Error processing administrative deletion cascade for log ${logId}:`, error);
    throw error;
  }
});
