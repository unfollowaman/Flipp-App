package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
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
}
