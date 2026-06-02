package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import java.io.OutputStream

object ImageUtils {

    fun addWatermark(
        context: Context,
        baseImageUri: Uri,
        watermarkType: String, // "text" or "image"
        watermarkText: String,
        watermarkImageUri: Uri?,
        position: String,
        opacity: Float, // 0.0 to 1.0
        rotation: Float,
        size: Float, // 0.1 to 2.0 multiplier for text, or proportion for image
        colorStr: String, // Hex color for text
        outputStream: OutputStream
    ) {
        val options = BitmapFactory.Options()
        options.inMutable = true

        val inputStream = context.contentResolver.openInputStream(baseImageUri) ?: throw Exception("Failed to open base image")
        val baseBitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: throw Exception("Failed to decode base image")
        inputStream.close()

        val canvas = Canvas(baseBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.alpha = (opacity * 255).toInt()

        val width = baseBitmap.width
        val height = baseBitmap.height

        val margin = (width * 0.05f).coerceAtLeast(20f)

        var x = 0f
        var y = 0f

        if (watermarkType == "text") {
            // Text Watermark
            paint.color = try { android.graphics.Color.parseColor(colorStr) } catch(e: Exception) { android.graphics.Color.BLACK }
            paint.alpha = (opacity * 255).toInt()

            // Base font size depends on image width and user size slider
            val baseTextSize = width * 0.05f
            paint.textSize = baseTextSize * size
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)

            val bounds = android.graphics.Rect()
            paint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)
            val textWidth = paint.measureText(watermarkText)
            val textHeight = bounds.height().toFloat()

            when {
                position.contains("left") -> x = margin
                position.contains("center") -> x = (width - textWidth) / 2f
                position.contains("right") -> x = width - textWidth - margin
            }

            when {
                position.contains("top") -> y = margin + textHeight
                position.contains("bottom") -> y = height - margin
                else -> y = (height + textHeight) / 2f // exact center y
            }

            canvas.save()
            // Rotate around text center
            canvas.rotate(rotation, x + textWidth / 2f, y - textHeight / 2f)
            canvas.drawText(watermarkText, x, y, paint)
            canvas.restore()

        } else if (watermarkType == "image" && watermarkImageUri != null) {
            // Image Watermark
            val wmInputStream = context.contentResolver.openInputStream(watermarkImageUri) ?: throw Exception("Failed to open watermark image")
            val wmBitmap = BitmapFactory.decodeStream(wmInputStream) ?: throw Exception("Failed to decode watermark image")
            wmInputStream.close()

            // Calculate scale based on base image width and user size setting
            val targetWmWidth = width * 0.2f * size
            val scale = targetWmWidth / wmBitmap.width
            val targetWmHeight = wmBitmap.height * scale

            when {
                position.contains("left") -> x = margin
                position.contains("center") -> x = (width - targetWmWidth) / 2f
                position.contains("right") -> x = width - targetWmWidth - margin
            }

            when {
                position.contains("top") -> y = margin
                position.contains("bottom") -> y = height - targetWmHeight - margin
                else -> y = (height - targetWmHeight) / 2f
            }

            val matrix = Matrix()
            // First translate to position
            matrix.postTranslate(x, y)
            // Then rotate around the center of the watermark
            matrix.postRotate(rotation, x + targetWmWidth / 2f, y + targetWmHeight / 2f)

            // Create scaled version
            val scaledWm = Bitmap.createScaledBitmap(wmBitmap, targetWmWidth.toInt().coerceAtLeast(1), targetWmHeight.toInt().coerceAtLeast(1), true)

            // Draw
            canvas.save()
            canvas.concat(matrix)
            // reset translate since it's already in matrix
            canvas.drawBitmap(scaledWm, 0f, 0f, paint)
            canvas.restore()

            scaledWm.recycle()
            wmBitmap.recycle()
        }

        // Save to output stream
        // use original format or JPEG as default
        val compressFormat = if (baseImageUri.toString().lowercase().endsWith("png")) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }

        baseBitmap.compress(compressFormat, 95, outputStream)
        baseBitmap.recycle()
    }
}
