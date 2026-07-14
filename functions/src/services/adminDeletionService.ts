import { logger } from "firebase-functions";
import { SearchService } from "../search/SearchService.js";

export async function deleteFromSearch(
  resourceId: string | undefined,
  resourceType: string | undefined,
  logId: string
) {
  if (resourceId && resourceType) {
    try {
      logger.info(`Propagating deletion to Algolia for resource ${resourceId} of type ${resourceType}`);
      await SearchService.deleteResource(resourceType, resourceId);
    } catch (algoliaError) {
      logger.error(`Failed to propagate deletion to Algolia for resource ${resourceId}:`, algoliaError);
    }
  } else {
    logger.warn(`Skipping Algolia deletion: resourceId or resourceType is missing in deletion log ${logId}`);
  }
}

export async function markDeletionProcessed(
  logRef: FirebaseFirestore.DocumentReference,
  logId: string
) {
  await logRef.update({ notificationSent: true });
  logger.info(`Successfully completed administrative deletion cascade for log ${logId}.`);
}
