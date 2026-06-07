import { getStorage } from "firebase-admin/storage";
import * as crypto from "crypto";

/**
 * Downloads a file from the default Firebase Storage bucket into memory as a Buffer.
 * @param storagePath The path to the file in the bucket.
 */
export async function downloadFile(storagePath: string): Promise<Buffer> {
  const bucket = getStorage().bucket();
  const file = bucket.file(storagePath);
  const [buffer] = await file.download();
  return buffer;
}

/**
 * Gets the size of a file in Firebase Storage in bytes.
 * @param storagePath The path to the file in the bucket.
 */
export async function getFileSize(storagePath: string): Promise<number> {
  const bucket = getStorage().bucket();
  const file = bucket.file(storagePath);
  const [metadata] = await file.getMetadata();
  const size = metadata.size;
  if (typeof size === "number") {
    return size;
  }
  return parseInt(size || "0", 10);
}

/**
 * Downloads a file from Firebase Storage directly to a local file path.
 * @param storagePath The path to the file in the bucket.
 * @param localPath The local destination path.
 */
export async function downloadFileToPath(storagePath: string, localPath: string): Promise<void> {
  const bucket = getStorage().bucket();
  const file = bucket.file(storagePath);
  await file.download({ destination: localPath });
}

/**
 * Uploads a compressed thumbnail buffer to Firebase Storage, sets the public access token,
 * and returns the persistent download URL.
 * @param thumbnailPath The path where the thumbnail will be saved.
 * @param buffer The compressed image buffer.
 */
export async function uploadThumbnail(thumbnailPath: string, buffer: Buffer, contentType: string): Promise<string> {
  const bucket = getStorage().bucket();
  const file = bucket.file(thumbnailPath);
  const downloadToken = crypto.randomUUID();

  await file.save(buffer, {
    metadata: {
      contentType: contentType,
      metadata: {
        firebaseStorageDownloadTokens: downloadToken,
      },
    },
  });

  return `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(thumbnailPath)}?alt=media&token=${downloadToken}`;
}
