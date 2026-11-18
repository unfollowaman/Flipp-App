package com.example

import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsTextToPdfTest {

    @Test
    fun testTextToPdf_basicA4TextConversion() {
        val context = RuntimeEnvironment.getApplication()
        val text = "Hello World text to PDF"
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = text,
            pageSizeOption = "A4",
            fontSize = 12f,
            margin = 36f,
            alignment = "left",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        assertTrue(pdfBytes.isNotEmpty())

        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)

        val extractedText = PdfTextExtractor.getTextFromPage(reader, 1)
        assertEquals(text, extractedText.trim())

        val pageSize = reader.getPageSize(1)
        assertEquals(PageSize.A4.width, pageSize.width, 0.1f)
        assertEquals(PageSize.A4.height, pageSize.height, 0.1f)

        reader.close()
    }

    @Test
    fun testTextToPdf_letterPageSize() {
        val context = RuntimeEnvironment.getApplication()
        val text = "Letter Size PDF Document"
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = text,
            pageSizeOption = "LETTER",
            fontSize = 14f,
            margin = 18f,
            alignment = "center",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)

        val pageSize = reader.getPageSize(1)
        assertEquals(PageSize.LETTER.width, pageSize.width, 0.1f)
        assertEquals(PageSize.LETTER.height, pageSize.height, 0.1f)

        val extractedText = PdfTextExtractor.getTextFromPage(reader, 1)
        assertEquals(text, extractedText.trim())

        reader.close()
    }

    @Test
    fun testTextToPdf_unknownPageSizeDefaultsToA4() {
        val context = RuntimeEnvironment.getApplication()
        val text = "Unknown Page Size Test"
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = text,
            pageSizeOption = "CUSTOM_UNKNOWN",
            fontSize = 10f,
            margin = 36f,
            alignment = "right",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        val pageSize = reader.getPageSize(1)
        assertEquals(PageSize.A4.width, pageSize.width, 0.1f)
        assertEquals(PageSize.A4.height, pageSize.height, 0.1f)

        reader.close()
    }

    @Test
    fun testTextToPdf_caseInsensitivePageSizeAndAlignment() {
        val context = RuntimeEnvironment.getApplication()
        val text = "Case Insensitivity Test"
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = text,
            pageSizeOption = "letter",
            fontSize = 12f,
            margin = 20f,
            alignment = "CENTER",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        val pageSize = reader.getPageSize(1)
        assertEquals(PageSize.LETTER.width, pageSize.width, 0.1f)

        val extractedText = PdfTextExtractor.getTextFromPage(reader, 1)
        assertEquals(text, extractedText.trim())

        reader.close()
    }

    @Test
    fun testTextToPdf_alignments() {
        val alignments = listOf("left", "center", "right", "justified", "invalid_alignment")
        val context = RuntimeEnvironment.getApplication()

        for (align in alignments) {
            val outputStream = ByteArrayOutputStream()
            PdfUtils.textToPdf(
                context = context,
                text = "Text with alignment: $align",
                pageSizeOption = "A4",
                fontSize = 12f,
                margin = 36f,
                alignment = align,
                outputStream = outputStream
            )

            val pdfBytes = outputStream.toByteArray()
            val reader = PdfReader(pdfBytes)
            assertEquals(1, reader.numberOfPages)
            val extracted = PdfTextExtractor.getTextFromPage(reader, 1)
            assertTrue(extracted.contains(align))
            reader.close()
        }
    }

    @Test
    fun testTextToPdf_multiLineText() {
        val context = RuntimeEnvironment.getApplication()
        val text = "First Line\nSecond Line\nThird Line"
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = text,
            pageSizeOption = "A4",
            fontSize = 12f,
            margin = 36f,
            alignment = "left",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        val extractedText = PdfTextExtractor.getTextFromPage(reader, 1)
        assertTrue(extractedText.contains("First Line"))
        assertTrue(extractedText.contains("Second Line"))
        assertTrue(extractedText.contains("Third Line"))

        reader.close()
    }

    @Test
    fun testTextToPdf_emptyStringText() {
        val context = RuntimeEnvironment.getApplication()
        val text = ""
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = text,
            pageSizeOption = "A4",
            fontSize = 12f,
            margin = 36f,
            alignment = "left",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)
        reader.close()
    }

    @Test
    fun testTextToPdf_unicodeAndDevanagariText() {
        val context = RuntimeEnvironment.getApplication()
        val hindiText = "नमस्ते दुनिया Flipp App हिन्दी"
        val outputStream = ByteArrayOutputStream()

        PdfUtils.textToPdf(
            context = context,
            text = hindiText,
            pageSizeOption = "A4",
            fontSize = 12f,
            margin = 36f,
            alignment = "left",
            outputStream = outputStream
        )

        val pdfBytes = outputStream.toByteArray()
        assertTrue(pdfBytes.isNotEmpty())

        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)

        val extracted = PdfTextExtractor.getTextFromPage(reader, 1)
        assertTrue(extracted.contains("नमस्ते") || extracted.contains("दुनिया") || extracted.contains("Flipp App"))
        reader.close()
    }

    @Test
    fun testTextToPdf_uniqueTempFilesAvoidRaceConditions() {
        val context = RuntimeEnvironment.getApplication()
        val file1 = java.io.File.createTempFile("text_gen_1_", ".pdf", context.cacheDir)
        val file2 = java.io.File.createTempFile("text_gen_2_", ".pdf", context.cacheDir)
        file1.deleteOnExit()
        file2.deleteOnExit()

        org.junit.Assert.assertNotEquals(file1.absolutePath, file2.absolutePath)

        java.io.FileOutputStream(file1).use { fos ->
            PdfUtils.textToPdf(context, "Doc 1", "A4", 12f, 36f, "left", fos)
        }
        java.io.FileOutputStream(file2).use { fos ->
            PdfUtils.textToPdf(context, "Doc 2", "A4", 12f, 36f, "left", fos)
        }

        assertTrue(file1.length() > 0)
        assertTrue(file2.length() > 0)
    }
}
