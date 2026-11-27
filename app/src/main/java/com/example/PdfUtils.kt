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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ConvertedPageResult(
    val pageIndex: Int,
    val filename: String,
    val file: File,
    val bitmap: Bitmap?
)

data class ConversionResult(
    val pages: List<ConvertedPageResult>,
    val zipFile: File
)

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
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        orientation: String = "PORTRAIT" // "PORTRAIT", "LANDSCAPE"
    ) {
        val total = imageUris.size
        val optionUpper = pageSizeOption.uppercase()
        val isAuto = optionUpper == "AUTO"
        val isLandscape = orientation.uppercase() == "LANDSCAPE"
        
        // Initial page size placeholder; adjusted depending on options
        val baseSize = when (optionUpper) {
            "A4" -> PageSize.A4
            "LETTER" -> PageSize.LETTER
            else -> PageSize.A4 // Default or will adjust dynamically below
        }
        val docSize = if (isLandscape && !isAuto) baseSize.rotate() else baseSize
        
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
        password: String,
        outputStream: OutputStream
    ) {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream for PDF")
        inputStream.use { stream ->
            val reader = PdfReader(stream)
            try {
                // Optimize I/O: Wrap outputStream in BufferedOutputStream to minimize disk write system calls during PDF encryption
                val bufferedOut = outputStream.buffered()
                val stamper = PdfStamper(reader, bufferedOut)
                stamper.setEncryption(
                    password.toByteArray(),
                    password.toByteArray(),
                    PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY,
                    PdfWriter.ENCRYPTION_AES_256
                )
                stamper.close()
            } finally {
                reader.close()
            }
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

    /**
     * Converts PDF pages to individual image files (PNG/JPG) and compiles them into a ZIP archive.
     */
    fun convertPdfToImages(
        context: Context,
        pdfUri: Uri,
        format: String = "PNG",
        scale: Int = 2,
        pageRange: IntRange? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): ConversionResult {
        val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: throw Exception("Failed to open file descriptor for PDF")

        val isJpg = format.uppercase() == "JPG" || format.uppercase() == "JPEG"
        val extension = if (isJpg) "jpg" else "png"
        val compressFormat = if (isJpg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val compressQuality = if (isJpg) 85 else 100

        val results = mutableListOf<ConvertedPageResult>()
        val zipFile = File.createTempFile("converted_images_", ".zip", context.cacheDir)

        pfd.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            val totalPages = renderer.pageCount
            val range = pageRange ?: (0 until totalPages)
            val validRange = range.filter { it in 0 until totalPages }
            val count = validRange.size

            ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zipOut ->
                if (!isJpg) {
                    zipOut.setLevel(Deflater.NO_COMPRESSION)
                }

                for ((index, pageIndex) in validRange.withIndex()) {
                    val page = renderer.openPage(pageIndex)
                    val width = page.width * scale
                    val height = page.height * scale
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    if (isJpg) {
                        bitmap.eraseColor(Color.WHITE)
                    }
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val filename = "page-${String.format("%03d", pageIndex + 1)}.$extension"
                    val pageFile = File(context.cacheDir, filename)
                    FileOutputStream(pageFile).use { fos ->
                        bitmap.compress(compressFormat, compressQuality, fos)
                    }

                    val zipEntry = ZipEntry(filename)
                    zipOut.putNextEntry(zipEntry)
                    pageFile.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()

                    // Downsample thumbnail for UI preview
                    val thumbScale = 300f / maxOf(width, height)
                    val thumbWidth = maxOf(1, (width * thumbScale).toInt())
                    val thumbHeight = maxOf(1, (height * thumbScale).toInt())
                    val thumbBitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, true)
                    bitmap.recycle()

                    results.add(
                        ConvertedPageResult(
                            pageIndex = pageIndex,
                            filename = filename,
                            file = pageFile,
                            bitmap = thumbBitmap
                        )
                    )

                    onProgress(index + 1, count)
                }
            }
            renderer.close()
        }
        return ConversionResult(pages = results, zipFile = zipFile)
    }

    /**
     * Splits a PDF into multiple documents of N pages each, bundled into a ZIP file.
     */
    fun splitPdfEveryNPages(
        context: Context,
        pdfUri: Uri,
        n: Int,
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) {
        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        val reader = PdfReader(inputStream)
        val numPages = reader.numberOfPages
        if (n < 1 || numPages == 0) {
            reader.close()
            inputStream.close()
            throw IllegalArgumentException("Invalid split configuration")
        }

        val chunks = (1..numPages).chunked(n)
        val zipOut = ZipOutputStream(outputStream.buffered())

        for ((index, chunk) in chunks.withIndex()) {
            val start = chunk.first()
            val end = chunk.last()
            val entryName = "split_part_${index + 1}_pages_${start}-${end}.pdf"
            zipOut.putNextEntry(ZipEntry(entryName))

            val doc = Document()
            val copy = PdfCopy(doc, zipOut)
            doc.open()

            for (p in chunk) {
                copy.addPage(copy.getImportedPage(reader, p))
            }

            doc.close()
            copy.close()
            zipOut.closeEntry()
            onProgress(index + 1, chunks.size)
        }

        zipOut.finish()
        zipOut.close()
        reader.close()
        inputStream.close()
    }

    /**
     * Checks whether a PDF file is encrypted/password-protected.
     */
    fun isPdfEncrypted(context: Context, pdfUri: Uri): Boolean {
        try {
            context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
                try {
                    val renderer = PdfRenderer(pfd)
                    renderer.close()
                } catch (e: SecurityException) {
                    return true
                }
            }
        } catch (e: Throwable) {
            // Check via PdfReader
        }
        return try {
            context.contentResolver.openInputStream(pdfUri)?.use { stream ->
                val reader = PdfReader(stream)
                try {
                    reader.isEncrypted
                } finally {
                    reader.close()
                }
            } ?: false
        } catch (e: BadPasswordException) {
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Decrypts an encrypted PDF with the provided password.
     */
    fun unlockPdf(
        context: Context,
        pdfUri: Uri,
        password: String,
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            val reader = PdfReader(s, password.toByteArray())
            try {
                val stamper = PdfStamper(reader, outputStream.buffered())
                stamper.close()
            } finally {
                reader.close()
            }
        }
    }

    /**
     * Stamps a signature image onto the specified page of a PDF.
     */
    fun signPdf(
        context: Context,
        pdfUri: Uri,
        signatureBitmap: Bitmap,
        pageIndex: Int, // 1-based index
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float,
        heightRatio: Float,
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            val reader = PdfReader(s)
            val stamper = PdfStamper(reader, outputStream.buffered())
            val targetPage = if (pageIndex in 1..reader.numberOfPages) pageIndex else 1
            val overContent = stamper.getOverContent(targetPage)
            val pageSize = reader.getPageSize(targetPage)

            val streamBytes = ByteArrayOutputStream()
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, streamBytes)
            val image = Image.getInstance(streamBytes.toByteArray())

            val targetWidth = pageSize.width * widthRatio.coerceIn(0.1f, 0.9f)
            val targetHeight = (targetWidth / signatureBitmap.width) * signatureBitmap.height
            image.scaleToFit(targetWidth, targetHeight)

            val x = (pageSize.width * xRatio).coerceIn(0f, pageSize.width - targetWidth)
            val y = (pageSize.height * (1f - yRatio) - targetHeight).coerceIn(0f, pageSize.height - targetHeight)

            image.setAbsolutePosition(x, y)
            overContent.addImage(image)

            stamper.close()
            reader.close()
        }
    }

    /**
     * Deletes specified pages from a PDF.
     */
    fun deletePdfPages(
        context: Context,
        pdfUri: Uri,
        pagesToDelete: Set<Int>, // 1-based page indices
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            val reader = PdfReader(s)
            val total = reader.numberOfPages
            val remainingPages = (1..total).filter { it !in pagesToDelete }
            if (remainingPages.isEmpty()) {
                reader.close()
                throw IllegalArgumentException("Cannot delete all pages from PDF")
            }

            val doc = Document()
            val copy = PdfCopy(doc, outputStream.buffered())
            doc.open()

            for (p in remainingPages) {
                copy.addPage(copy.getImportedPage(reader, p))
            }

            doc.close()
            copy.close()
            reader.close()
        }
    }

    /**
     * Reorders pages in a PDF according to the given order list.
     */
    fun reorderPdfPages(
        context: Context,
        pdfUri: Uri,
        newOrder: List<Int>, // 1-based page indices
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            val reader = PdfReader(s)
            val total = reader.numberOfPages
            val validOrder = newOrder.filter { it in 1..total }
            if (validOrder.isEmpty()) {
                reader.close()
                throw IllegalArgumentException("Invalid reorder list")
            }

            val doc = Document()
            val copy = PdfCopy(doc, outputStream.buffered())
            doc.open()

            for (p in validOrder) {
                copy.addPage(copy.getImportedPage(reader, p))
            }

            doc.close()
            copy.close()
            reader.close()
        }
    }

    /**
     * Compresses a PDF file using full object stream compression.
     */
    fun compressPdf(
        context: Context,
        pdfUri: Uri,
        level: String = "RECOMMENDED", // "RECOMMENDED", "MAXIMUM"
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            PdfReader.unethicalreading = true
            val reader = PdfReader(s)
            val stamper = PdfStamper(reader, outputStream.buffered())
            stamper.setFullCompression()
            stamper.writer.compressionLevel = 9
            stamper.close()
            reader.close()
        }
    }

    /**
     * Converts a PDF to an offline-compatible Word .docx document.
     */
    fun convertPdfToWordDocx(
        context: Context,
        pdfUri: Uri,
        outputStream: OutputStream
    ) {
        val extractedText = extractTextFromPdf(context, pdfUri) { _, _ -> }
        val paragraphs = extractedText.split("\n\n").filter { it.isNotBlank() }

        val zipOut = ZipOutputStream(outputStream.buffered())

        // 1. [Content_Types].xml
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
        zipOut.write(contentTypes.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // 2. _rels/.rels
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
        zipOut.write(rels.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // 3. word/document.xml
        zipOut.putNextEntry(ZipEntry("word/document.xml"))
        val docXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
""")
            for (para in paragraphs) {
                append("    <w:p><w:r><w:t>")
                append(para.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"))
                append("</w:t></w:r></w:p>\n")
            }
            append("""    <w:sectPr/>
  </w:body>
</w:document>""")
        }
        zipOut.write(docXml.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        zipOut.finish()
        zipOut.close()
    }

    /**
     * Stamps text watermark onto each page of a PDF document.
     */
    fun addWatermarkToPdf(
        context: Context,
        pdfUri: Uri,
        watermarkText: String,
        opacity: Float = 0.5f,
        rotation: Float = 45f,
        fontSize: Float = 48f,
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            val reader = PdfReader(s)
            val stamper = PdfStamper(reader, outputStream.buffered())
            val baseFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED)
            val gState = com.itextpdf.text.pdf.PdfGState()
            gState.setFillOpacity(opacity.coerceIn(0.05f, 1f))

            for (i in 1..reader.numberOfPages) {
                val overContent = stamper.getOverContent(i)
                val pageSize = reader.getPageSize(i)
                val centerX = pageSize.width / 2f
                val centerY = pageSize.height / 2f

                overContent.saveState()
                overContent.setGState(gState)
                overContent.beginText()
                overContent.setFontAndSize(baseFont, fontSize)
                overContent.showTextAligned(Element.ALIGN_CENTER, watermarkText, centerX, centerY, rotation)
                overContent.endText()
                overContent.restoreState()
            }

            stamper.close()
            reader.close()
        }
    }

    /**
     * Stamps an annotation or markup overlay bitmap directly onto a page of a PDF.
     */
    fun stampOverlayOnPdfPage(
        context: Context,
        pdfUri: Uri,
        overlayBitmap: Bitmap,
        pageIndex: Int,
        outputStream: OutputStream
    ) {
        val stream = context.contentResolver.openInputStream(pdfUri)
            ?: throw Exception("Failed to open input stream")
        stream.use { s ->
            val reader = PdfReader(s)
            val stamper = PdfStamper(reader, outputStream.buffered())
            val targetPage = if (pageIndex in 1..reader.numberOfPages) pageIndex else 1
            val overContent = stamper.getOverContent(targetPage)
            val pageSize = reader.getPageSize(targetPage)

            val streamBytes = ByteArrayOutputStream()
            overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, streamBytes)
            val image = Image.getInstance(streamBytes.toByteArray())
            image.scaleToFit(pageSize.width, pageSize.height)
            image.setAbsolutePosition(0f, 0f)

            overContent.addImage(image)
            stamper.close()
            reader.close()
        }
    }
}
