package com.example

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [MainActivityThumbnailsTest.ShadowTestPdfRenderer::class, MainActivityThumbnailsTest.ShadowTestPdfRendererPage::class])
class MainActivityThumbnailsTest {

    class DummyPdfProvider : ContentProvider() {
        companion object {
            var pdfFile: File? = null
        }

        override fun onCreate(): Boolean = true
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            return pdfFile?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
        }
    }

    @Implements(PdfRenderer::class)
    class ShadowTestPdfRenderer {
        companion object {
            var pageCountToReturn = 5
            var shouldThrowSecurityException = false
        }

        @Implementation
        fun __constructor__(input: ParcelFileDescriptor) {
            if (shouldThrowSecurityException) {
                throw SecurityException("PDF is password protected")
            }
        }

        @Implementation
        fun getPageCount(): Int = pageCountToReturn

        @Implementation
        fun openPage(index: Int): PdfRenderer.Page {
            if (index < 0 || index >= pageCountToReturn) {
                throw IllegalArgumentException("Invalid page index $index")
            }
            return Shadow.newInstanceOf(PdfRenderer.Page::class.java)
        }

        @Implementation
        fun close() {}
    }

    @Implements(PdfRenderer.Page::class)
    class ShadowTestPdfRendererPage {
        @Implementation
        fun getWidth(): Int = 200

        @Implementation
        fun getHeight(): Int = 300

        @Implementation
        fun render(destination: Bitmap, destClip: android.graphics.Rect?, transform: android.graphics.Matrix?, renderMode: Int) {
            // Render mock
        }

        @Implementation
        fun close() {}
    }

    @Before
    fun setUp() {
        ShadowTestPdfRenderer.pageCountToReturn = 5
        ShadowTestPdfRenderer.shouldThrowSecurityException = false
    }

    @Test
    fun loadPdfThumbnails_handlesExceptionGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidUri = Uri.parse("file:///nonexistent_dir/nonexistent.pdf")
        val thumbnails = loadPdfThumbnails(context, invalidUri)
        assertEquals(emptyList<Any>(), thumbnails)
    }

    @Test
    fun loadPdfThumbnails_returnsBitmapsForValidPdfUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempPdf = File.createTempFile("test_sample", ".pdf", context.cacheDir)
        tempPdf.writeText("%PDF-1.4 dummy content")
        tempPdf.deleteOnExit()

        DummyPdfProvider.pdfFile = tempPdf
        val uri = Uri.parse("content://dummy_pdf_thumbs/test.pdf")

        val provider = DummyPdfProvider()
        val info = ProviderInfo().apply { authority = "dummy_pdf_thumbs" }
        provider.attachInfo(context, info)
        ShadowContentResolver.registerProviderInternal("dummy_pdf_thumbs", provider)

        val thumbnails = loadPdfThumbnails(context, uri, maxPages = 4)

        // pageCountToReturn is 5, maxPages is 4 -> should return 4 Bitmaps
        assertEquals(4, thumbnails.size)
        thumbnails.forEach { bitmap ->
            assertNotNull(bitmap)
            assertEquals(100, bitmap.width) // 200 / 2
            assertEquals(150, bitmap.height) // 300 / 2
        }
    }

    @Test
    fun loadPdfThumbnails_respectsMaxPagesLimit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempPdf = File.createTempFile("test_sample_limit", ".pdf", context.cacheDir)
        tempPdf.writeText("%PDF-1.4 dummy content")
        tempPdf.deleteOnExit()

        DummyPdfProvider.pdfFile = tempPdf
        val uri = Uri.parse("content://dummy_pdf_limit/test.pdf")

        val provider = DummyPdfProvider()
        val info = ProviderInfo().apply { authority = "dummy_pdf_limit" }
        provider.attachInfo(context, info)
        ShadowContentResolver.registerProviderInternal("dummy_pdf_limit", provider)

        ShadowTestPdfRenderer.pageCountToReturn = 10
        val thumbnails = loadPdfThumbnails(context, uri, maxPages = 2)

        // Total 10 pages, maxPages = 2 -> should return 2 Bitmaps
        assertEquals(2, thumbnails.size)
    }

    @Test
    fun loadPdfThumbnails_handlesSecurityExceptionForEncryptedPdf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempPdf = File.createTempFile("test_sample_encrypted", ".pdf", context.cacheDir)
        tempPdf.writeText("%PDF-1.4 dummy content")
        tempPdf.deleteOnExit()

        DummyPdfProvider.pdfFile = tempPdf
        val uri = Uri.parse("content://dummy_pdf_encrypted/test.pdf")

        val provider = DummyPdfProvider()
        val info = ProviderInfo().apply { authority = "dummy_pdf_encrypted" }
        provider.attachInfo(context, info)
        ShadowContentResolver.registerProviderInternal("dummy_pdf_encrypted", provider)

        ShadowTestPdfRenderer.shouldThrowSecurityException = true

        val thumbnails = loadPdfThumbnails(context, uri, maxPages = 4)

        assertTrue(thumbnails.isEmpty())
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
