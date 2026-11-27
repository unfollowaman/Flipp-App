package com.example

import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.exceptions.InvalidPdfException
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
class PdfUtilsProtectTest {

    private fun createDummyPdf(pages: List<String> = listOf("Page 1: Confidential content", "Page 2: Sensitive financial data")): File {
        val file = File.createTempFile("dummy_protect", ".pdf")
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

    private fun createCorruptedFile(): File {
        val file = File.createTempFile("corrupt_test", ".pdf")
        file.deleteOnExit()
        file.writeBytes("This is not a valid PDF file content at all. Corrupted payload header.".toByteArray())
        return file
    }

    /**
     * Requirement 1 & 8: Protect an ordinary PDF with a password and verify resulting PDF
     * opens with correct password and contains expected pages and text.
     */
    @Test
    fun test1_protectOrdinaryPdfWithPassword_createsEncryptedPdfWithExpectedPages() {
        val context = RuntimeEnvironment.getApplication()
        val pageTexts = listOf("Page 1: Confidential content", "Page 2: Sensitive financial data")
        val inputFile = createDummyPdf(pageTexts)
        val inputUri = Uri.fromFile(inputFile)

        val outputFile = File.createTempFile("protected_output", ".pdf")
        outputFile.deleteOnExit()

        val password = "secret_password_123"

        FileOutputStream(outputFile).use { fos ->
            PdfUtils.protectPdf(
                context = context,
                pdfUri = inputUri,
                password = password,
                outputStream = fos
            )
        }

        assertTrue("Protected file must exist and have non-zero size", outputFile.length() > 0)

        // Attempting to open without password should throw BadPasswordException
        assertThrows(BadPasswordException::class.java) {
            PdfReader(outputFile.absolutePath)
        }

        // Opening with correct password should succeed
        val reader = PdfReader(outputFile.absolutePath, password.toByteArray())
        try {
            assertNotNull(reader)
            assertTrue("PDF must report isEncrypted = true", reader.isEncrypted)
            assertEquals("Page count must match original", 2, reader.numberOfPages)

            // Requirement 8: Verify content integrity of all pages
            val page1Text = PdfTextExtractor.getTextFromPage(reader, 1).trim()
            val page2Text = PdfTextExtractor.getTextFromPage(reader, 2).trim()
            assertTrue("Page 1 text must match original", page1Text.contains("Page 1: Confidential content"))
            assertTrue("Page 2 text must match original", page2Text.contains("Page 2: Sensitive financial data"))
        } finally {
            reader.close()
        }
    }

    /**
     * Requirement 2: Open the protected PDF with the correct password.
     */
    @Test
    fun test2_openProtectedPdfWithCorrectPassword() {
        val context = RuntimeEnvironment.getApplication()
        val inputFile = createDummyPdf()
        val outputFile = File.createTempFile("protected_test2", ".pdf")
        outputFile.deleteOnExit()
        val password = "CorrectPassword_456!"

        FileOutputStream(outputFile).use { fos ->
            PdfUtils.protectPdf(context, Uri.fromFile(inputFile), password, fos)
        }

        val reader = PdfReader(outputFile.absolutePath, password.toByteArray())
        try {
            assertTrue(reader.isEncrypted)
            assertEquals(2, reader.numberOfPages)
        } finally {
            reader.close()
        }
    }

    /**
     * Requirement 3: Reject an incorrect password gracefully.
     */
    @Test
    fun test3_rejectIncorrectPasswordGracefully() {
        val context = RuntimeEnvironment.getApplication()
        val inputFile = createDummyPdf()
        val outputFile = File.createTempFile("protected_test3", ".pdf")
        outputFile.deleteOnExit()
        val password = "ActualPassword789"

        FileOutputStream(outputFile).use { fos ->
            PdfUtils.protectPdf(context, Uri.fromFile(inputFile), password, fos)
        }

        // 1. Unsupplied password
        assertThrows(BadPasswordException::class.java) {
            PdfReader(outputFile.absolutePath)
        }

        // 2. Incorrect password
        assertThrows(BadPasswordException::class.java) {
            PdfReader(outputFile.absolutePath, "WrongPassword".toByteArray())
        }
    }

    /**
     * Requirement 5: Handle an invalid or corrupted PDF gracefully.
     */
    @Test
    fun test5_handleInvalidOrCorruptedPdf() {
        val context = RuntimeEnvironment.getApplication()
        val corruptFile = createCorruptedFile()
        val corruptUri = Uri.fromFile(corruptFile)
        val outputFile = File.createTempFile("out_corrupt", ".pdf")
        outputFile.deleteOnExit()

        assertThrows(Exception::class.java) {
            FileOutputStream(outputFile).use { fos ->
                PdfUtils.protectPdf(context, corruptUri, "anyPass", fos)
            }
        }
    }

    /**
     * Requirement 6: Handle an already encrypted PDF.
     * Attempting to encrypt an already encrypted PDF with protectPdf throws BadPasswordException.
     */
    @Test
    fun test6_handleAlreadyEncryptedPdf() {
        val context = RuntimeEnvironment.getApplication()
        val inputFile = createDummyPdf()
        val protectedFile = File.createTempFile("already_encrypted", ".pdf")
        protectedFile.deleteOnExit()
        val firstPassword = "initial_password_1"

        FileOutputStream(protectedFile).use { fos ->
            PdfUtils.protectPdf(context, Uri.fromFile(inputFile), firstPassword, fos)
        }

        val outputSecondFile = File.createTempFile("second_protected", ".pdf")
        outputSecondFile.deleteOnExit()

        // Calling protectPdf on an already encrypted PDF must fail gracefully with BadPasswordException (or Exception)
        assertThrows(BadPasswordException::class.java) {
            FileOutputStream(outputSecondFile).use { fos ->
                PdfUtils.protectPdf(context, Uri.fromFile(protectedFile), "second_password_2", fos)
            }
        }
    }

    /**
     * Requirement 7: Handle unsupported encryption or malformed PDF gracefully without crashing JVM.
     */
    @Test
    fun test7_handleUnsupportedOrMalformedEncryptionGracefully() {
        val context = RuntimeEnvironment.getApplication()
        val corruptFile = createCorruptedFile()
        val corruptUri = Uri.fromFile(corruptFile)

        // isPdfEncrypted must return false safely without LinkageError or crash
        val isEncrypted = PdfUtils.isPdfEncrypted(context, corruptUri)
        assertFalse("Corrupted file is not a valid encrypted PDF", isEncrypted)
    }

    /**
     * Validation: Empty password must be rejected.
     */
    @Test
    fun testProtectPdf_emptyPassword_throwsIllegalArgumentException() {
        val context = RuntimeEnvironment.getApplication()
        val inputFile = createDummyPdf()
        val outputFile = File.createTempFile("out_empty_pass", ".pdf")
        outputFile.deleteOnExit()

        assertThrows(IllegalArgumentException::class.java) {
            FileOutputStream(outputFile).use { fos ->
                PdfUtils.protectPdf(context, Uri.fromFile(inputFile), "", fos)
            }
        }
    }

    /**
     * Non-existent file URI handling.
     */
    @Test
    fun testProtectPdf_nonExistentUri_throwsException() {
        val context = RuntimeEnvironment.getApplication()
        val nonExistentUri = Uri.parse("file:///nonexistent/file.pdf")
        val outputFile = File.createTempFile("dummy_out", ".pdf")
        outputFile.deleteOnExit()

        assertThrows(Exception::class.java) {
            FileOutputStream(outputFile).use { fos ->
                PdfUtils.protectPdf(
                    context = context,
                    pdfUri = nonExistentUri,
                    password = "pass",
                    outputStream = fos
                )
            }
        }
    }

    /**
     * isPdfEncrypted correctly distinguishes plain vs encrypted PDFs.
     */
    @Test
    fun testIsPdfEncrypted_detectsEncryptedAndUnencryptedPdfsCorrectly() {
        val context = RuntimeEnvironment.getApplication()
        val plainFile = createDummyPdf()
        val plainUri = Uri.fromFile(plainFile)

        assertFalse("Plain PDF should not be reported as encrypted", PdfUtils.isPdfEncrypted(context, plainUri))

        val protectedFile = File.createTempFile("check_encrypted", ".pdf")
        protectedFile.deleteOnExit()
        FileOutputStream(protectedFile).use { fos ->
            PdfUtils.protectPdf(context, plainUri, "CheckPass123", fos)
        }

        assertTrue("Protected PDF must be reported as encrypted", PdfUtils.isPdfEncrypted(context, Uri.fromFile(protectedFile)))
    }
}
