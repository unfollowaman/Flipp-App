package com.example

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfUtilsReadBytesTest {

    class ExceptionThrowingProvider : ContentProvider() {
        override fun onCreate(): Boolean = true
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ): Cursor? = null
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
            if (uri.path == "/throw_exception") {
                throw FileNotFoundException("Simulated error reading bytes")
            }
            return null
        }
    }

    private fun invokeReadBytesFromUri(context: Context, uri: Uri): ByteArray? {
        val method = PdfUtils::class.java.getDeclaredMethod("readBytesFromUri", Context::class.java, Uri::class.java)
        method.isAccessible = true
        return method.invoke(PdfUtils, context, uri) as ByteArray?
    }

    private fun createSampleImageFile(): File {
        val file = File.createTempFile("sample_image", ".png")
        file.deleteOnExit()
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    @Test
    fun testReadBytesFromUri_exceptionPath_returnsNull() {
        val app = RuntimeEnvironment.getApplication()
        val provider = ExceptionThrowingProvider()
        val info = ProviderInfo().apply { authority = "exception.provider" }
        provider.attachInfo(app, info)
        ShadowContentResolver.registerProviderInternal("exception.provider", provider)

        val exceptionUri = Uri.parse("content://exception.provider/throw_exception")
        val result = invokeReadBytesFromUri(app, exceptionUri)

        assertNull(result)
    }

    @Test
    fun testReadBytesFromUri_nullInputStream_returnsEmptyByteArray() {
        val app = RuntimeEnvironment.getApplication()
        val provider = ExceptionThrowingProvider()
        val info = ProviderInfo().apply { authority = "exception.provider" }
        provider.attachInfo(app, info)
        ShadowContentResolver.registerProviderInternal("exception.provider", provider)

        val nullUri = Uri.parse("content://exception.provider/null_file")
        val result = invokeReadBytesFromUri(app, nullUri)

        assertNotNull(result)
        assertEquals(0, result?.size)
    }

    @Test
    fun testReadBytesFromUri_validFile_returnsByteArray() {
        val app = RuntimeEnvironment.getApplication()
        val validImageFile = createSampleImageFile()
        val validUri = Uri.fromFile(validImageFile)

        val result = invokeReadBytesFromUri(app, validUri)

        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())
    }

    @Test
    fun testCombineImagesToPdf_skipsInvalidUriWhenReadBytesReturnsNull() {
        val app = RuntimeEnvironment.getApplication()
        val provider = ExceptionThrowingProvider()
        val info = ProviderInfo().apply { authority = "exception.provider" }
        provider.attachInfo(app, info)
        ShadowContentResolver.registerProviderInternal("exception.provider", provider)

        val validImageFile = createSampleImageFile()
        val validUri = Uri.fromFile(validImageFile)
        val invalidUri = Uri.parse("content://exception.provider/throw_exception")

        val outputStream = ByteArrayOutputStream()
        var progressCalls = 0

        PdfUtils.combineImagesToPdf(
            context = app,
            imageUris = listOf(validUri, invalidUri),
            pageSizeOption = "LETTER",
            outputStream = outputStream,
            onProgress = { _, _ -> progressCalls++ }
        )

        // Only validUri passes readBytesFromUri and reaches onProgress
        assertEquals(1, progressCalls)
        val resultBytes = outputStream.toByteArray()
        assertNotNull(resultBytes)
        assertTrue(resultBytes.isNotEmpty())
    }
}
