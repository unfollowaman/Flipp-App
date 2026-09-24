package com.example

import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsExtractTextTest {

    private fun createPdfWithText(pages: List<String>): File {
        val file = File.createTempFile("extract_test", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        for ((index, pageText) in pages.withIndex()) {
            if (index > 0) {
                document.newPage()
            }
            if (pageText.isNotEmpty()) {
                document.add(Paragraph(pageText))
            }
        }
        document.close()
        return file
    }

    @Test
    fun testExtractTextFromPdf_singlePageText() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createPdfWithText(listOf("Hello World PDF"))
        val uri = Uri.fromFile(pdfFile)

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val result = PdfUtils.extractTextFromPdf(context, uri) { current, total ->
            progressUpdates.add(current to total)
        }

        assertEquals("Hello World PDF", result)
        assertEquals(1, progressUpdates.size)
        assertEquals(1 to 1, progressUpdates[0])
    }

    @Test
    fun testExtractTextFromPdf_multiPageText() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createPdfWithText(listOf("First Page Content", "Second Page Content"))
        val uri = Uri.fromFile(pdfFile)

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val result = PdfUtils.extractTextFromPdf(context, uri) { current, total ->
            progressUpdates.add(current to total)
        }

        assertTrue(result.contains("First Page Content"))
        assertTrue(result.contains("Second Page Content"))
        assertEquals(2, progressUpdates.size)
        assertEquals(1 to 2, progressUpdates[0])
        assertEquals(2 to 2, progressUpdates[1])
    }

    @Test
    fun testExtractTextFromPdf_blankPdf() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createPdfWithText(listOf(""))
        val uri = Uri.fromFile(pdfFile)

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val result = PdfUtils.extractTextFromPdf(context, uri) { current, total ->
            progressUpdates.add(current to total)
        }

        assertEquals("", result)
        assertEquals(1, progressUpdates.size)
        assertEquals(1 to 1, progressUpdates[0])
    }

    @Test
    fun testExtractTextFromPdf_invalidUri_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val invalidUri = Uri.parse("content://invalid/nonexistent.pdf")

        val exception = assertThrows(Exception::class.java) {
            PdfUtils.extractTextFromPdf(context, invalidUri) { _, _ -> }
        }

        assertEquals("Failed to open input stream for PDF", exception.message)
    }
}
