import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { algoliaAdminApiKey } from "../search/SearchConfig.js";
import { deleteFromSearch, markDeletionProcessed } from "../services/adminDeletionService.js";
import { notifyUploader, updateUploaderStats } from "../services/uploaderService.js";
import { notifyReporter } from "../services/reportService.js";
import { processBookmarks } from "../services/bookmarkService.js";

// Administrative Deletion Cascade Trigger
export const onAdminDeletionLogCreated = onDocumentCreated({
  document: "admin_deletion_logs/{logId}",
  memory: "512MiB",
  secrets: [algoliaAdminApiKey],
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
  const triggeredByReport = logData.triggeredByReport === true;
  const reporterUid = logData.reporterUid;
  const reportReason = logData.reportReason;
  const reportNotificationSent = logData.reportNotificationSent === true;
  const reportId = logData.reportId;
  logger.info(`Processing administrative deletion cascade for log ${logId}. Resource: ${resourceTitle} (${resourceType}, ${resourceId})`);

  try {
    // 1.5. Propagate deletion to Algolia search index
    await deleteFromSearch(resourceId, resourceType, logId);

    // 2. Notify the uploader
    await notifyUploader(uploaderUid, logId, resourceTitle, deletionReason, resourceId, resourceType);

    // 3. Update uploader upload counts and contributor level
    await updateUploaderStats(uploaderUid, resourceType, resourceId);

    // 3.5 Notify the reporter (only for report-based deletions)
    await notifyReporter(triggeredByReport, reporterUid, reportNotificationSent, logId, resourceTitle, resourceId, resourceType, reportReason, reportId, logRef);

    // 4. Find all bookmark users, notify them, delete bookmark docs, and update bookmark counts
    await processBookmarks(resourceId, resourceTitle, resourceType, logId, deletionReason);

    // 7. Mark deletion log processed
    await markDeletionProcessed(logRef, logId);
  } catch (error) {
    logger.error(`Error processing administrative deletion cascade for log ${logId}:`, error);
    throw error;
  }
});
