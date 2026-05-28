import {setGlobalOptions} from "firebase-functions";
import {onRequest} from "firebase-functions/https";


// Global settings
setGlobalOptions({
  maxInstances: 10,
});

// Test function
export const helloWorld = onRequest((request, response) => {
  const name = request.query.name || "Student";

  response.send(`Hello bhai ${name} 😭`);
});