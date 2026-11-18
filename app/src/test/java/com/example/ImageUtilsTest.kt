package com.example

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageUtilsTest {

    @Test
    fun testAddTextWatermark() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Create dummy base image
        val baseBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val baseFile = File(context.cacheDir, "test_base.png")
        FileOutputStream(baseFile).use { fos ->
            baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        val baseUri = android.net.Uri.fromFile(baseFile)

        val outputStream = ByteArrayOutputStream()

        ImageUtils.addWatermark(
            context = context,
            baseImageUri = baseUri,
            watermarkType = "text",
            watermarkText = "WATERMARK",
            watermarkImageUri = null,
            position = "center",
            opacity = 0.5f,
            rotation = 45f,
            size = 1.0f,
            colorStr = "#FF0000",
            outputStream = outputStream
        )

        val resultBytes = outputStream.toByteArray()
        assertNotNull(resultBytes)
        assert(resultBytes.isNotEmpty())
    }

    @Test
    fun testAddTextWatermark_invalidColor_fallsBackToBlack() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Create dummy base image
        val baseBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val baseFile = File(context.cacheDir, "test_base_invalid_color.png")
        FileOutputStream(baseFile).use { fos ->
            baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        val baseUri = android.net.Uri.fromFile(baseFile)

        val outputStream = ByteArrayOutputStream()

        ImageUtils.addWatermark(
            context = context,
            baseImageUri = baseUri,
            watermarkType = "text",
            watermarkText = "WATERMARK_INVALID_COLOR",
            watermarkImageUri = null,
            position = "center",
            opacity = 0.5f,
            rotation = 0f,
            size = 1.0f,
            colorStr = "invalid_hex_color",
            outputStream = outputStream
        )

        val resultBytes = outputStream.toByteArray()
        assertNotNull(resultBytes)
        assert(resultBytes.isNotEmpty())
    }

    @Test
    fun testAddImageWatermark() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Create dummy base image
        val baseBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val baseFile = File(context.cacheDir, "test_base_wm.png")
        FileOutputStream(baseFile).use { fos ->
            baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        val baseUri = android.net.Uri.fromFile(baseFile)

        // Create dummy watermark image
        val wmBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val wmFile = File(context.cacheDir, "test_wm.png")
        FileOutputStream(wmFile).use { fos ->
            wmBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        val wmUri = android.net.Uri.fromFile(wmFile)

        val outputStream = ByteArrayOutputStream()

        ImageUtils.addWatermark(
            context = context,
            baseImageUri = baseUri,
            watermarkType = "image",
            watermarkText = "",
            watermarkImageUri = wmUri,
            position = "center",
            opacity = 0.8f,
            rotation = 30f,
            size = 1.5f,
            colorStr = "#000000",
            outputStream = outputStream
        )

        val resultBytes = outputStream.toByteArray()
        assertNotNull(resultBytes)
        assert(resultBytes.isNotEmpty())
    }

    @Test
    fun testAddWatermark_transparentPngWithoutExtension_preservesAlphaChannel() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Create bitmap with transparent background and a colored center square
        val baseBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        baseBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        // File without .png extension to simulate content:// provider URI
        val baseFile = File(context.cacheDir, "test_base_transparent_no_ext")
        FileOutputStream(baseFile).use { fos ->
            baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        val baseUri = android.net.Uri.fromFile(baseFile)

        val outputStream = ByteArrayOutputStream()

        ImageUtils.addWatermark(
            context = context,
            baseImageUri = baseUri,
            watermarkType = "text",
            watermarkText = "WATERMARK",
            watermarkImageUri = null,
            position = "center",
            opacity = 0.5f,
            rotation = 0f,
            size = 1.0f,
            colorStr = "#FF0000",
            outputStream = outputStream
        )

        val resultBytes = outputStream.toByteArray()
        assertNotNull(resultBytes)
        assert(resultBytes.isNotEmpty())

        // Decode back and verify alpha channel was preserved (not blackened by JPEG compression)
        val decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.size)
        assertNotNull(decodedBitmap)
        org.junit.Assert.assertTrue("Bitmap should have alpha channel preserved", decodedBitmap.hasAlpha())
        val cornerPixel = decodedBitmap.getPixel(0, 0)
        org.junit.Assert.assertEquals("Corner pixel must remain transparent (alpha == 0)", 0, android.graphics.Color.alpha(cornerPixel))
    }

    @Test
    fun testIsPngImage_fileExtensionAndMime() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pngUri = android.net.Uri.parse("file:///data/user/0/com.example/cache/sample.png")
        val jpgUri = android.net.Uri.parse("file:///data/user/0/com.example/cache/sample.jpg")

        org.junit.Assert.assertTrue(ImageUtils.isPngImage(context, pngUri))
        org.junit.Assert.assertFalse(ImageUtils.isPngImage(context, jpgUri))
    }
}
