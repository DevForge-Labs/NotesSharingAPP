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

export async function updateUploaderStats(uploaderUid: string | undefined, resourceType: string) {
  if (uploaderUid) {
    const db = getFirestore();
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
}
