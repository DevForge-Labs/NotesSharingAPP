import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions";

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

    const title = data.title || "Campus Pages Update";
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
      data: {
        ...sanitizedData,
        title,
        body,
      },
      android: {
        priority: "high" as const,
      },
    };

    const response = await getMessaging().send(message);
    logger.info(`FCM notification successfully sent to user ${userId}. MessageID: ${response}`);
  } catch (error) {
    logger.error(`Failed to deliver push notification for notification ${notificationId} to user ${userId}:`, error);
  }
});
