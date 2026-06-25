import algoliasearch, { SearchClient } from "algoliasearch";
import { getSearchConfig } from "./SearchConfig.js";

let clientInstance: SearchClient | null = null;

/**
 * Initializes and returns a singleton instance of the Algolia SearchClient.
 */
export function getAlgoliaClient(): SearchClient {
  if (!clientInstance) {
    const config = getSearchConfig();
    if (!config.appId || !config.adminApiKey) {
      throw new Error("Algolia credentials are not fully configured in environment.");
    }
    
    // Typecast to handle default export with esModuleInterop
    const algoliaFactory = (algoliasearch as any).default || algoliasearch;
    clientInstance = algoliaFactory(config.appId, config.adminApiKey);
  }
  return clientInstance!;
}
