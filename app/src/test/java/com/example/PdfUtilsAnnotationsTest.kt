package com.example

import android.graphics.Color
import android.net.Uri
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfNumber
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.PdfName
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PdfUtilsAnnotationsTest {
    @Test
    fun readsDisplayedDimensionsForRotatedPdfPage() {
        val context = RuntimeEnvironment.getApplication()
        val source = createRotatedThreePagePdf()

        val geometry = PdfUtils.getPdfPageGeometry(context, Uri.fromFile(source), pageIndex = 1)

        assertEquals(612f, geometry.width, 0.01f)
        assertEquals(792f, geometry.height, 0.01f)
        assertEquals(792f, geometry.displayWidth, 0.01f)
        assertEquals(612f, geometry.displayHeight, 0.01f)
        assertEquals(PdfPoint(0f, 0f), geometry.toPdfPoint(PagePoint(0f, 0f)))
    }

    @Test
    fun vectorStrokesExportOnTheirOwnPagesAndPreservePdfContentAndDimensions() {
        val context = RuntimeEnvironment.getApplication()
        val source = createRotatedThreePagePdf()
        val sourceBytes = source.readBytes()
        val annotations = mapOf(
            1 to PageAnnotationSet(
                strokes = listOf(
                    PdfStroke(
                        points = listOf(PagePoint(0.02f, 0.03f), PagePoint(0.98f, 0.96f)),
                        colorArgb = Color.RED,
                        width = 0.01f
                    ),
                    PdfStroke(
                        points = listOf(PagePoint(0.5f, 0.5f), PagePoint(0.6f, 0.5f)),
                        colorArgb = Color.GREEN,
                        width = 0.015f
                    )
                )
            ),
            3 to PageAnnotationSet(
                strokes = listOf(
                    PdfStroke(
                        points = listOf(PagePoint(0.5f, 0.02f), PagePoint(0.5f, 0.98f)),
                        colorArgb = Color.BLUE,
                        width = 0.02f
                    )
                ),
                text = PdfTextAnnotation("Page 3 note", PagePoint(0.1f, 0.15f), Color.BLACK, 0.04f)
            )
        )
        val output = ByteArrayOutputStream()

        PdfUtils.stampAnnotationsOnPdfPages(context, Uri.fromFile(source), annotations, output)

        val originalReader = PdfReader(sourceBytes)
        val editedReader = PdfReader(output.toByteArray())
        try {
            assertEquals(originalReader.numberOfPages, editedReader.numberOfPages)
            for (page in 1..3) {
                assertEquals(originalReader.getPageSize(page), editedReader.getPageSize(page))
                assertEquals(originalReader.getPageRotation(page), editedReader.getPageRotation(page))
                assertTrue(PdfTextExtractor.getTextFromPage(editedReader, page).contains("Original page $page"))
            }
            assertEquals(90, editedReader.getPageRotation(2))
            assertEquals(792f, editedReader.getPageSizeWithRotation(2).width, 0.01f)
            assertEquals(612f, editedReader.getPageSizeWithRotation(2).height, 0.01f)

            val pageOneContent = String(editedReader.getPageContent(1), Charsets.ISO_8859_1)
            val pageTwoContent = String(editedReader.getPageContent(2), Charsets.ISO_8859_1)
            val pageThreeContent = String(editedReader.getPageContent(3), Charsets.ISO_8859_1)
            assertTrue(pageOneContent.contains("6.12 w"))
            assertTrue(pageOneContent.contains("1 0 0 RG"))
            assertTrue(pageOneContent.contains("9.18 w"))
            assertTrue(pageOneContent.contains("0 1 0 RG"))
            assertTrue(pageOneContent.contains("12.24 768.24 m"))
            assertFalse(pageTwoContent.contains("6.12 w"))
            assertFalse(pageTwoContent.contains("12.24 w"))
            assertTrue(pageThreeContent.contains("12.24 w"))
            assertTrue(pageThreeContent.contains("0 0 1 RG"))
            assertTrue(PdfTextExtractor.getTextFromPage(editedReader, 3).contains("Page 3 note"))
        } finally {
            originalReader.close()
            editedReader.close()
        }
    }

    @Test
    fun emptyAnnotationExportCopiesTheOriginalPdfUnchanged() {
        val context = RuntimeEnvironment.getApplication()
        val source = createRotatedThreePagePdf()
        val sourceBytes = source.readBytes()
        val output = ByteArrayOutputStream()

        PdfUtils.stampAnnotationsOnPdfPages(context, Uri.fromFile(source), emptyMap(), output)

        assertArrayEquals(sourceBytes, output.toByteArray())
    }

    @Test
    fun annotationExportRejectsAnInvalidPageNumber() {
        val context = RuntimeEnvironment.getApplication()
        val source = createRotatedThreePagePdf()

        assertThrows(IllegalArgumentException::class.java) {
            PdfUtils.stampAnnotationsOnPdfPages(
                context,
                Uri.fromFile(source),
                mapOf(4 to PageAnnotationSet(strokes = listOf(PdfStroke(listOf(PagePoint(0.5f, 0.5f)), Color.BLACK, 0.01f)))),
                ByteArrayOutputStream()
            )
        }
    }

    private fun createRotatedThreePagePdf(): File {
        val source = File.createTempFile("edit_annotations_source", ".pdf")
        source.deleteOnExit()
        val document = Document(PageSize.LETTER)
        val writer = PdfWriter.getInstance(document, FileOutputStream(source))
        document.open()
        for (page in 1..3) {
            if (page > 1) document.newPage()
            document.add(Paragraph("Original page $page"))
            drawReferenceGrid(writer)
        }
        document.close()

        val rotated = File.createTempFile("edit_annotations_rotated", ".pdf")
        rotated.deleteOnExit()
        val reader = PdfReader(source.readBytes())
        val stamper = PdfStamper(reader, FileOutputStream(rotated))
        try {
            reader.getPageN(2).put(PdfName.ROTATE, PdfNumber(90))
        } finally {
            stamper.close()
            reader.close()
        }
        return rotated
    }

    private fun drawReferenceGrid(writer: PdfWriter) {
        val canvas = writer.directContent
        canvas.saveState()
        canvas.setColorStroke(BaseColor(190, 190, 190))
        canvas.setLineWidth(0.35f)
        for (division in 1 until 10) {
            val x = 612f * division / 10f
            val y = 792f * division / 10f
            canvas.moveTo(x, 0f)
            canvas.lineTo(x, 792f)
            canvas.moveTo(0f, y)
            canvas.lineTo(612f, y)
        }
        canvas.stroke()
        canvas.setColorStroke(BaseColor(90, 90, 90))
        canvas.setLineWidth(0.8f)
        canvas.moveTo(306f, 378f)
        canvas.lineTo(306f, 414f)
        canvas.moveTo(288f, 396f)
        canvas.lineTo(324f, 396f)
        canvas.stroke()
        canvas.restoreState()
    }
}
