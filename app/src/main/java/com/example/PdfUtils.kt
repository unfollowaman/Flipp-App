package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfUtils {

    /**
     * Renders all pages of a PDF to a ZIP output stream.
     */
    fun convertPdfToPngZip(
        context: Context,
        pdfUri: Uri,
        scale: Int,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val contentResolver = context.contentResolver
        val pfd = contentResolver.openFileDescriptor(pdfUri, "r") ?: throw Exception("Failed to open file descriptor")
        
        try {
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            
            val zipOut = ZipOutputStream(outputStream)
            
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                
                // Scale width/height by resolution options (1x, 2x, 3x)
                val width = page.width * scale
                val height = page.height * scale
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                // Add to zip
                val entry = ZipEntry("page_${i + 1}.png")
                zipOut.putNextEntry(entry)
                
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                zipOut.closeEntry()
                
                page.close()
                bitmap.recycle()
                
                onProgress(i + 1, pageCount)
            }
            zipOut.finish()
            zipOut.close()
            renderer.close()
        } finally {
            pfd.close()
        }
    }

    /**
     * Gets total page count of a PDF using Android PdfRenderer.
     */
    fun getPdfPageCount(context: Context, pdfUri: Uri): Int {
        val contentResolver = context.contentResolver
        return contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            val pages = renderer.pageCount
            renderer.close()
            pages
        } ?: 0
    }

    /**
     * Combines multiple images into a single PDF.
     */
    fun combineImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        pageSizeOption: String, // "A4", "LETTER", "AUTO"
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val total = imageUris.size
        
        // Initial page size placeholder; adjusted depending on options
        val docSize = when (pageSizeOption.uppercase()) {
            "A4" -> PageSize.A4
            "LETTER" -> PageSize.LETTER
            else -> PageSize.A4 // Default or will adjust dynamically below
        }
        
        val doc = Document(docSize, 36f, 36f, 36f, 36f)
        val writer = PdfWriter.getInstance(doc, outputStream)
        
        doc.open()
        
        for ((index, uri) in imageUris.withIndex()) {
            val bytes = readBytesFromUri(context, uri) ?: continue
            val image = Image.getInstance(bytes)
            
            if (pageSizeOption.uppercase() == "AUTO") {
                // Adjust page size dynamically to accommodate the image dimensions
                val rect = Rectangle(image.width, image.height)
                doc.setPageSize(rect)
                doc.newPage()
                image.setAbsolutePosition(0f, 0f)
                doc.add(image)
            } else {
                // Scale image to fit within A4 or Letter page margins
                doc.newPage()
                val targetWidth = doc.pageSize.width - doc.leftMargin() - doc.rightMargin()
                val targetHeight = doc.pageSize.height - doc.topMargin() - doc.bottomMargin()
                
                image.scaleToFit(targetWidth, targetHeight)
                
                // Center image
                val x = (doc.pageSize.width - image.scaledWidth) / 2f
                val y = (doc.pageSize.height - image.scaledHeight) / 2f
                image.setAbsolutePosition(x, y)
                doc.add(image)
            }
            onProgress(index + 1, total)
        }
        
        doc.close()
        writer.close()
    }

    /**
     * Merges multiple PDF files into one.
     */
    fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ) {
        val doc = Document()
        val copy = PdfCopy(doc, outputStream)
        doc.open()
        
        val total = pdfUris.size
        
        for ((index, uri) in pdfUris.withIndex()) {
            val bytes = readBytesFromUri(context, uri) ?: continue
            val reader = PdfReader(bytes)
            val numPages = reader.numberOfPages
            
            for (p in 1..numPages) {
                copy.addPage(copy.getImportedPage(reader, p))
            }
            reader.close()
            onProgress(index + 1, total)
        }
        
        doc.close()
        copy.close()
    }

    /**
     * Extracts a page range from a PDF.
     */
    fun splitPdf(
        context: Context,
        pdfUri: Uri,
        startPage: Int,
        endPage: Int,
        outputStream: OutputStream
    ) {
        val bytes = readBytesFromUri(context, pdfUri) ?: throw Exception("Failed to read file bytes")
        val reader = PdfReader(bytes)
        val numPages = reader.numberOfPages
        
        if (startPage < 1 || endPage > numPages || startPage > endPage) {
            reader.close()
            throw IllegalArgumentException("No valid pages in range.")
        }
        
        val doc = Document()
        val copy = PdfCopy(doc, outputStream)
        doc.open()
        
        for (p in startPage..endPage) {
            copy.addPage(copy.getImportedPage(reader, p))
        }
        
        doc.close()
        copy.close()
        reader.close()
    }

    /**
     * Encrypts a PDF file using AES-128 password encryption.
     */
    fun protectPdf(
        context: Context,
        pdfUri: Uri,
        password: java.lang.String,
        outputStream: OutputStream
    ) {
        val bytes = readBytesFromUri(context, pdfUri) ?: throw Exception("Failed to read PDF bytes")
        val reader = PdfReader(bytes)
        
        val stamper = PdfStamper(reader, outputStream)
        stamper.setEncryption(
            password.bytes,
            password.bytes,
            PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
            PdfWriter.ENCRYPTION_AES_128
        )
        
        stamper.close()
        reader.close()
    }

    /**
     * Stitche page numbers onto an existing PDF with coordinates and custom font.
     */
    fun addPageNumbers(
        context: Context,
        pdfUri: Uri,
        position: String, // "top-left", "top-center", "top-right", "bottom-left", "bottom-center", "bottom-right"
        startNumber: Int,
        outputStream: OutputStream
    ) {
        val bytes = readBytesFromUri(context, pdfUri) ?: throw Exception("Failed to read PDF bytes")
        val reader = PdfReader(bytes)
        val totalPages = reader.numberOfPages
        
        val stamper = PdfStamper(reader, outputStream)
        val baseFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED)
        
        val fontSize = 11f
        val margin = 36f // Margin from edges
        
        for (i in 1..totalPages) {
            val overContent = stamper.getOverContent(i)
            val pageSize = reader.getPageSize(i)
            val width = pageSize.width
            val height = pageSize.height
            
            // Current page number designation
            val currentNumber = startNumber + (i - 1)
            val labelText = currentNumber.toString()
            
            val alignment: Int
            val x: Float
            val y: Float
            
            // X positioning
            when {
                position.contains("left") -> {
                    alignment = com.itextpdf.text.Element.ALIGN_LEFT
                    x = margin
                }
                position.contains("center") -> {
                    alignment = com.itextpdf.text.Element.ALIGN_CENTER
                    x = width / 2f
                }
                else -> { // right
                    alignment = com.itextpdf.text.Element.ALIGN_RIGHT
                    x = width - margin
                }
            }
            
            // Y positioning
            when {
                position.contains("top") -> {
                    y = height - margin
                }
                else -> { // bottom
                    y = margin
                }
            }
            
            overContent.beginText()
            overContent.setFontAndSize(baseFont, fontSize)
            overContent.showTextAligned(alignment, labelText, x, y, 0f)
            overContent.endText()
        }
        
        stamper.close()
        reader.close()
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 4096
            val buffer = ByteArray(bufferSize)
            var len: Int
            if (inputStream != null) {
                while (inputStream.read(buffer).also { len = it } != -1) {
                    byteBuffer.write(buffer, 0, len)
                }
                inputStream.close()
            }
            byteBuffer.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
