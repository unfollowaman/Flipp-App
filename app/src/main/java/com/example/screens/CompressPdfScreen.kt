package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.CreamColor
import com.example.ui.theme.MintColor
import com.example.ui.theme.PinkColor
import com.example.ui.theme.SkyBlueColor
import com.example.ui.theme.WhiteColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun CompressPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var originalSizeBytes by rememberSaveable { mutableLongStateOf(0L) }
    var compressedSizeBytes by rememberSaveable { mutableLongStateOf(0L) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }

    var compressionPreset by rememberSaveable { mutableStateOf("RECOMMENDED") } // "RECOMMENDED", "MAXIMUM"
    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempCompressedFile by remember { mutableStateOf<File?>(null) }

    fun resetState() {
        thumbnails.forEach { it.recycle() }
        thumbnails.clear()
        tempCompressedFile?.delete()
        tempCompressedFile = null
        selectedPdfUri = null
        pdfFileName = ""
        originalSizeBytes = 0L
        compressedSizeBytes = 0L
        totalPages = 1
        progressVal = 0f
        stage = 1
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempCompressedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempCompressedFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Compressed PDF saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    fun startCompression() {
        val uri = selectedPdfUri ?: return
        stage = 2
        progressVal = 0.3f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "compressed_document.pdf")
                FileOutputStream(tempFile).use { fos ->
                    PdfUtils.compressPdf(
                        context = context,
                        pdfUri = uri,
                        level = compressionPreset,
                        outputStream = fos
                    )
                }

                val newSize = tempFile.length()
                withContext(Dispatchers.Main) {
                    compressedSizeBytes = newSize
                    tempCompressedFile = tempFile
                    progressVal = 1f
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Compression failed: ${e.message}", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    fun formatBytes(bytes: Long): String {
        return if (bytes < 1024 * 1024) {
            String.format("%.1f KB", bytes / 1024f)
        } else {
            String.format("%.2f MB", bytes / (1024f * 1024f))
        }
    }

    val savingsPercent: Int = if (originalSizeBytes > 0 && compressedSizeBytes > 0 && compressedSizeBytes < originalSizeBytes) {
        (((originalSizeBytes - compressedSizeBytes).toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
    } else {
        0
    }

    ToolScreenTemplate(
        title = "Compress PDF",
        desc = "Reduce file size while optimizing for maximal PDF quality.",
        badgeLabel = "COMPRESS",
        onBack = onBack,
        badgeColor = PinkColor
    ) {
        when (stage) {
            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select PDF Document",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                    Text(
                        text = "Compress streams and objects completely offline with zero data leakage.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    SingleFileDropZone(
                        mimeTypes = arrayOf("application/pdf"),
                        prompt = "Drag PDF here or",
                        onCancelledMessage = "",
                        onFileSelected = { uri ->
                            selectedPdfUri = uri
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                                    cursor?.use {
                                        if (it.moveToFirst()) {
                                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                                            if (nameIndex != -1) pdfFileName = it.getString(nameIndex) ?: "document.pdf"
                                            if (sizeIndex != -1) originalSizeBytes = it.getLong(sizeIndex)
                                        }
                                    }
                                } catch (_: Exception) {}

                                try {
                                    val count = PdfUtils.getPdfPageCount(context, uri)
                                    totalPages = count
                                    val thumbs = loadPdfThumbnails(context, uri, maxPages = 4)
                                    withContext(Dispatchers.Main) {
                                        thumbnails.clear()
                                        thumbnails.addAll(thumbs)
                                        stage = 2
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // How it works card
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "HOW IT WORKS 📦",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Select PDF file", "Pick any large PDF document from your device.", PinkColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Choose compression level", "Select Recommended (good quality) or Maximum (smallest size).", SkyBlueColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Download smaller PDF", "Save your optimized document with instant storage savings.", MintColor)
                        }
                    }
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DocumentSummaryHeader(
                        fileName = pdfFileName.ifEmpty { "document.pdf" },
                        pageCount = totalPages,
                        fileSize = formatBytes(originalSizeBytes),
                        accentColor = PinkColor
                    )

                    if (progressVal > 0f && progressVal < 1f) {
                        StageProgressBar(progress = progressVal, label = "Optimizing and compressing object streams...")
                    }

                    // Compression Preset Selector
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Compression Level",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("RECOMMENDED", "MAXIMUM"),
                                selectedOption = compressionPreset,
                                onOptionSelected = { compressionPreset = it },
                                labelProvider = { if (it == "RECOMMENDED") "Recommended" else "Maximum Compression" },
                                sublabelProvider = { if (it == "RECOMMENDED") "Good quality, ~40% smaller" else "Smallest size, max savings" },
                                activeColor = PinkColor
                            )
                        }
                    }

                    // Thumbnail Preview
                    if (thumbnails.isNotEmpty()) {
                        Text(
                            text = "Document Preview",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor
                        )
                        PageThumbnailGrid(
                            thumbnails = thumbnails,
                            modifier = Modifier.heightIn(max = 240.dp)
                        )
                    }

                    // Action CTA
                    BrutalistButton(
                        text = "Compress PDF ➔",
                        onClick = { startCompression() },
                        backgroundColor = PinkColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BrutalistButton(
                        text = "Select Different File ↺",
                        onClick = { resetState() },
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val savingsSubtitle = if (savingsPercent > 0) {
                        "✓ Saved $savingsPercent%! Reduced from ${formatBytes(originalSizeBytes)} to ${formatBytes(compressedSizeBytes)}."
                    } else {
                        "Document optimized. Ready to save (${formatBytes(compressedSizeBytes)})."
                    }

                    ResultsHeroBanner(
                        title = "✓ PDF compressed successfully!",
                        subtitle = savingsSubtitle,
                        primaryCtaText = "Download Compressed PDF 📁",
                        onPrimaryCta = {
                            val defaultName = pdfFileName.substringBeforeLast(".") + "_compressed.pdf"
                            pdfSaver.launch(defaultName)
                        },
                        onReset = { resetState() },
                        accentColor = MintColor
                    )

                    // Savings breakdown card
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "COMPRESSION STATS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Original Size:", fontSize = 13.sp, color = Color(0xFF555555))
                                Text(formatBytes(originalSizeBytes), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BlackColor)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Compressed Size:", fontSize = 13.sp, color = Color(0xFF555555))
                                Text(formatBytes(compressedSizeBytes), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MintColor)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Saved:", fontSize = 13.sp, color = Color(0xFF555555))
                                Text("$savingsPercent% reduction", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BlackColor)
                            }
                        }
                    }

                    BrutalistButton(
                        text = "Compress another file ↺",
                        onClick = { resetState() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
