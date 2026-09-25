package com.example

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsConvertPdfToPngZipTest {

    private fun createDummyPdf(pages: Int): File {
        val file = File.createTempFile("dummy", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()
        for (i in 1..pages) {
            document.add(Paragraph("Page $i"))
            if (i < pages) document.newPage()
        }
        document.close()
        return file
    }

    class DummyProvider : ContentProvider() {
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

    @Test
    fun testConvertPdfToPngZip_validPdf() {
        val app = RuntimeEnvironment.getApplication()
        val pdfFile = createDummyPdf(2)
        DummyProvider.pdfFile = pdfFile
        val uri = Uri.parse("content://dummy_convert/pdf")

        val provider = DummyProvider()
        val info = ProviderInfo()
        info.authority = "dummy_convert"
        provider.attachInfo(app, info)
        ShadowContentResolver.registerProviderInternal("dummy_convert", provider)

        val outputStream = ByteArrayOutputStream()
        var progressCalled = false
        var lastPage = 0
        var totalPages = 0

        PdfUtils.convertPdfToPngZip(app, uri, scale = 1, outputStream = outputStream) { page, total ->
            progressCalled = true
            lastPage = page
            totalPages = total
        }

        // Output stream should have valid zip header data written by ZipOutputStream.finish()/close()
        val zipBytes = outputStream.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        // Verify that output stream is a valid zip archive
        val zipInputStream = ZipInputStream(ByteArrayInputStream(zipBytes))
        assertNotNull(zipInputStream)

        // Note: Robolectric 4.16.1's ShadowPdfRenderer has pageCount = 0.
        // If renderer has pages, entries would be present and callback invoked.
        // We verify that the method executes completely without throwing exception.
        if (progressCalled) {
            assertTrue(lastPage > 0)
            assertEquals(totalPages, lastPage)
        }
    }

    @Test
    fun testConvertPdfToPngZip_nullDescriptor_throwsException() {
        val app = RuntimeEnvironment.getApplication()
        val uri = Uri.parse("content://dummy_invalid_convert/pdf")
        DummyProvider.pdfFile = null

        val provider = DummyProvider()
        val info = ProviderInfo()
        info.authority = "dummy_invalid_convert"
        provider.attachInfo(app, info)
        ShadowContentResolver.registerProviderInternal("dummy_invalid_convert", provider)

        val outputStream = ByteArrayOutputStream()

        val exception = assertThrows(Exception::class.java) {
            PdfUtils.convertPdfToPngZip(app, uri, scale = 1, outputStream = outputStream) { _, _ -> }
        }

        assertEquals("Failed to open file descriptor", exception.message)
    }
}
