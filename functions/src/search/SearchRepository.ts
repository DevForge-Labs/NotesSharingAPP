import { SearchResource } from "./models/SearchResource.js";
import { getAlgoliaClient } from "./AlgoliaClient.js";
import { getSearchConfig } from "./SearchConfig.js";

/**
 * Persistence layer repository for managing resource indexing operations.
 * Completely abstracts search provider implementation (currently Algolia).
 */
export class SearchRepository {
  /**
   * Resolves the underlying search index reference.
   */
  private static getIndex() {
    const config = getSearchConfig();
    const client = getAlgoliaClient();
    return client.initIndex(config.indexName);
  }

  /**
   * Saves or updates a resource in the search index.
   * @param resource The search-compatible resource model.
   */
  static async save(resource: SearchResource): Promise<void> {
    const index = this.getIndex();
    await index.saveObject(resource);
  }

  /**
   * Deletes a resource from the search index by its unique objectID.
   * @param objectID The ID of the resource to delete.
   */
  static async delete(objectID: string): Promise<void> {
    const index = this.getIndex();
    await index.deleteObject(objectID);
  }
}
