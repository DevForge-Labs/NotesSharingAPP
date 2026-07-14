import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { handleReportResolved } from "../services/reportService.js";

// Report Resolved Trigger for Dismissed Reports
export const onReportResolved = onDocumentUpdated({
  document: "reports/{reportId}",
  memory: "512MiB",
}, async (event) => {
  const reportId = event.params.reportId;
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();

  if (!beforeData || !afterData) {
    return;
  }

  await handleReportResolved(reportId, beforeData, afterData);
});
