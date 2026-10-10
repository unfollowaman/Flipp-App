## 2025-05-25 - [Direct Matrix Canvas Scaling for Watermarking]
**Learning:** Calling `Bitmap.createScaledBitmap` creates a full secondary ARGB bitmap allocation in memory, causing memory pressure and potential GC pauses on large images. Applying scaling directly through a `Matrix` passed to `Canvas.drawBitmap(bitmap, matrix, paint)` performs the transformation in a single pass without allocating intermediate bitmaps.
**Action:** When performing image scaling alongside translation and rotation on Android Canvas, always combine operations into a single `Matrix` transformation instead of creating intermediate scaled `Bitmap` objects.

## 2025-05-26 - [No-Compression Zip Level for Pre-Compressed PNG Streams]
**Learning:** `Bitmap.compress(Bitmap.CompressFormat.PNG, ...)` outputs PNG data that is already losslessly compressed using zlib DEFLATE. Writing these bytes directly into a default `ZipOutputStream` forces a redundant second DEFLATE compression pass that consumes heavy CPU time with ~0% file size savings.
**Action:** When creating ZIP archives containing pre-compressed formats like PNGs, always set `zipOut.setLevel(Deflater.NO_COMPRESSION)` to bypass the redundant DEFLATE pass and optimize throughput.

## 2025-05-27 - [Downsampling Watermark Decodes with inSampleSize]
**Learning:** Decoding high-resolution watermark images directly into memory decodes millions of pixels into ARGB_8888 bitmap arrays (e.g. 48MB RAM for a 12MP image) before Matrix canvas scaling. First reading dimensions with `inJustDecodeBounds = true` and setting `inSampleSize` to downsample decoding to target dimensions eliminates up to 95%+ of RAM allocations during bitmap decoding.
**Action:** Before decoding image overlays or watermarks from Uri/InputStream, always read image bounds first with `inJustDecodeBounds = true` to calculate and apply `inSampleSize` downsampling.
