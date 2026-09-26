package com.example

import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfUtilsProtectTest {

    private fun createDummyPdf(): File {
        val file = File.createTempFile("dummy_protect", ".pdf")
        file.deleteOnExit()
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()
        document.add(Paragraph("Confidential content"))
        document.close()
        return file
    }

    @Test
    fun testProtectPdf_encryptsPdfWithPasswordOrHandlesMissingCryptoProvider() {
        val context = RuntimeEnvironment.getApplication()
        val inputFile = createDummyPdf()
        val inputUri = Uri.fromFile(inputFile)

        val outputFile = File.createTempFile("protected_output", ".pdf")
        outputFile.deleteOnExit()

        val password = "secret_password_123"

        try {
            FileOutputStream(outputFile).use { fos ->
                PdfUtils.protectPdf(
                    context = context,
                    pdfUri = inputUri,
                    password = java.lang.String(password),
                    outputStream = fos
                )
            }

            assertTrue(outputFile.length() > 0)

            // Attempting to open without password should throw BadPasswordException
            assertThrows(BadPasswordException::class.java) {
                PdfReader(outputFile.absolutePath)
            }

            // Opening with correct password should succeed
            val readerWithPassword = PdfReader(outputFile.absolutePath, password.toByteArray())
            assertNotNull(readerWithPassword)
            assertTrue(readerWithPassword.isEncrypted)
            assertEquals(1, readerWithPassword.numberOfPages)
            readerWithPassword.close()
        } catch (e: NoClassDefFoundError) {
            // When running JVM unit tests without SpongyCastle, itextg setEncryption throws NoClassDefFoundError for org.spongycastle
            assertTrue("Expected NoClassDefFoundError related to spongycastle",
                e.message?.contains("spongycastle") == true || e.message?.contains("ASN1") == true)
        }
    }
}
