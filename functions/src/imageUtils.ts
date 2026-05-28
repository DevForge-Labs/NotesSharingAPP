import sharp from "sharp";

/**
 * Resizes an image buffer to fit inside 500x500 pixels, compresses it to 80% quality,
 * and transcodes it to progressive JPEG.
 * Used for image uploads to keep existing flow untouched.
 * @param imageBuffer The original image buffer.
 */
export async function resizeAndCompressImageToJpeg(imageBuffer: Buffer): Promise<Buffer> {
  return sharp(imageBuffer)
    .resize({
      width: 500,
      height: 500,
      fit: "inside",
      withoutEnlargement: true,
    })
    .jpeg({
      quality: 80,
      progressive: true,
    })
    .toBuffer();
}

/**
 * Resizes an image buffer to fit inside 500x500 pixels, compresses it to 80% quality,
 * and transcodes it to WebP.
 * Used for PDF page rasterized previews.
 * @param imageBuffer The original image buffer.
 */
export async function resizeAndCompressImageToWebp(imageBuffer: Buffer): Promise<Buffer> {
  return sharp(imageBuffer)
    .resize({
      width: 500,
      height: 500,
      fit: "inside",
      withoutEnlargement: true,
    })
    .webp({
      quality: 80,
    })
    .toBuffer();
}
