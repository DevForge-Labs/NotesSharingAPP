/**
 * Interface representing a lightweight searchable resource record in the search index.
 * Keeps only searchable fields and minimal metadata needed to render search results.
 */
export interface SearchResource {
  objectID: string; // Firestore document ID used as Algolia record ID
  title: string;
  displaySubject: string;
  subject: string;
  searchKey: string;
  description: string;
  documentType: string; // Resource type label (e.g. "Notes", "PYQ")
  trendingScore: number;
  thumbnailUrl: string;
  uploadedAt: number; // Epoch milliseconds timestamp
}
