## 2025-05-25 - [Direct Matrix Canvas Scaling for Watermarking]
**Learning:** Calling `Bitmap.createScaledBitmap` creates a full secondary ARGB bitmap allocation in memory, causing memory pressure and potential GC pauses on large images. Applying scaling directly through a `Matrix` passed to `Canvas.drawBitmap(bitmap, matrix, paint)` performs the transformation in a single pass without allocating intermediate bitmaps.
**Action:** When performing image scaling alongside translation and rotation on Android Canvas, always combine operations into a single `Matrix` transformation instead of creating intermediate scaled `Bitmap` objects.

## 2025-05-26 - [No-Compression Zip Level for Pre-Compressed PNG Streams]
**Learning:** `Bitmap.compress(Bitmap.CompressFormat.PNG, ...)` outputs PNG data that is already losslessly compressed using zlib DEFLATE. Writing these bytes directly into a default `ZipOutputStream` forces a redundant second DEFLATE compression pass that consumes heavy CPU time with ~0% file size savings.
**Action:** When creating ZIP archives containing pre-compressed formats like PNGs, always set `zipOut.setLevel(Deflater.NO_COMPRESSION)` to bypass the redundant DEFLATE pass and optimize throughput.

## 2025-05-27 - [Bitmap Downsampling via inSampleSize for Watermark Overlays]
**Learning:** Decoding full-resolution watermark image overlays into RAM before scaling on Canvas allocates massive unscaled ARGB_8888 Bitmaps (e.g., 48MB for 12MP images) that trigger heavy GC pressure and OOM risk. Inspecting dimensions first with `inJustDecodeBounds = true` and applying a calculated power-of-two `inSampleSize` downsamples image pixels during decoding, reducing memory overhead by up to 98% with zero visual quality loss.
**Action:** Always inspect bitmap dimensions with `inJustDecodeBounds = true` and compute an appropriate `inSampleSize` when loading image overlays or watermarks before drawing them onto Canvas.
