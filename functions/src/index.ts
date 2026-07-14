import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions";

// 1. Initialize Firebase Admin SDK
initializeApp();

// 2. Set global options for Cloud Functions v2
setGlobalOptions({
  maxInstances: 10,
  memory: "512MiB", // Allocate enough memory for image resizing & PDF extraction
});

// 3. Export Firestore triggers
export * from "./triggers/resourceCreate.js";
export * from "./triggers/resourceUpdate.js";
export * from "./triggers/notificationTrigger.js";
export * from "./triggers/adminDeletionTrigger.js";
export * from "./triggers/reportTrigger.js";
export * from "./upvote.js";
export * from "./trending.js";
export * from "./videoRepair.js";