import { SearchResource } from "./models/SearchResource.js";

export class SearchMapper {
  /**
   * Maps a Firestore document data map to the SearchResource interface.
   * @param docId The Firestore document ID.
   * @param data The Firestore document data.
   */
  static toSearchResource(docId: string, data: any): SearchResource {
    return {
      objectID: docId,
      title: data.title || "",
      displaySubject: data.displaySubject || data.subject || "",
      subject: data.subject || "",
      searchKey: data.searchKey || "",
      description: data.description || "",
      documentType: data.documentType || data.type || "",
      trendingScore: typeof data.trendingScore === "number" ? data.trendingScore : 0,
      thumbnailUrl: data.thumbnailUrl || "",
      uploadedAt: this.resolveTimestamp(data.uploadedAt),
    };
  }

  /**
   * Safely resolves a timestamp value from Firestore data (could be number, Timestamp, etc.)
   * to epoch milliseconds.
   */
  private static resolveTimestamp(uploadedAt: any): number {
    if (uploadedAt === undefined || uploadedAt === null) {
      return Date.now();
    }
    if (typeof uploadedAt === "number") {
      return uploadedAt;
    }
    if (typeof uploadedAt.toDate === "function") {
      return uploadedAt.toDate().getTime();
    }
    if (typeof uploadedAt.toMillis === "function") {
      return uploadedAt.toMillis();
    }
    if (typeof uploadedAt._seconds === "number") {
      return uploadedAt._seconds * 1000;
    }
    if (typeof uploadedAt.seconds === "number") {
      return uploadedAt.seconds * 1000;
    }
    const parsed = Date.parse(String(uploadedAt));
    return isNaN(parsed) ? Date.now() : parsed;
  }
}
