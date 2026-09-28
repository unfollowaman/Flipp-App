## 2025-05-25 - [Direct Matrix Canvas Scaling for Watermarking]
**Learning:** Calling `Bitmap.createScaledBitmap` creates a full secondary ARGB bitmap allocation in memory, causing memory pressure and potential GC pauses on large images. Applying scaling directly through a `Matrix` passed to `Canvas.drawBitmap(bitmap, matrix, paint)` performs the transformation in a single pass without allocating intermediate bitmaps.
**Action:** When performing image scaling alongside translation and rotation on Android Canvas, always combine operations into a single `Matrix` transformation instead of creating intermediate scaled `Bitmap` objects.

## 2025-05-26 - [No Deflate Compression on Zip Archives of PNG Images]
**Learning:** Writing compressed PNG data to a `ZipOutputStream` using default DEFLATE compression forces CPU-heavy re-compression passes with zero file size improvement because PNG images are already DEFLATE-compressed. Calling `zipOut.setLevel(Deflater.NO_COMPRESSION)` bypasses redundant compression passes, significantly reducing CPU cycles and processing time when exporting image archives.
**Action:** Whenever streaming pre-compressed image binaries (like PNGs) into a `ZipOutputStream`, explicitly set `setLevel(Deflater.NO_COMPRESSION)`.
