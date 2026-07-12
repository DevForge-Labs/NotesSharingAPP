import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onTaskDispatched } from "firebase-functions/v2/tasks";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getFunctions } from "firebase-admin/functions";
import { logger } from "firebase-functions";
import { SearchService } from "./search/SearchService.js";
import { algoliaAdminApiKey } from "./search/SearchConfig.js";

/**
 * Callable Cloud Function that enqueues a delayed task to repair a video's search index.
 * It is called by the client app right after video document creation.
 */
export const enqueueVideoIndexing = onCall(async (request) => {
  const docId = request.data.docId;
  if (!docId || typeof docId !== "string") {
    throw new HttpsError("invalid-argument", "The function must be called with a string 'docId'.");
  }

  try {
    const queue = getFunctions().taskQueue("repairVideoSearchIndexTask");
    await queue.enqueue(
      { docId },
      {
        scheduleTime: new Date(Date.now() + 60 * 1000), // 60 seconds delay
      }
    );
    logger.info(`Successfully enqueued delayed search indexing task for video ${docId}.`);
    return { success: true };
  } catch (error) {
    logger.error("Failed to enqueue delayed search indexing task:", error);
    throw new HttpsError("internal", "Failed to enqueue delayed search indexing task.");
  }
});

/**
 * Task Queue Cloud Function that runs 60 seconds after a video is created.
 * Checks if searchIndexed is false, indexes the video, and sets searchIndexed = true.
 */
export const repairVideoSearchIndexTask = onTaskDispatched({
  memory: "256MiB",
  secrets: [algoliaAdminApiKey],
  retryConfig: {
    maxAttempts: 1, // Let trendingScore cron rescue on persistent failure
  },
}, async (request) => {
  const docId = request.data.docId;
  if (!docId) {
    logger.error("No docId provided in task request.");
    return;
  }

  const db = getFirestore();
  const docRef = db.collection("videos").doc(docId);
  const docSnap = await docRef.get();

  if (!docSnap.exists) {
    logger.warn(`Video document ${docId} does not exist. Skipping delayed indexing.`);
    return;
  }

  const data = docSnap.data();
  if (data && data.searchIndexed === false) {
    logger.info(`Starting delayed indexing repair task for video ${docId}.`);
    
    // Call SearchService.indexResource
    const success = await SearchService.indexResource("videos", docId, data);
    
    if (success) {
      await docRef.update({
        searchIndexed: true,
        searchIndexedAt: FieldValue.serverTimestamp(),
        needsSearchIndexing: false,
      });
      logger.info(`Successfully repaired video search index and updated Firestore for video ${docId}.`);
    } else {
      logger.error("VIDEO_INDEX_FAILED", {
        docId,
        reason: "SearchService.indexResource returned false",
      });
    }
  } else {
    logger.info(`Video document ${docId} is already indexed or does not require repair.`);
  }
});
