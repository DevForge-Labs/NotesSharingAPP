import { createCanvas, Image, ImageData } from "@napi-rs/canvas";
import * as pdfjsLib from "pdfjs-dist/legacy/build/pdf.mjs";
import { pathToFileURL } from "url";

// Polyfill Image and ImageData globals for the Node.js environment
if (typeof global !== "undefined") {
  (global as any).Image = Image;
  (global as any).ImageData = ImageData;
}

/**
 * Converts the first page of a PDF buffer or local file path into a PNG image buffer using pdfjs-dist and @napi-rs/canvas.
 * @param pdfSource The original PDF file buffer or local file path.
 * @param docId Unique identifier to avoid temporary file name collisions.
 */
export async function convertPdfPageToImage(pdfSource: Buffer | string, docId: string): Promise<Buffer> {
  let loadingTask;
  if (typeof pdfSource === "string") {
    const fileUrl = pdfSource.startsWith("file://")
      ? pdfSource
      : pathToFileURL(pdfSource).href;
    loadingTask = pdfjsLib.getDocument({
      url: fileUrl,
    });
  } else {
    // Directly load the PDF from memory using a zero-copy Uint8Array view of the Buffer
    const uint8Array = new Uint8Array(pdfSource.buffer, pdfSource.byteOffset, pdfSource.byteLength);
    loadingTask = pdfjsLib.getDocument({
      data: uint8Array,
    });
  }

  const pdfDocument = await loadingTask.promise;
  try {
    const page = await pdfDocument.getPage(1);

    // Calculate dynamic scale so rendered width is approximately 500px
    const baseViewport = page.getViewport({ scale: 1 });
    const desiredWidth = 500;
    const scale = baseViewport.width > 0 ? desiredWidth / baseViewport.width : 1.0;
    const viewport = page.getViewport({ scale });

    const canvas = createCanvas(viewport.width, viewport.height);
    const context = canvas.getContext("2d");

    await page.render({
      canvasContext: context as any,
      viewport: viewport,
    }).promise;

    // Explicitly release page-specific resources if supported
    page.cleanup?.();

    return canvas.toBuffer("image/png");
  } finally {
    // Explicitly release PDF document resources
    await pdfDocument.destroy().catch(() => {});
  }
}
