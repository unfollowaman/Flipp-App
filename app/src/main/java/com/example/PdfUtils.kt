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
import com.itextpdf.text.exceptions.BadPasswordException
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
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
        
        try {
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            
            // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PNG compression
            val zipOut = ZipOutputStream(outputStream.buffered())
            // Optimize: PNG images are already compressed using zlib DEFLATE in Bitmap.compress.
            // Setting zip compression level to NO_COMPRESSION bypasses a redundant second DEFLATE pass, saving CPU time.
            zipOut.setLevel(Deflater.NO_COMPRESSION)
            
            var reusableBitmap: Bitmap? = null
            try {
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)

                    // Scale width/height by resolution options (1x, 2x, 3x)
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

                    // Add to zip
                    val entry = ZipEntry("page_${i + 1}.png")
                    zipOut.putNextEntry(entry)

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                    zipOut.closeEntry()

                    page.close()

                    onProgress(i + 1, pageCount)
                }
            } finally {
                reusableBitmap?.recycle()
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
        
        var pagesAdded = 0
        try {
            doc.open()
            
            for ((index, uri) in imageUris.withIndex()) {
                val bytes = readBytesFromUri(context, uri)
                if (bytes == null || bytes.isEmpty()) {
                    continue
                }
                val image = try {
                    Image.getInstance(bytes)
                } catch (e: Exception) {
                    AppLogger.e("PdfUtils", "Skipping invalid image format at URI: $uri", e)
                    continue
                }

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
                pagesAdded++
                onProgress(index + 1, total)
            }
            if (pagesAdded == 0) {
                throw IllegalArgumentException("No valid images could be converted to PDF.")
            }
        } finally {
            if (doc.isOpen && pagesAdded > 0) {
                doc.close()
            }
            writer.close()
            bufferedOut.flush()
        }
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
        // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF merge
        val bufferedOut = outputStream.buffered()
        val doc = Document()
        val copy = PdfCopy(doc, bufferedOut)
        var pagesAdded = 0
        try {
            doc.open()
            val total = pdfUris.size

            for ((index, uri) in pdfUris.withIndex()) {
                val inputStream = context.contentResolver.openInputStream(uri) ?: continue
                try {
                    val reader = PdfReader(inputStream)
                    try {
                        val numPages = reader.numberOfPages
                        for (p in 1..numPages) {
                            copy.addPage(copy.getImportedPage(reader, p))
                            pagesAdded++
                        }
                        // Optimize memory: Free reader internal objects in PdfCopy after importing pages to allow early GC
                        copy.freeReader(reader)
                    } finally {
                        reader.close()
                    }
                } finally {
                    inputStream.close()
                }
                onProgress(index + 1, total)
            }

            if (pagesAdded == 0) {
                throw IllegalArgumentException("No readable pages found to merge.")
            }
        } finally {
            if (doc.isOpen && pagesAdded > 0) {
                try {
                    doc.close()
                } catch (e: Exception) {
                    AppLogger.e("PdfUtils", "Error closing Document during merge", e)
                }
            }
            try {
                copy.close()
            } catch (e: Exception) {
                AppLogger.e("PdfUtils", "Error closing PdfCopy during merge", e)
            }
            bufferedOut.flush()
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
            ?: throw FileNotFoundException("Failed to open file descriptor: $pdfUri")
        try {
            val reader = PdfReader(inputStream)
            try {
                val numPages = reader.numberOfPages

                if (startPage < 1 || endPage > numPages || startPage > endPage) {
                    throw IllegalArgumentException("No valid pages in range.")
                }

                // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF split
                val bufferedOut = outputStream.buffered()
                val doc = Document()
                val copy = PdfCopy(doc, bufferedOut)
                try {
                    doc.open()
                    for (p in startPage..endPage) {
                        copy.addPage(copy.getImportedPage(reader, p))
                    }
                    // Optimize memory: Free reader internal objects in PdfCopy after importing pages
                    copy.freeReader(reader)
                } finally {
                    if (doc.isOpen) {
                        try {
                            doc.close()
                        } catch (e: Exception) {
                            AppLogger.e("PdfUtils", "Error closing Document during split", e)
                        }
                    }
                    try {
                        copy.close()
                    } catch (e: Exception) {
                        AppLogger.e("PdfUtils", "Error closing PdfCopy during split", e)
                    }
                    bufferedOut.flush()
                }
            } finally {
                reader.close()
            }
        } finally {
            inputStream.close()
        }
    }

    /**
     * Encrypts a PDF file using AES-256 password encryption with standard UTF-8 encoding
     * and a distinct cryptographically secure owner password.
     */
    fun protectPdf(
        context: Context,
        pdfUri: Uri,
        password: CharSequence,
        outputStream: OutputStream
    ) {
        val pwd = password.toString()
        if (pwd.isEmpty()) {
            throw IllegalArgumentException("Password cannot be empty.")
        }

        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw FileNotFoundException("Failed to open file descriptor: $pdfUri")
        try {
            val reader = try {
                PdfReader(inputStream)
            } catch (e: BadPasswordException) {
                throw IllegalArgumentException("The selected PDF is already password-protected.", e)
            }
            try {
                // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF encryption
                val bufferedOut = outputStream.buffered()
                val stamper = PdfStamper(reader, bufferedOut)
                try {
                    val userPasswordBytes = pwd.toByteArray(Charsets.UTF_8)
                    val ownerPasswordBytes = ByteArray(32).apply {
                        SecureRandom().nextBytes(this)
                    }
                    stamper.setEncryption(
                        userPasswordBytes,
                        ownerPasswordBytes,
                        PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
                        PdfWriter.ENCRYPTION_AES_256
                    )
                } finally {
                    try {
                        stamper.close()
                    } catch (e: Exception) {
                        AppLogger.e("PdfUtils", "Error closing PdfStamper during protectPdf", e)
                    }
                    bufferedOut.flush()
                }
            } finally {
                try {
                    reader.close()
                } catch (ignored: Exception) {}
            }
        } finally {
            inputStream.close()
        }
    }

    /**
     * Stitches page numbers onto an existing PDF with coordinates and custom font.
     * Uses getCropBox/getPageSize box coordinates (left, right, bottom, top) to ensure correct
     * placement on PDFs with non-zero origins and custom crop boxes.
     */
    fun addPageNumbers(
        context: Context,
        pdfUri: Uri,
        position: String, // "top-left", "top-center", "top-right", "bottom-left", "bottom-center", "bottom-right"
        startNumber: Int,
        outputStream: OutputStream
    ) {
        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw FileNotFoundException("Failed to open file descriptor: $pdfUri")
        try {
            val reader = try {
                PdfReader(inputStream)
            } catch (e: BadPasswordException) {
                throw IllegalArgumentException("The selected PDF is password-protected.", e)
            }
            try {
                val totalPages = reader.numberOfPages
                if (totalPages == 0) {
                    throw IllegalArgumentException("PDF contains no pages.")
                }

                // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF stamping
                val bufferedOut = outputStream.buffered()
                val stamper = PdfStamper(reader, bufferedOut)
                try {
                    val baseFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED)
                    val fontSize = 11f
                    val margin = 36f // Margin from edges

                    // Optimize: Evaluate position flags and alignment ONCE outside the page loop
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
                        val box = reader.getCropBox(i) ?: reader.getPageSize(i)
                        val left = box.left
                        val right = box.right
                        val bottom = box.bottom
                        val top = box.top

                        // Current page number designation
                        val currentNumber = startNumber + (i - 1)
                        val labelText = currentNumber.toString()

                        val x = when {
                            isLeft -> left + margin
                            isCenter -> (left + right) / 2f
                            else -> right - margin
                        }

                        val y = if (isTop) top - margin else bottom + margin

                        overContent.beginText()
                        overContent.setFontAndSize(baseFont, fontSize)
                        overContent.showTextAligned(alignment, labelText, x, y, 0f)
                        overContent.endText()
                    }
                } finally {
                    try {
                        stamper.close()
                    } catch (e: Exception) {
                        AppLogger.e("PdfUtils", "Error closing PdfStamper in addPageNumbers", e)
                    }
                    bufferedOut.flush()
                }
            } finally {
                try {
                    reader.close()
                } catch (ignored: Exception) {}
            }
        } finally {
            inputStream.close()
        }
    }

    /**
     * Obtains a font supporting Unicode (including Latin and Devanagari).
     * Attempts bundled asset font first, then system fallback fonts, and finally Helvetica.
     */
    fun getUnicodeFont(context: Context, fontSize: Float): Font {
        // 1. Try bundled Poppins font from assets (covers Latin and Devanagari)
        try {
            val assetPath = "fonts/Poppins-Regular.ttf"
            val fontBytes = context.assets.open(assetPath).use { it.readBytes() }
            if (fontBytes.isNotEmpty()) {
                val baseFont = BaseFont.createFont(
                    "Poppins-Regular.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontBytes,
                    null
                )
                return Font(baseFont, fontSize)
            }
        } catch (e: Exception) {
            AppLogger.d("PdfUtils", "Could not load bundled Poppins font from assets: ${e.message}")
        }

        // 2. Try Android system Noto Sans Devanagari or Noto Sans fallback
        val systemFonts = listOf(
            "/system/fonts/NotoSansDevanagari-Regular.ttf",
            "/system/fonts/NotoSans-Regular.ttf",
            "/system/fonts/DroidSansFallback.ttf"
        )
        for (fontPath in systemFonts) {
            val file = File(fontPath)
            if (file.exists() && file.canRead()) {
                try {
                    val baseFont = BaseFont.createFont(
                        fontPath,
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED
                    )
                    return Font(baseFont, fontSize)
                } catch (e: Exception) {
                    AppLogger.d("PdfUtils", "Could not load system font $fontPath: ${e.message}")
                }
            }
        }

        // 3. Fallback to standard Helvetica
        val baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED)
        return Font(baseFont, fontSize)
    }

    /**
     * Converts text to a PDF file with full Unicode support (including Devanagari/Hindi).
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

        try {
            doc.open()

            val font = getUnicodeFont(context, fontSize)
            val paragraph = Paragraph(text.ifEmpty { " " }, font)

            when (alignment.lowercase()) {
                "left" -> paragraph.alignment = Element.ALIGN_LEFT
                "center" -> paragraph.alignment = Element.ALIGN_CENTER
                "right" -> paragraph.alignment = Element.ALIGN_RIGHT
                "justified" -> paragraph.alignment = Element.ALIGN_JUSTIFIED
                else -> paragraph.alignment = Element.ALIGN_LEFT
            }

            doc.add(paragraph)
        } finally {
            if (doc.isOpen) {
                try {
                    doc.close()
                } catch (e: Exception) {
                    AppLogger.e("PdfUtils", "Error closing Document in textToPdf", e)
                }
            }
            try {
                writer.close()
            } catch (e: Exception) {
                AppLogger.e("PdfUtils", "Error closing PdfWriter in textToPdf", e)
            }
            bufferedOut.flush()
        }
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
    internal fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                if (bytes.isNotEmpty()) bytes else null
            }
        } catch (e: Exception) {
            AppLogger.e("PdfUtils", "Error reading bytes from URI", e)
            null
        }
    }
}
