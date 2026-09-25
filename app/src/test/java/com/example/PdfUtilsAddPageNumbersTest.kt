package com.example

import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsAddPageNumbersTest {

    private fun createPdfWithPages(pageCount: Int): File {
        val file = File.createTempFile("add_page_numbers_test", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        for (i in 1..pageCount) {
            if (i > 1) {
                document.newPage()
            }
            document.add(Paragraph("Content on page $i"))
        }
        document.close()
        return file
    }

    @Test
    fun testAddPageNumbers_basicMultiPage() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createPdfWithPages(3)
        val uri = Uri.fromFile(pdfFile)
        val outputStream = ByteArrayOutputStream()

        PdfUtils.addPageNumbers(
            context = context,
            pdfUri = uri,
            position = "bottom-center",
            startNumber = 1,
            outputStream = outputStream
        )

        val resultBytes = outputStream.toByteArray()
        assertTrue(resultBytes.isNotEmpty())

        val reader = PdfReader(resultBytes)
        assertEquals(3, reader.numberOfPages)

        for (i in 1..3) {
            val extractedText = PdfTextExtractor.getTextFromPage(reader, i)
            assertTrue(extractedText.contains("Content on page $i"))
            assertTrue(extractedText.contains(i.toString()))
        }

        reader.close()
    }

    @Test
    fun testAddPageNumbers_customStartNumber() {
        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createPdfWithPages(2)
        val uri = Uri.fromFile(pdfFile)
        val outputStream = ByteArrayOutputStream()

        val startNum = 10
        PdfUtils.addPageNumbers(
            context = context,
            pdfUri = uri,
            position = "top-right",
            startNumber = startNum,
            outputStream = outputStream
        )

        val resultBytes = outputStream.toByteArray()
        val reader = PdfReader(resultBytes)

        assertEquals(2, reader.numberOfPages)

        val page1Text = PdfTextExtractor.getTextFromPage(reader, 1)
        val page2Text = PdfTextExtractor.getTextFromPage(reader, 2)

        assertTrue(page1Text.contains("10"))
        assertTrue(page2Text.contains("11"))

        reader.close()
    }

    @Test
    fun testAddPageNumbers_allPositions() {
        val positions = listOf(
            "top-left",
            "top-center",
            "top-right",
            "bottom-left",
            "bottom-center",
            "bottom-right"
        )

        val context = RuntimeEnvironment.getApplication()
        val pdfFile = createPdfWithPages(1)
        val uri = Uri.fromFile(pdfFile)

        for (pos in positions) {
            val outputStream = ByteArrayOutputStream()
            PdfUtils.addPageNumbers(
                context = context,
                pdfUri = uri,
                position = pos,
                startNumber = 1,
                outputStream = outputStream
            )

            val resultBytes = outputStream.toByteArray()
            val reader = PdfReader(resultBytes)
            assertEquals(1, reader.numberOfPages)

            val text = PdfTextExtractor.getTextFromPage(reader, 1)
            assertTrue(text.contains("1"))

            reader.close()
        }
    }

    @Test
    fun testAddPageNumbers_invalidUri_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val invalidUri = Uri.parse("content://invalid/nonexistent.pdf")
        val outputStream = ByteArrayOutputStream()

        val exception = assertThrows(Exception::class.java) {
            PdfUtils.addPageNumbers(
                context = context,
                pdfUri = invalidUri,
                position = "bottom-center",
                startNumber = 1,
                outputStream = outputStream
            )
        }

        val msg = exception.message ?: ""
        assertTrue(msg.contains("Failed to open file descriptor") || msg.contains("ShadowContentResolver"))
    }
}
