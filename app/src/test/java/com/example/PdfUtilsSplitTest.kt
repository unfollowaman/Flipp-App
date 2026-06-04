package com.example

import android.content.Context
import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfUtilsSplitTest {

    private fun createDummyPdf(): File {
        val file = File.createTempFile("dummy", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()
        document.add(Paragraph("Page 1"))
        document.newPage()
        document.add(Paragraph("Page 2"))
        document.newPage()
        document.add(Paragraph("Page 3"))
        document.close()
        return file
    }

    @Test
    fun testSplitPdf_invalidRange_startPageLessThanOne() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createDummyPdf()
        val uri = Uri.fromFile(pdfFile)

        val outputStream = ByteArrayOutputStream()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            PdfUtils.splitPdf(context, uri, 0, 2, outputStream)
        }
        assertEquals("No valid pages in range.", exception.message)
    }

    @Test
    fun testSplitPdf_invalidRange_endPageGreaterThanTotal() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createDummyPdf()
        val uri = Uri.fromFile(pdfFile)

        val outputStream = ByteArrayOutputStream()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            PdfUtils.splitPdf(context, uri, 1, 4, outputStream)
        }
        assertEquals("No valid pages in range.", exception.message)
    }

    @Test
    fun testSplitPdf_invalidRange_startPageGreaterThanEndPage() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createDummyPdf()
        val uri = Uri.fromFile(pdfFile)

        val outputStream = ByteArrayOutputStream()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            PdfUtils.splitPdf(context, uri, 3, 2, outputStream)
        }
        assertEquals("No valid pages in range.", exception.message)
    }

    @Test
    fun testSplitPdf_validRange_doesNotThrow() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createDummyPdf()
        val uri = Uri.fromFile(pdfFile)

        val outputStream = ByteArrayOutputStream()

        // This should not throw an exception
        PdfUtils.splitPdf(context, uri, 1, 2, outputStream)
    }
}
