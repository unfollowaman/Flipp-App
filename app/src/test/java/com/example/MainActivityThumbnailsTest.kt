package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityThumbnailsTest {

    @Test
    fun loadPdfThumbnails_handlesExceptionGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidUri = Uri.parse("file:///nonexistent_dir/nonexistent.pdf")
        val thumbnails = loadPdfThumbnails(context, invalidUri)
        assertEquals(emptyList<Any>(), thumbnails)
    }

    @Test
    fun loadImageThumbnail_handlesExceptionGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidUri = Uri.parse("file:///nonexistent_dir/nonexistent.png")
        val thumbnail = loadImageThumbnail(context, invalidUri)
        assertNull(thumbnail)
    }

    @Test
    fun loadImageThumbnail_returnsBitmapForValidImageUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create a dummy image file
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageFile = File(context.cacheDir, "test_thumbnail.png")
        FileOutputStream(imageFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        val imageUri = Uri.fromFile(imageFile)

        val thumbnail = loadImageThumbnail(context, imageUri)

        assertNotNull(thumbnail)
        assertTrue(thumbnail!!.width > 0)
        assertTrue(thumbnail.height > 0)
    }
}
