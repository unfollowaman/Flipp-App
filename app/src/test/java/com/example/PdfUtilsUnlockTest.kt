package com.example

import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class PdfUtilsUnlockTest {

    private fun createDummyPdf(pages: List<String> = listOf("Page 1: Unlocked document content", "Page 2: Second page data")): File {
        val file = File.createTempFile("dummy_unlock", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()
        for ((index, text) in pages.withIndex()) {
            if (index > 0) {
                document.newPage()
            }
            document.add(Paragraph(text))
        }
        document.close()
        return file
    }

    private fun createProtectedPdf(password: String, pages: List<String>): File {
        val plainFile = createDummyPdf(pages)
        val protectedFile = File.createTempFile("protected_source", ".pdf")
        protectedFile.deleteOnExit()
        val context = RuntimeEnvironment.getApplication()
        FileOutputStream(protectedFile).use { fos ->
            PdfUtils.protectPdf(context, Uri.fromFile(plainFile), password, fos)
        }
        return protectedFile
    }

    /**
     * Requirement 4 & 8: Unlock a password-protected PDF.
     * Verifies that the unlocked PDF can be opened without a password and contains
     * the expected page count and page text content.
     */
    @Test
    fun test4_unlockPasswordProtectedPdf_producesDecryptedPdfWithExpectedPages() {
        val context = RuntimeEnvironment.getApplication()
        val password = "MySecretUnlockPassword!"
        val pages = listOf("Page 1: Unlocked document content", "Page 2: Second page data")
        val protectedFile = createProtectedPdf(password, pages)
        val protectedUri = Uri.fromFile(protectedFile)

        val unlockedFile = File.createTempFile("unlocked_result", ".pdf")
        unlockedFile.deleteOnExit()

        FileOutputStream(unlockedFile).use { fos ->
            PdfUtils.unlockPdf(context, protectedUri, password, fos)
        }

        assertTrue("Unlocked file must exist and have non-zero size", unlockedFile.length() > 0)

        // Opening WITHOUT any password must succeed
        val reader = PdfReader(unlockedFile.absolutePath)
        try {
            assertNotNull(reader)
            assertFalse("Unlocked PDF must have isEncrypted = false", reader.isEncrypted)
            assertEquals("Page count must be preserved", 2, reader.numberOfPages)

            // Requirement 8: Verify content integrity
            val page1Text = PdfTextExtractor.getTextFromPage(reader, 1).trim()
            val page2Text = PdfTextExtractor.getTextFromPage(reader, 2).trim()
            assertTrue("Page 1 text preserved", page1Text.contains("Page 1: Unlocked document content"))
            assertTrue("Page 2 text preserved", page2Text.contains("Page 2: Second page data"))
        } finally {
            reader.close()
        }
    }

    /**
     * Requirement 3: Reject an incorrect password when unlocking.
     */
    @Test
    fun test_unlockPdf_withWrongPassword_throwsBadPasswordException() {
        val context = RuntimeEnvironment.getApplication()
        val correctPassword = "CorrectPassword123"
        val protectedFile = createProtectedPdf(correctPassword, listOf("Secret page"))
        val protectedUri = Uri.fromFile(protectedFile)

        val unlockedFile = File.createTempFile("failed_unlock", ".pdf")
        unlockedFile.deleteOnExit()

        assertThrows(BadPasswordException::class.java) {
            FileOutputStream(unlockedFile).use { fos ->
                PdfUtils.unlockPdf(context, protectedUri, "WrongPassword456", fos)
            }
        }
    }

    /**
     * Requirement 5: Unlock on corrupted file throws Exception gracefully.
     */
    @Test
    fun test_unlockPdf_corruptedFile_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val corruptFile = File.createTempFile("corrupt_unlock", ".pdf")
        corruptFile.deleteOnExit()
        corruptFile.writeBytes("INVALID_PDF_DATA_STREAM".toByteArray())
        val corruptUri = Uri.fromFile(corruptFile)

        val outputFile = File.createTempFile("out_corrupt_unlock", ".pdf")
        outputFile.deleteOnExit()

        assertThrows(Exception::class.java) {
            FileOutputStream(outputFile).use { fos ->
                PdfUtils.unlockPdf(context, corruptUri, "anyPassword", fos)
            }
        }
    }

    /**
     * Unlock on an unencrypted PDF handles gracefully and copies stream.
     */
    @Test
    fun test_unlockPdf_unencryptedPdf_handlesGracefully() {
        val context = RuntimeEnvironment.getApplication()
        val plainFile = createDummyPdf(listOf("Plain text page"))
        val plainUri = Uri.fromFile(plainFile)

        val outputFile = File.createTempFile("out_plain_unlock", ".pdf")
        outputFile.deleteOnExit()

        FileOutputStream(outputFile).use { fos ->
            PdfUtils.unlockPdf(context, plainUri, "irrelevantPassword", fos)
        }

        assertTrue(outputFile.length() > 0)
        val reader = PdfReader(outputFile.absolutePath)
        try {
            assertFalse(reader.isEncrypted)
            assertEquals(1, reader.numberOfPages)
        } finally {
            reader.close()
        }
    }

    /**
     * Non-existent URI throws Exception.
     */
    @Test
    fun test_unlockPdf_nonExistentUri_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val nonExistentUri = Uri.parse("file:///nonexistent/file.pdf")
        val outputFile = File.createTempFile("dummy_unlock_out", ".pdf")
        outputFile.deleteOnExit()

        assertThrows(Exception::class.java) {
            FileOutputStream(outputFile).use { fos ->
                PdfUtils.unlockPdf(context, nonExistentUri, "pass", fos)
            }
        }
    }
}
