package com.example

import android.graphics.Bitmap
import android.net.Uri
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class PdfUtilsCombineImagesTest {

    private fun createSampleImageFile(width: Int = 100, height: Int = 100): File {
        val file = File.createTempFile("sample_img_${width}x${height}_", ".png")
        file.deleteOnExit()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    @Test
    fun testCombineImagesToPdf_A4PageSize() {
        val context = RuntimeEnvironment.getApplication()
        val imageFile = createSampleImageFile(200, 300)
        val imageUri = Uri.fromFile(imageFile)

        val outputStream = ByteArrayOutputStream()
        var progressCalled = false

        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = listOf(imageUri),
            pageSizeOption = "A4",
            outputStream = outputStream,
            onProgress = { current, total ->
                assertEquals(1, current)
                assertEquals(1, total)
                progressCalled = true
            }
        )

        assertTrue(progressCalled)
        val pdfBytes = outputStream.toByteArray()
        assertTrue(pdfBytes.isNotEmpty())

        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)

        val pageSize = reader.getPageSize(1)
        assertEquals(PageSize.A4.width, pageSize.width, 0.1f)
        assertEquals(PageSize.A4.height, pageSize.height, 0.1f)

        reader.close()
    }

    @Test
    fun testCombineImagesToPdf_LetterPageSize() {
        val context = RuntimeEnvironment.getApplication()
        val imageFile = createSampleImageFile(150, 150)
        val imageUri = Uri.fromFile(imageFile)

        val outputStream = ByteArrayOutputStream()

        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = listOf(imageUri),
            pageSizeOption = "LETTER",
            outputStream = outputStream,
            onProgress = { _, _ -> }
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)

        val pageSize = reader.getPageSize(1)
        assertEquals(PageSize.LETTER.width, pageSize.width, 0.1f)
        assertEquals(PageSize.LETTER.height, pageSize.height, 0.1f)

        reader.close()
    }

    @Test
    fun testCombineImagesToPdf_AutoPageSize() {
        val context = RuntimeEnvironment.getApplication()
        val imageFile = createSampleImageFile(250, 400)
        val imageUri = Uri.fromFile(imageFile)

        val outputStream = ByteArrayOutputStream()

        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = listOf(imageUri),
            pageSizeOption = "AUTO",
            outputStream = outputStream,
            onProgress = { _, _ -> }
        )

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        assertEquals(1, reader.numberOfPages)

        val pageSize = reader.getPageSize(1)
        // In AUTO mode, page size matches image dimensions in points
        assertEquals(250f, pageSize.width, 0.1f)
        assertEquals(400f, pageSize.height, 0.1f)

        reader.close()
    }

    @Test
    fun testCombineImagesToPdf_multipleImages() {
        val context = RuntimeEnvironment.getApplication()
        val imageFile1 = createSampleImageFile(100, 100)
        val imageFile2 = createSampleImageFile(200, 200)
        val imageFile3 = createSampleImageFile(300, 300)

        val imageUris = listOf(
            Uri.fromFile(imageFile1),
            Uri.fromFile(imageFile2),
            Uri.fromFile(imageFile3)
        )

        val outputStream = ByteArrayOutputStream()
        val progressReports = mutableListOf<Pair<Int, Int>>()

        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = imageUris,
            pageSizeOption = "A4",
            outputStream = outputStream,
            onProgress = { current, total ->
                progressReports.add(Pair(current, total))
            }
        )

        assertEquals(3, progressReports.size)
        assertEquals(Pair(1, 3), progressReports[0])
        assertEquals(Pair(2, 3), progressReports[1])
        assertEquals(Pair(3, 3), progressReports[2])

        val pdfBytes = outputStream.toByteArray()
        val reader = PdfReader(pdfBytes)
        assertEquals(3, reader.numberOfPages)

        reader.close()
    }

    @Test
    fun testCombineImagesToPdf_caseInsensitivePageSizeAndDefaultFallback() {
        val context = RuntimeEnvironment.getApplication()
        val imageFile = createSampleImageFile(100, 100)
        val imageUri = Uri.fromFile(imageFile)

        // Case-insensitive letter
        val outputStreamLetter = ByteArrayOutputStream()
        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = listOf(imageUri),
            pageSizeOption = "letter",
            outputStream = outputStreamLetter,
            onProgress = { _, _ -> }
        )
        val readerLetter = PdfReader(outputStreamLetter.toByteArray())
        assertEquals(PageSize.LETTER.width, readerLetter.getPageSize(1).width, 0.1f)
        readerLetter.close()

        // Case-insensitive auto
        val outputStreamAuto = ByteArrayOutputStream()
        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = listOf(imageUri),
            pageSizeOption = "auto",
            outputStream = outputStreamAuto,
            onProgress = { _, _ -> }
        )
        val readerAuto = PdfReader(outputStreamAuto.toByteArray())
        assertEquals(100f, readerAuto.getPageSize(1).width, 0.1f)
        readerAuto.close()

        // Unknown page size defaults to A4
        val outputStreamUnknown = ByteArrayOutputStream()
        PdfUtils.combineImagesToPdf(
            context = context,
            imageUris = listOf(imageUri),
            pageSizeOption = "UNKNOWN_OPTION",
            outputStream = outputStreamUnknown,
            onProgress = { _, _ -> }
        )
        val readerUnknown = PdfReader(outputStreamUnknown.toByteArray())
        assertEquals(PageSize.A4.width, readerUnknown.getPageSize(1).width, 0.1f)
        readerUnknown.close()
    }

    @Test
    fun testCombineImagesToPdf_emptyImageList() {
        val context = RuntimeEnvironment.getApplication()
        val outputStream = ByteArrayOutputStream()
        var progressCalled = false

        var exceptionThrown = false
        try {
            PdfUtils.combineImagesToPdf(
                context = context,
                imageUris = emptyList(),
                pageSizeOption = "A4",
                outputStream = outputStream,
                onProgress = { _, _ -> progressCalled = true }
            )
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue(!progressCalled)
        assertTrue(exceptionThrown)
    }
}
