import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import * as crypto from "crypto";

export interface NotificationInput {
  title: string;
  body: string;
  type: string;
  data?: Record<string, any>;
  deepLink?: string;
}



/**
 * Splits an array into smaller chunks of a specified size.
 */
function chunkArray<T>(array: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
}

/**
 * Writes a notification document into: users/{uid}/notifications/{notificationId}
 * This serves as the primary source of truth for the notification center.
 */
export async function createNotification(
  uid: string,
  notification: NotificationInput
): Promise<FirebaseFirestore.DocumentReference> {
  const db = getFirestore();

  // Generate unique ID for deduplication if not explicitly provided
  let uniqueId = notification.data?.notificationId || notification.data?.notification_id;
  if (!uniqueId) {
    uniqueId = crypto.createHash("md5")
      .update(`${notification.title}_${notification.body}_${notification.type}_${notification.deepLink || ""}`)
      .digest("hex");
  }

  const notificationData = {
    title: notification.title,
    body: notification.body,
    type: notification.type,
    createdAt: FieldValue.serverTimestamp(),
    read: false,
    ...(notification.data ? { data: notification.data } : {}),
    ...(notification.deepLink ? { deepLink: notification.deepLink } : {}),
  };

  const docRef = db.collection("users").doc(uid).collection("notifications").doc(uniqueId);
  await docRef.set(notificationData, { merge: true });
  logger.info(`Notification document created/merged for user ${uid} with ID: ${uniqueId}`);
  return docRef;
}

/**
 * Writes notification documents in batch (up to 500 per batch) to multiple users.
 */
export async function createNotificationsForUsers(
  uids: string[],
  notification: NotificationInput
): Promise<void> {
  if (uids.length === 0) return;

  const db = getFirestore();
  const chunks = chunkArray(uids, 500);

  let baseUniqueId = notification.data?.notificationId || notification.data?.notification_id;
  if (!baseUniqueId) {
    baseUniqueId = crypto.createHash("md5")
      .update(`${notification.title}_${notification.body}_${notification.type}_${notification.deepLink || ""}`)
      .digest("hex");
  }

  for (const chunk of chunks) {
    const batch = db.batch();
    for (const uid of chunk) {
      const uniqueId = `${uid}_${baseUniqueId}`;
      const docRef = db.collection("users").doc(uid).collection("notifications").doc(uniqueId);
      batch.set(docRef, {
        title: notification.title,
        body: notification.body,
        type: notification.type,
        createdAt: FieldValue.serverTimestamp(),
        read: false,
        ...(notification.data ? { data: notification.data } : {}),
        ...(notification.deepLink ? { deepLink: notification.deepLink } : {}),
      }, { merge: true });
    }
    await batch.commit();
    logger.info(`Batch-created/merged notification documents for ${chunk.length} users.`);
  }
}

/**
 * Sends a push notification to a single user.
 * Writes to the user's notification database subcollection first.
 */
export async function sendNotificationToUser(
  uid: string,
  title: string,
  body: string,
  data?: Record<string, any>,
  type = "general",
  deepLink?: string
): Promise<boolean> {
  // Database persistence is our source of truth.
  // The Firestore trigger onNotificationCreated will handle the FCM push.
  try {
    await createNotification(uid, { title, body, type, data, deepLink });
    return true;
  } catch (dbError) {
    logger.error(`Database write failed for user ${uid} notification:`, dbError);
    return false;
  }
}

/**
 * Sends push notifications to multiple users.
 * Writes database entries for all users in batch, then sends multicast FCM pushes.
 */
export async function sendNotificationToUsers(
  uids: string[],
  title: string,
  body: string,
  data?: Record<string, any>,
  type = "general",
  deepLink?: string
): Promise<{ successCount: number; failureCount: number }> {
  if (uids.length === 0) {
    logger.info("No recipient UIDs provided. Skipping.");
    return { successCount: 0, failureCount: 0 };
  }

  // Database persistence is our source of truth.
  // The Firestore trigger onNotificationCreated will handle the FCM push for each user.
  try {
    await createNotificationsForUsers(uids, { title, body, type, data, deepLink });
    return { successCount: uids.length, failureCount: 0 };
  } catch (dbError) {
    logger.error("Database batch write failed for multiple user notifications:", dbError);
    return { successCount: 0, failureCount: uids.length };
  }
}

/**
 * Sends notifications to users matching exact branch and semester.
 */
export async function sendNotificationToBranchSemester(
  branch: string,
  semester: string,
  title: string,
  body: string,
  data?: Record<string, any>,
  type = "general",
  deepLink?: string
): Promise<{ successCount: number; failureCount: number }> {
  try {
    const db = getFirestore();

    logger.info(`Querying users for branch: ${branch} and semester: ${semester}`);
    const snapshot = await db
      .collection("users")
      .where("branch", "==", branch)
      .where("semester", "==", semester)
      .get();

    if (snapshot.empty) {
      logger.info(`No users found matching branch: ${branch} and semester: ${semester}.`);
      return { successCount: 0, failureCount: 0 };
    }

    const uids: string[] = [];
    snapshot.forEach((doc) => {
      uids.push(doc.id);
    });

    logger.info(`Resolved ${uids.length} matching users. Delegating to sendNotificationToUsers.`);
    return await sendNotificationToUsers(uids, title, body, data, type, deepLink);
  } catch (error) {
    logger.error(`Error in sendNotificationToBranchSemester (Branch: ${branch}, Semester: ${semester}):`, error);
    return { successCount: 0, failureCount: 0 };
  }
}

/**
 * Reusable Notification Service API namespace export.
 */
export const NotificationService = {
  sendNotificationToUser,
  sendNotificationToUsers,
  sendNotificationToBranchSemester,
  createNotification,
  createNotificationsForUsers,
};


