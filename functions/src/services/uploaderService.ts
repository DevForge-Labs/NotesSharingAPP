import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import * as crypto from "crypto";

export async function notifyUploader(
  uploaderUid: string | undefined,
  logId: string,
  resourceTitle: string,
  deletionReason: string,
  resourceId: string | undefined,
  resourceType: string | undefined
) {
  if (uploaderUid) {
    const db = getFirestore();
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
          resourceId: resourceId || null,
          resourceType: resourceType || null,
        }, { merge: true });
      } else {
        logger.info(`Uploader ${uploaderUid} has notifications disabled. Skipping notification.`);
      }
    }
  }
}

export const CONTRIBUTOR_LEVEL_THRESHOLDS = [
  0, // Level 1
  5, // Level 2
  15, // Level 3
  30, // Level 4
  50, // Level 5
];

export function calculateLevel(totalUploads: number): number {
  for (let i = CONTRIBUTOR_LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
    if (totalUploads >= CONTRIBUTOR_LEVEL_THRESHOLDS[i]) {
      return i + 1;
    }
  }
  return 1;
}

export function getUploadTypeField(resourceType: string): string | null {
  const typeStr = resourceType.toLowerCase().replace(/[\s_-]/g, "");
  if (typeStr === "notes") return "notesUploads";
  if (typeStr === "pyq" || typeStr === "pyqs") return "pyqUploads";
  if (typeStr === "assignment" || typeStr === "assignments") return "assignmentUploads";
  if (typeStr === "cheatsheet" || typeStr === "cheatsheets") return "cheatSheetUploads";
  if (typeStr === "video" || typeStr === "videos" || typeStr === "youtube" || typeStr === "youtuberesource") return "youtubeResourceUploads";
  return null;
}

export async function updateUploaderStats(
  uploaderUid: string | undefined,
  resourceType: string,
  resourceId?: string
) {
  if (uploaderUid) {
    const db = getFirestore();

    // 1. Idempotency Check (statsDecremented flag on resource document)
    if (resourceId && resourceType) {
      const resourceRef = db.collection(resourceType).doc(resourceId);
      const alreadyDecremented = await db.runTransaction(async (transaction) => {
        const resourceSnap = await transaction.get(resourceRef);
        if (resourceSnap.exists) {
          const data = resourceSnap.data();
          if (data?.statsDecremented === true) {
            return true; // Already decremented uploader stats for this resource
          }
          // Mark as decremented
          transaction.update(resourceRef, { statsDecremented: true });
        }
        return false;
      });

      if (alreadyDecremented) {
        logger.info(`Stats for resource ${resourceType}/${resourceId} were already decremented. Skipping.`);
        return;
      }
    }

    const uploaderRef = db.collection("users").doc(uploaderUid);
    await db.runTransaction(async (transaction) => {
      const uploaderSnap = await transaction.get(uploaderRef);
      if (uploaderSnap.exists) {
        const uploaderData = uploaderSnap.data();

        // Get mapped field name
        const fieldToDecrement = getUploadTypeField(resourceType);

        const currentTotal = uploaderData?.totalUploads ?? 0;
        const newTotal = Math.max(0, currentTotal - 1);

        const updates: Record<string, any> = {
          totalUploads: newTotal,
          contributorLevel: calculateLevel(newTotal),
        };

        if (fieldToDecrement) {
          const currentVal = uploaderData?.[fieldToDecrement] ?? 0;
          updates[fieldToDecrement] = Math.max(0, currentVal - 1);
        }

        transaction.update(uploaderRef, updates);
        logger.info(`Decremented uploader ${uploaderUid} totalUploads to ${newTotal}. Level: ${updates.contributorLevel}. Field: ${fieldToDecrement}`);
      }
    });
  }
}
