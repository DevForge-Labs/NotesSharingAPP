import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions";
import { onRequest } from "firebase-functions/https";

// 1. Initialize Firebase Admin SDK
initializeApp();

// 2. Set global options for Cloud Functions v2
setGlobalOptions({
  maxInstances: 10,
  memory: "512MiB", // Allocate enough memory for image resizing & PDF extraction
});

// 3. Export Firestore triggers
export * from "./firestoreTriggers.js";
export * from "./upvote.js";
export * from "./trending.js";
export { sendTestNotification } from "./notificationService.js";

// 4. Preserve existing test function
export const helloWorld = onRequest((request, response) => {
  const name = request.query.name || "Student";
  response.send(`Hola Bhai ${name} 😭`);
});