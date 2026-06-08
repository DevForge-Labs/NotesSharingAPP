import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions";
import { onCall, HttpsError } from "firebase-functions/v2/https";

export interface NotificationInput {
  title: string;
  body: string;
  type: string;
  data?: Record<string, any>;
  deepLink?: string;
}

/**
 * Safely sanitizes the payload data object.
 * FCM requires all values in the data dictionary to be strings.
 */
function sanitizeData(data?: Record<string, any>): Record<string, string> | undefined {
  if (!data) return undefined;
  const sanitized: Record<string, string> = {};
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null) {
      sanitized[key] = typeof value === "object" ? JSON.stringify(value) : String(value);
    }
  }
  return sanitized;
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
  const notificationData = {
    title: notification.title,
    body: notification.body,
    type: notification.type,
    createdAt: FieldValue.serverTimestamp(),
    read: false,
    ...(notification.data ? { data: notification.data } : {}),
    ...(notification.deepLink ? { deepLink: notification.deepLink } : {}),
  };

  const docRef = await db.collection("users").doc(uid).collection("notifications").add(notificationData);
  logger.info(`Notification document created for user ${uid} with ID: ${docRef.id}`);
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

  for (const chunk of chunks) {
    const batch = db.batch();
    for (const uid of chunk) {
      const docRef = db.collection("users").doc(uid).collection("notifications").doc();
      batch.set(docRef, {
        title: notification.title,
        body: notification.body,
        type: notification.type,
        createdAt: FieldValue.serverTimestamp(),
        read: false,
        ...(notification.data ? { data: notification.data } : {}),
        ...(notification.deepLink ? { deepLink: notification.deepLink } : {}),
      });
    }
    await batch.commit();
    logger.info(`Batch-created notification documents for ${chunk.length} users.`);
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
  // 1. Database persistence (Source of truth)
  try {
    await createNotification(uid, { title, body, type, data, deepLink });
  } catch (dbError) {
    logger.error(`Database write failed for user ${uid} notification, proceeding with FCM:`, dbError);
  }

  // 2. Fetch FCM Token and send push
  try {
    const db = getFirestore();
    const userDoc = await db.collection("users").doc(uid).get();
    if (!userDoc.exists) {
      logger.warn(`User ${uid} not found in Firestore. Skipping push notification.`);
      return false;
    }

    const token = userDoc.data()?.fcmToken;
    if (!token) {
      logger.info(`User ${uid} does not have an fcmToken. Skipping push notification.`);
      return false;
    }

    const message = {
      token,
      notification: {
        title,
        body,
      },
      data: sanitizeData(data),
    };

    const response = await getMessaging().send(message);
    logger.info(`FCM notification successfully sent to user ${uid}. MessageID: ${response}`);
    return true;
  } catch (fcmError) {
    logger.error(`Failed to deliver push notification to user ${uid}:`, fcmError);
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

  // 1. Database persistence (Source of truth)
  try {
    await createNotificationsForUsers(uids, { title, body, type, data, deepLink });
  } catch (dbError) {
    logger.error("Database batch write failed for multiple user notifications, proceeding with FCM:", dbError);
  }

  // 2. Multicast FCM push
  try {
    const db = getFirestore();
    const tokens: string[] = [];

    // Fetch user documents in chunks of 1000 (limit of db.getAll)
    const userRefs = uids.map((uid) => db.collection("users").doc(uid));
    const refChunks = chunkArray(userRefs, 1000);
    const userDocs: FirebaseFirestore.DocumentSnapshot[] = [];

    for (const chunk of refChunks) {
      const docs = await db.getAll(...chunk);
      userDocs.push(...docs);
    }

    for (const doc of userDocs) {
      if (doc.exists) {
        const token = doc.data()?.fcmToken;
        if (token) {
          tokens.push(token);
        } else {
          logger.info(`User ${doc.id} does not have an fcmToken. Skipping push.`);
        }
      } else {
        logger.warn(`User document ${doc.id} not found in Firestore. Skipping push.`);
      }
    }

    if (tokens.length === 0) {
      logger.info("No valid FCM tokens resolved. Skipping FCM multicast.");
      return { successCount: 0, failureCount: 0 };
    }

    // Send in chunks of 500 (FCM multicast limit)
    const tokenChunks = chunkArray(tokens, 500);
    let totalSuccess = 0;
    let totalFailure = 0;

    const messaging = getMessaging();
    const sanitizedData = sanitizeData(data);

    for (const chunk of tokenChunks) {
      const response = await messaging.sendEachForMulticast({
        tokens: chunk,
        notification: {
          title,
          body,
        },
        data: sanitizedData,
      });

      totalSuccess += response.successCount;
      totalFailure += response.failureCount;

      logger.info(`FCM multicast chunk status: Success: ${response.successCount}, Failure: ${response.failureCount}`);

      response.responses.forEach((resp, idx) => {
        if (!resp.success) {
          logger.error(`FCM deliver failed for token ${chunk[idx]}:`, resp.error);
        }
      });
    }

    return { successCount: totalSuccess, failureCount: totalFailure };
  } catch (error) {
    logger.error("Error in sendNotificationToUsers:", error);
    // Don't fail the entire process if multicast compilation crashes
    return { successCount: 0, failureCount: 0 };
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

/**
 * Callable test function: sendTestNotification
 * Accepts `uid` in request data and triggers test notification flow.
 */
export const sendTestNotification = onCall(async (request) => {
  // Ensure user triggering this function is authenticated
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be authenticated to trigger test notification.");
  }

  const targetUid = request.data?.uid;
  if (!targetUid || typeof targetUid !== "string") {
    throw new HttpsError("invalid-argument", "The 'uid' field must be provided as a string.");
  }

  logger.info(`Triggering test notification for target UID: ${targetUid}`);

  const success = await sendNotificationToUser(
    targetUid,
    "NotesSharing Test",
    "Notifications are working successfully.",
    undefined,
    "test"
  );

  if (!success) {
    throw new HttpsError(
      "internal",
      `Failed to send push notification to user ${targetUid}. Check Firebase Function logs for details.`
    );
  }

  return {
    success: true,
    message: `Test notification sent successfully to user ${targetUid}`,
  };
});
