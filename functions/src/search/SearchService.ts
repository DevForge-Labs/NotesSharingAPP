import { logger } from "firebase-functions";
import { getFirestore } from "firebase-admin/firestore";
import { SearchMapper } from "./SearchMapper.js";
import { SearchRepository } from "./SearchRepository.js";
import { getSearchConfig } from "./SearchConfig.js";

/**
 * Service orchestrating application search indexing and cleanup tasks.
 * Remains completely provider-agnostic.
 */
export class SearchService {
  /**
   * Orchestrates indexing a Firestore resource document into the search index.
   * Resolves configuration, maps data to search schema, and saves to repository.
   * Catches errors gracefully to avoid blocking Firestore transaction flows.
   */
  static async indexResource(collectionName: string, docId: string, data: any): Promise<void> {
    try {
      const config = getSearchConfig();
      if (!config.appId || !config.adminApiKey) {
        logger.warn("Skipping search indexing: Search configuration parameters are not fully set.", {
          collection: collectionName,
          documentId: docId,
          status: "skipped",
        });
        return;
      }

      let finalData = data;
      const thumbnailUrl = data ? (data.thumbnailUrl || "") : "";
      if (!thumbnailUrl || String(thumbnailUrl).trim() === "") {
        logger.info("thumbnailUrl is null, undefined or blank. Waiting 5 seconds before re-reading document...", {
          collection: collectionName,
          documentId: docId,
        });

        // Wait 5 seconds
        await new Promise((resolve) => setTimeout(resolve, 5000));

        try {
          const db = getFirestore();
          const docSnap = await db.collection(collectionName).doc(docId).get();
          if (docSnap.exists) {
            const refreshedData = docSnap.data();
            if (refreshedData) {
              finalData = refreshedData;
              logger.info("Successfully re-read document from Firestore as single source of truth.", {
                collection: collectionName,
                documentId: docId,
                thumbnailUrlAvailable: !!refreshedData.thumbnailUrl,
              });
            }
          } else {
            logger.warn("Refreshed document does not exist. Proceeding with original data.", {
              collection: collectionName,
              documentId: docId,
            });
          }
        } catch (readError) {
          logger.error("Failed to re-read document from Firestore. Proceeding with original data.", {
            collection: collectionName,
            documentId: docId,
            error: readError instanceof Error ? readError.message : String(readError),
          });
        }
      }

      const resource = {
        ...SearchMapper.toSearchResource(docId, finalData),
        documentType: finalData.documentType ?? null,
        sectionDisplay: finalData.sectionDisplay ?? null,
        examYear: finalData.examYear ?? null,
        examType: finalData.examType ?? null,
        branch: finalData.branch ?? null,
        semester: finalData.semester ?? null,
        college: finalData.college ?? null,
        channelName: finalData.channelName ?? null,
        playlistTitle: finalData.playlistTitle ?? null,
      };
      
      await SearchRepository.save(resource as any);

      logger.info("Successfully indexed resource to search index", {
        collection: collectionName,
        documentId: docId,
        title: resource.title,
        status: "success",
      });
    } catch (error) {
      logger.error("Failed to index resource to search index", {
        collection: collectionName,
        documentId: docId,
        title: data.title || "",
        status: "failure",
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

  /**
   * Orchestrates deleting a Firestore resource document from the search index.
   * Catches errors gracefully to avoid blocking Firestore deletion cascades.
   */
  static async deleteResource(collectionName: string, docId: string): Promise<void> {
    try {
      const config = getSearchConfig();
      if (!config.appId || !config.adminApiKey) {
        logger.warn("Skipping search deletion: Search configuration parameters are not fully set.", {
          collection: collectionName,
          documentId: docId,
          status: "skipped",
        });
        return;
      }

      await SearchRepository.delete(docId);

      logger.info("Successfully deleted resource from search index", {
        collection: collectionName,
        documentId: docId,
        status: "success",
      });
    } catch (error) {
      logger.error("Failed to delete resource from search index", {
        collection: collectionName,
        documentId: docId,
        status: "failure",
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }
}
