import { createCanvas, Image, ImageData } from "@napi-rs/canvas";
import * as pdfjsLib from "pdfjs-dist/legacy/build/pdf.mjs";
import * as path from "path";
import * as os from "os";
import * as fs from "fs";

// Polyfill Image and ImageData globals for the Node.js environment
if (typeof global !== "undefined") {
  (global as any).Image = Image;
  (global as any).ImageData = ImageData;
}

/**
 * Converts the first page of a PDF buffer into a PNG image buffer using pdfjs-dist and @napi-rs/canvas.
 * @param pdfBuffer The original PDF file buffer.
 * @param docId Unique identifier to avoid temporary file name collisions.
 */
export async function convertPdfPageToImage(pdfBuffer: Buffer, docId: string): Promise<Buffer> {
  const tempPdfPath = path.join(os.tmpdir(), `pdf_${docId}_${Date.now()}.pdf`);
  await fs.promises.writeFile(tempPdfPath, pdfBuffer);

  try {
    const fileData = await fs.promises.readFile(tempPdfPath);
    const uint8Array = new Uint8Array(fileData);

    const loadingTask = pdfjsLib.getDocument({
      data: uint8Array,
    });

    const pdfDocument = await loadingTask.promise;
    const page = await pdfDocument.getPage(1);

    const scale = 1.5;
    const viewport = page.getViewport({ scale });

    const canvas = createCanvas(viewport.width, viewport.height);
    const context = canvas.getContext("2d");

    await page.render({
      canvasContext: context as any,
      viewport: viewport,
    }).promise;

    return canvas.toBuffer("image/png");
  } finally {
    // Cleanup temporary PDF file
    await fs.promises.unlink(tempPdfPath).catch(() => {});
  }
}
