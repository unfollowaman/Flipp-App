package com.example

import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsMergeTest {

    private fun createPdfWithText(pages: List<String>): File {
        val file = File.createTempFile("merge_test", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        for ((index, pageText) in pages.withIndex()) {
            if (index > 0) {
                document.newPage()
            }
            document.add(Paragraph(pageText.ifEmpty { " " }))
        }
        document.close()
        return file
    }

    @Test
    fun testMergePdfs_multiplePdfs() {
        val context = RuntimeEnvironment.getApplication()
        val pdf1 = createPdfWithText(listOf("Doc1 Page1"))
        val pdf2 = createPdfWithText(listOf("Doc2 Page1", "Doc2 Page2"))

        val uri1 = Uri.fromFile(pdf1)
        val uri2 = Uri.fromFile(pdf2)

        val outputStream = ByteArrayOutputStream()
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        PdfUtils.mergePdfs(
            context = context,
            pdfUris = listOf(uri1, uri2),
            outputStream = outputStream,
            onProgress = { current, total -> progressUpdates.add(current to total) }
        )

        val mergedBytes = outputStream.toByteArray()
        assertTrue(mergedBytes.isNotEmpty())

        val reader = PdfReader(ByteArrayInputStream(mergedBytes))
        assertEquals(3, reader.numberOfPages)
        reader.close()

        assertEquals(2, progressUpdates.size)
        assertEquals(1 to 2, progressUpdates[0])
        assertEquals(2 to 2, progressUpdates[1])
    }

    @Test
    fun testMergePdfs_singlePdf() {
        val context = RuntimeEnvironment.getApplication()
        val pdf1 = createPdfWithText(listOf("Single PDF Page 1", "Single PDF Page 2"))
        val uri1 = Uri.fromFile(pdf1)

        val outputStream = ByteArrayOutputStream()
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        PdfUtils.mergePdfs(
            context = context,
            pdfUris = listOf(uri1),
            outputStream = outputStream,
            onProgress = { current, total -> progressUpdates.add(current to total) }
        )

        val mergedBytes = outputStream.toByteArray()
        assertTrue(mergedBytes.isNotEmpty())

        val reader = PdfReader(ByteArrayInputStream(mergedBytes))
        assertEquals(2, reader.numberOfPages)
        reader.close()

        assertEquals(1, progressUpdates.size)
        assertEquals(1 to 1, progressUpdates[0])
    }

    @Test
    fun testMergePdfs_emptyList_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val outputStream = ByteArrayOutputStream()

        assertThrows(Exception::class.java) {
            PdfUtils.mergePdfs(
                context = context,
                pdfUris = emptyList(),
                outputStream = outputStream,
                onProgress = { _, _ -> }
            )
        }
    }

    @Test
    fun testMergePdfs_nonExistentFile_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val nonExistentFile = File("/nonexistent/file.pdf")
        val invalidUri = Uri.fromFile(nonExistentFile)

        val outputStream = ByteArrayOutputStream()

        assertThrows(FileNotFoundException::class.java) {
            PdfUtils.mergePdfs(
                context = context,
                pdfUris = listOf(invalidUri),
                outputStream = outputStream,
                onProgress = { _, _ -> }
            )
        }
    }
}
