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
}
