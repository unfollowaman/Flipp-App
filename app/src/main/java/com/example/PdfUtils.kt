package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
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

        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val pageCount = renderer.pageCount
                val zipOut = ZipOutputStream(outputStream.buffered())
                zipOut.setLevel(Deflater.NO_COMPRESSION)

                try {
                    var reusableBitmap: Bitmap? = null
                    try {
                        for (i in 0 until pageCount) {
                            renderer.openPage(i).use { page ->
                                val width = page.width * scale
                                val height = page.height * scale

                                val bitmap = if (reusableBitmap != null && reusableBitmap.width == width && reusableBitmap.height == height) {
                                    reusableBitmap.eraseColor(Color.TRANSPARENT)
                                    reusableBitmap
                                } else {
                                    reusableBitmap?.recycle()
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                                        reusableBitmap = it
                                    }
                                }

                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val entry = ZipEntry("page_${i + 1}.png")
                                zipOut.putNextEntry(entry)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                                zipOut.closeEntry()

                                onProgress(i + 1, pageCount)
                            }
                        }
                    } finally {
                        reusableBitmap?.recycle()
                    }
                    zipOut.finish()
                } finally {
                    zipOut.close()
                }
            }
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
        val optionUpper = pageSizeOption.uppercase()
        val isAuto = optionUpper == "AUTO"
        
        // Initial page size placeholder; adjusted depending on options
        val docSize = when (optionUpper) {
            "A4" -> PageSize.A4
            "LETTER" -> PageSize.LETTER
            else -> PageSize.A4 // Default or will adjust dynamically below
        }
        
        // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF creation
        val bufferedOut = outputStream.buffered()
        val doc = Document(docSize, 36f, 36f, 36f, 36f)
        val writer = PdfWriter.getInstance(doc, bufferedOut)
        
        doc.open()
        
        for ((index, uri) in imageUris.withIndex()) {
            val bytes = readBytesFromUri(context, uri) ?: continue
            val image = Image.getInstance(bytes)

            if (isAuto) {
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
        val bufferedOut = outputStream.buffered()
        val doc = Document()
        val copy = PdfCopy(doc, bufferedOut)
        doc.open()
        
        try {
            val total = pdfUris.size
            for ((index, uri) in pdfUris.withIndex()) {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw java.io.FileNotFoundException("File not found: $uri")
                inputStream.use { stream ->
                    val reader = PdfReader(stream)
                    try {
                        val numPages = reader.numberOfPages
                        for (p in 1..numPages) {
                            copy.addPage(copy.getImportedPage(reader, p))
                        }
                        copy.freeReader(reader)
                    } finally {
                        reader.close()
                    }
                }
                onProgress(index + 1, total)
            }
            doc.close()
            copy.close()
        } catch (e: Throwable) {
            try {
                if (doc.isOpen) doc.close()
            } catch (_: Throwable) {}
            try {
                copy.close()
            } catch (_: Throwable) {}
            throw e
        }
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
        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw java.io.FileNotFoundException("Failed to open input stream: $pdfUri")
        inputStream.use { stream ->
            val reader = PdfReader(stream)
            try {
                val numPages = reader.numberOfPages
                if (startPage < 1 || endPage > numPages || startPage > endPage) {
                    throw IllegalArgumentException("No valid pages in range.")
                }

                val bufferedOut = outputStream.buffered()
                val doc = Document()
                val copy = PdfCopy(doc, bufferedOut)
                doc.open()
                try {
                    for (p in startPage..endPage) {
                        copy.addPage(copy.getImportedPage(reader, p))
                    }
                    doc.close()
                    copy.close()
                } catch (e: Throwable) {
                    try {
                        if (doc.isOpen) doc.close()
                    } catch (_: Throwable) {}
                    try {
                        copy.close()
                    } catch (_: Throwable) {}
                    throw e
                }
            } finally {
                reader.close()
            }
        }
    }

    /**
     * Encrypts a PDF file using AES-256 password encryption.
     */
    fun protectPdf(
        context: Context,
        pdfUri: Uri,
        password: java.lang.String,
        outputStream: OutputStream
    ) {
        val inputStream = context.contentResolver.openInputStream(pdfUri) ?: throw Exception("Failed to open input stream for PDF")
        inputStream.use { stream ->
            val reader = PdfReader(stream)

            // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF encryption
            val bufferedOut = outputStream.buffered()
            val stamper = PdfStamper(reader, bufferedOut)
            stamper.setEncryption(
                password.bytes,
                password.bytes,
                PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
                PdfWriter.ENCRYPTION_AES_256
            )

            stamper.close()
            reader.close()
        }
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
        context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
            val reader = PdfReader(inputStream)
            val totalPages = reader.numberOfPages
            
            // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF stamping
            val bufferedOut = outputStream.buffered()
            val stamper = PdfStamper(reader, bufferedOut)
            val baseFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED)
            
            val fontSize = 11f
            val margin = 36f // Margin from edges
            
            // Optimize: Evaluate position flags and alignment ONCE outside the page loop
            // to eliminate redundant string search pattern matching on every iteration.
            val isLeft = position.contains("left")
            val isCenter = position.contains("center")
            val isTop = position.contains("top")

            val alignment = when {
                isLeft -> Element.ALIGN_LEFT
                isCenter -> Element.ALIGN_CENTER
                else -> Element.ALIGN_RIGHT
            }

            for (i in 1..totalPages) {
                val overContent = stamper.getOverContent(i)
                val pageSize = reader.getPageSize(i)
                val width = pageSize.width
                val height = pageSize.height

                // Current page number designation
                val currentNumber = startNumber + (i - 1)
                val labelText = currentNumber.toString()

                val x = when {
                    isLeft -> margin
                    isCenter -> width / 2f
                    else -> width - margin
                }

                val y = if (isTop) height - margin else margin

                overContent.beginText()
                overContent.setFontAndSize(baseFont, fontSize)
                overContent.showTextAligned(alignment, labelText, x, y, 0f)
                overContent.endText()
            }
            
            stamper.close()
            reader.close()
        } ?: throw Exception("Failed to open file descriptor")
    }

    /**
     * Converts text to a PDF file.
     */
    fun textToPdf(
        context: Context,
        text: String,
        pageSizeOption: String, // "A4", "LETTER"
        fontSize: Float,
        margin: Float,
        alignment: String, // "left", "center", "right", "justified"
        outputStream: OutputStream
    ) {
        val docSize = when (pageSizeOption.uppercase()) {
            "A4" -> PageSize.A4
            "LETTER" -> PageSize.LETTER
            else -> PageSize.A4
        }

        // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF generation
        val bufferedOut = outputStream.buffered()
        val doc = Document(docSize, margin, margin, margin, margin)
        val writer = PdfWriter.getInstance(doc, bufferedOut)

        doc.open()

        val baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED)
        val font = Font(baseFont, fontSize)

        val paragraph = Paragraph(text.ifEmpty { " " }, font)

        when (alignment.lowercase()) {
            "left" -> paragraph.alignment = Element.ALIGN_LEFT
            "center" -> paragraph.alignment = Element.ALIGN_CENTER
            "right" -> paragraph.alignment = Element.ALIGN_RIGHT
            "justified" -> paragraph.alignment = Element.ALIGN_JUSTIFIED
            else -> paragraph.alignment = Element.ALIGN_LEFT
        }

        doc.add(paragraph)

        doc.close()
        writer.close()
    }


    /**
     * Extracts text from a PDF file.
     */
    fun extractTextFromPdf(
        context: Context,
        pdfUri: Uri,
        onProgress: (Int, Int) -> Unit
    ): String {
        val inputStream = context.contentResolver.openInputStream(pdfUri) ?: throw Exception("Failed to open input stream for PDF")
        return inputStream.use { stream ->
            val reader = PdfReader(stream)
            val numPages = reader.numberOfPages
            val stringBuilder = StringBuilder()
            // Optimize: Instead of unconditionally appending "\n\n" string objects on every page and calling .trim()
            // at the end (which allocates a secondary full String copy of the result), conditionally append page separators
            // with primitive char appends and check for non-blank page content to avoid unnecessary String allocations.
            for (i in 1..numPages) {
                val textFromPage = PdfTextExtractor.getTextFromPage(reader, i)
                if (!textFromPage.isNullOrBlank()) {
                    if (stringBuilder.isNotEmpty()) {
                        stringBuilder.append('\n').append('\n')
                    }
                    stringBuilder.append(textFromPage.trim())
                }
                onProgress(i, numPages)
            }
            reader.close()
            stringBuilder.toString()
        }
    }

    // Optimize: Use Kotlin's `readBytes()` on InputStream with `use` block.
    // This reduces manual byte buffer copying overhead and ensures stream is safely closed.
    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: ByteArray(0)
        } catch (e: Exception) {
            null
        }
    }
}
