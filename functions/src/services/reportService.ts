import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import * as crypto from "crypto";

export async function notifyReporter(
  triggeredByReport: boolean,
  reporterUid: string | undefined,
  reportNotificationSent: boolean,
  logId: string,
  resourceTitle: string,
  resourceId: string | undefined,
  resourceType: string | undefined,
  reportReason: string | undefined,
  reportId: string | undefined,
  logRef: FirebaseFirestore.DocumentReference
) {
  if (
    triggeredByReport &&
    reporterUid &&
    !reportNotificationSent
  ) {
    const db = getFirestore();
    const reporterDoc = await db.collection("users").doc(reporterUid).get();

    if (reporterDoc.exists) {
      const reporterData = reporterDoc.data();

      const masterEnabled =
        reporterData?.notifications_enabled !== false;

      const personalEnabled =
        reporterData?.pref_notifications_personal !== false;

      if (masterEnabled && personalEnabled) {
        logger.info(
          `Notifying reporter ${reporterUid} that report ${reportId} has been resolved.`
        );

        const reporterNotificationBody =
          `The resource you reported "${resourceTitle}" has been reviewed by our moderation team and has been removed.\n\n` +
          `Reported Reason:\n${reportReason}\n\n` +
          `Thank you for helping keep Campus Pages safe.`;

        const notificationId = crypto
          .createHash("md5")
          .update(`reporter_${logId}_${reporterUid}`)
          .digest("hex");

        await db
          .collection("users")
          .doc(reporterUid)
          .collection("notifications")
          .doc(notificationId)
          .set(
            {
              title: "Report Reviewed",
              body: reporterNotificationBody,
              message: reporterNotificationBody,
              type: "report_resolved_deleted",
              read: false,
              createdAt: FieldValue.serverTimestamp(),
              targetId: resourceId || "",
              targetType: resourceType || "",
              resourceTitle,
              resourceId: resourceId || "",
              resourceType: resourceType || "",
              reportId: reportId || "",
            },
            { merge: true }
          );

        await logRef.update({
          reportNotificationSent: true,
        });
      } else {
        logger.info(
          `Reporter ${reporterUid} has notifications disabled. Skipping reporter notification.`
        );
      }
    }
  }
}

export async function handleReportResolved(
  reportId: string,
  beforeData: any,
  afterData: any
) {
  logger.info(`onReportResolved trigger started for report ${reportId}`);

  const beforeStatus = beforeData.status;
  const afterStatus = afterData.status;
  const actionTaken = afterData.actionTaken;
  const notificationSent = afterData.notificationSent;
  const reporterUid = afterData.reporterUid;

  // 1. Explicitly ignore updates to reports that were already resolved
  if (beforeStatus === "resolved" || afterStatus !== "resolved") {
    return;
  }

  // 2. Check if already processed
  if (notificationSent === true) {
    logger.info(`Report ${reportId} skipped because already processed.`);
    return;
  }

  // 3. Check if action is not dismissed
  if (actionTaken !== "dismissed") {
    logger.info(`Report ${reportId} skipped because action is not dismissed.`);
    return;
  }

  // 4. Check if reporterUid exists
  if (!reporterUid) {
    logger.info(`Report ${reportId} skipped because reporterUid is missing.`);
    return;
  }

  // 5. Ensure notificationSent is false
  if (notificationSent !== false) {
    logger.info(`Report ${reportId} skipped because already processed.`);
    return;
  }

  try {
    const db = getFirestore();

    // Load reporter document
    const reporterDoc = await db.collection("users").doc(reporterUid).get();
    if (!reporterDoc.exists) {
      logger.warn(`User ${reporterUid} not found in Firestore. Skipping notification.`);
      return;
    }

    const reporterData = reporterDoc.data();
    const masterEnabled = reporterData?.notifications_enabled !== false;
    const personalEnabled = reporterData?.pref_notifications_personal !== false;

    if (!masterEnabled || !personalEnabled) {
      logger.info(`Report ${reportId} skipped because preferences disabled.`);
      return;
    }

    // Generate deterministic notification ID
    const notificationId = crypto
      .createHash("md5")
      .update(`reporter_dismissed_${reportId}_${reporterUid}`)
      .digest("hex");

    const resourceId = afterData.resourceId || "";
    const resourceTitle = afterData.resourceTitle || "Unknown Resource";
    const resourceType = afterData.resourceType || "";
    const moderatorMessage = afterData.moderatorMessage || "";

    const notificationBody =
      `Your report regarding "${resourceTitle}" has been reviewed by our moderation team.\n\n` +
      `After careful review, we determined that this resource does not violate the Campus Pages Community Guidelines and will remain available.\n\n` +
      `Moderator's Message:\n${moderatorMessage}\n\n` +
      `Thank you for helping improve Campus Pages.`;

    // Create notification document
    await db
      .collection("users")
      .doc(reporterUid)
      .collection("notifications")
      .doc(notificationId)
      .set(
        {
          title: "Report Reviewed",
          body: notificationBody,
          message: notificationBody,
          type: "report_dismissed",
          read: false,
          createdAt: FieldValue.serverTimestamp(),
          resourceId,
          resourceTitle,
          resourceType,
          reportId,
          actionTaken,
          moderatorMessage,
        },
        { merge: true }
      );

    logger.info(`Report ${reportId} notification created.`);

    // Mark report as notificationSent
    await db.collection("reports").doc(reportId).update({
      notificationSent: true,
    });

    logger.info(`Report ${reportId} marked as notificationSent.`);
  } catch (error) {
    logger.error(`Error processing report resolved trigger for report ${reportId}:`, error);
  }
}
