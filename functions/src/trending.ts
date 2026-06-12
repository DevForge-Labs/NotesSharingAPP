import { onSchedule } from "firebase-functions/v2/scheduler";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { logger } from "firebase-functions";

/**
 * Scheduled Cloud Function to run every 6 hours and update the trendingScore
 * for all documents in the notes, assignments, pyqs, cheatsheets, and videos collections.
 */
export const updateTrendingScores = onSchedule({
  schedule: "every 6 hours",
  memory: "512MiB",
  timeoutSeconds: 300,
}, async (event) => {
  logger.info("Starting scheduled updateTrendingScores function execution.");
  const db = getFirestore();
  const collections = ["notes", "assignments", "pyqs", "cheatsheets", "videos"];

  for (const col of collections) {
    try {
      logger.info(`Fetching documents from collection: ${col}`);
      const querySnapshot = await db.collection(col).get();
      
      if (querySnapshot.empty) {
        logger.info(`Collection ${col} is empty.`);
        continue;
      }

      logger.info(`Found ${querySnapshot.size} documents in collection ${col}. Processing...`);
      
      let batch = db.batch();
      let count = 0;
      let totalUpdated = 0;

      for (const doc of querySnapshot.docs) {
        try {
          const data = doc.data();
          
          // Defensive check against missing/null fields
          const upvotes = typeof data.upvotes === "number" ? data.upvotes : 0;
          const downloadsCount = typeof data.downloadsCount === "number" ? data.downloadsCount : 0;
          const viewsCount = typeof data.viewsCount === "number" ? data.viewsCount : 0;

          // 1. Calculate raw score
          const rawScore = (upvotes * 3) + (downloadsCount * 2) + (viewsCount * 0.1);

          // 2. Resolve uploaded timestamp
          let uploadedTimeMs = Date.now();
          const uploadedAt = data.uploadedAt;
          
          if (uploadedAt !== undefined && uploadedAt !== null) {
            if (typeof uploadedAt === "number") {
              uploadedTimeMs = uploadedAt;
            } else if (typeof uploadedAt.toDate === "function") {
              uploadedTimeMs = uploadedAt.toDate().getTime();
            } else if (typeof uploadedAt.toMillis === "function") {
              uploadedTimeMs = uploadedAt.toMillis();
            } else if (typeof uploadedAt._seconds === "number") {
              uploadedTimeMs = uploadedAt._seconds * 1000;
            } else if (typeof uploadedAt.seconds === "number") {
              uploadedTimeMs = uploadedAt.seconds * 1000;
            } else {
              const parsed = Date.parse(String(uploadedAt));
              if (!isNaN(parsed)) {
                uploadedTimeMs = parsed;
              }
            }
          }

          // 3. Apply recency decay
          const daysSinceUpload = (Date.now() - uploadedTimeMs) / (1000 * 60 * 60 * 24);
          const daysOld = Math.max(1, isNaN(daysSinceUpload) ? 0 : daysSinceUpload);
          const trendingScore = rawScore / Math.sqrt(daysOld);

          // 4. Queue updates
          batch.update(doc.ref, {
            trendingScore: trendingScore,
            trendingUpdatedAt: FieldValue.serverTimestamp(),
          });

          count++;
          totalUpdated++;

          // Firestore batch write limit is 500 operations
          if (count === 500) {
            await batch.commit();
            logger.info(`Committed batch of 500 updates for collection ${col}.`);
            batch = db.batch();
            count = 0;
          }
        } catch (docError) {
          logger.error(`Error processing document ID ${doc.id} in collection ${col}:`, docError);
        }
      }

      // Commit any remaining updates in the batch
      if (count > 0) {
        await batch.commit();
        logger.info(`Committed final batch of ${count} updates for collection ${col}.`);
      }

      logger.info(`Successfully updated trending scores for ${totalUpdated} documents in collection ${col}.`);
    } catch (colError) {
      logger.error(`Failed to process collection ${col}:`, colError);
    }
  }

  logger.info("Finished scheduled updateTrendingScores function execution.");
});
