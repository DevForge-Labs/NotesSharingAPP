import { defineString, defineSecret } from "firebase-functions/params";

// Define Firebase configuration parameters/secrets for search integration
export const algoliaAppId = defineString("ALGOLIA_APP_ID");
export const algoliaAdminApiKey = defineSecret("ALGOLIA_ADMIN_API_KEY");
export const algoliaIndexName = defineString("ALGOLIA_INDEX_NAME", { default: "resources" });

export interface SearchConfig {
  appId: string;
  adminApiKey: string;
  indexName: string;
}

/**
 * Resolves the configuration settings for the search service.
 */
export function getSearchConfig(): SearchConfig {
  return {
    appId: algoliaAppId.value(),
    adminApiKey: algoliaAdminApiKey.value(),
    indexName: algoliaIndexName.value(),
  };
}
