package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.FlippYellow
import com.example.ui.theme.MintColor
import com.example.ui.theme.WhiteColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfToPngScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: File Selected & Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeStr by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }

    var formatOption by rememberSaveable { mutableStateOf("PNG") }
    var scaleOption by rememberSaveable { mutableIntStateOf(2) } // 1x, 2x, 3x

    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val pdfThumbnails = remember { mutableStateListOf<Bitmap>() }
    var tempZipFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tempZipFile?.delete()
            pdfThumbnails.forEach { it.recycle() }
            pdfThumbnails.clear()
        }
    }

    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null && tempZipFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempZipFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "ZIP saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving zip: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    FeaturePageScaffold(
        title = "PDF to Images",
        desc = "Convert every page of a PDF into high-quality PNG images. Perfect for previews, thumbnails, or sharing individual pages.",
        badgeLabel = "PDF → PNG",
        onBack = onBack,
        badgeColor = FlippYellow
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle / Empty
                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Please select a PDF file.",
                    badgeColor = FlippYellow,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        fileName = FileUtils.getFileName(context, uri)
                        fileSizeStr = FileUtils.getFileSizeString(context, uri)
                        stage = 2

                        coroutineScope.launch {
                            pdfThumbnails.clear()
                            val thumbs = withContext(Dispatchers.IO) {
                                try {
                                    val count = PdfUtils.getPdfPageCount(context, uri)
                                    totalPages = count
                                } catch (_: Exception) {}
                                loadPdfThumbnails(context, uri, maxPages = 6)
                            }
                            if (thumbs.isEmpty() && totalPages == 0) {
                                errorMessage = "Failed to load PDF. Is it a valid, non-encrypted file?"
                                stage = 5
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            } else {
                                pdfThumbnails.addAll(thumbs)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select a PDF file from your device",
                    step2 = "Choose resolution (1x, 2x, 3x) and options",
                    step3 = "Export and download as a secure ZIP pack"
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Settings
                SelectedFileCard(
                    fileName = fileName.ifEmpty { "document.pdf" },
                    fileSizeStr = fileSizeStr,
                    pageCount = if (totalPages > 0) totalPages else pdfThumbnails.size.takeIf { it > 0 },
                    fileTypeBadge = "PDF",
                    badgeColor = FlippYellow,
                    previewBitmap = pdfThumbnails.firstOrNull(),
                    onChangeFile = {
                        selectedPdfUri = null
                        pdfThumbnails.clear()
                        stage = 1
                    }
                )

                CollapsibleSettingsCard(title = "Output Format & Resolution") {
                    NeobrutalistPillGroup(
                        title = "OUTPUT FORMAT",
                        options = listOf("PNG — Best quality", "JPG — Smaller size"),
                        selectedOption = if (formatOption == "PNG") "PNG — Best quality" else "JPG — Smaller size",
                        onOptionSelected = { chosen ->
                            formatOption = if (chosen.startsWith("PNG")) "PNG" else "JPG"
                        },
                        labelProvider = { it },
                        selectedColor = FlippYellow
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NeobrutalistPillGroup(
                        title = "RESOLUTION",
                        options = listOf(1, 2, 3),
                        selectedOption = scaleOption,
                        onOptionSelected = { scaleOption = it },
                        labelProvider = { "${it}x" },
                        subtextProvider = { scale ->
                            when (scale) {
                                1 -> "Standard"
                                2 -> "Recommended"
                                3 -> "Ultra-sharp"
                                else -> null
                            }
                        },
                        selectedColor = FlippYellow
                    )
                }

                if (pdfThumbnails.isNotEmpty()) {
                    Text(
                        text = "Page Previews (${pdfThumbnails.size} pages loaded)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(pdfThumbnails) { bitmap ->
                            Card(
                                modifier = Modifier
                                    .height(80.dp)
                                    .border(1.5.dp, BlackColor, RoundedCornerShape(6.dp)),
                                colors = CardDefaults.cardColors(containerColor = WhiteColor)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "PDF Page Thumbnail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                BrutalistButton(
                    text = "Convert All Pages →",
                    onClick = {
                        stage = 3
                        progressVal = 0.1f
                        progressLabel = "Starting conversion..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("converted_zip_", ".zip", context.cacheDir)
                                tempZipFile = tempFile

                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.convertPdfToPngZip(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        scale = scaleOption,
                                        outputStream = fos
                                    ) { current, total ->
                                        progressVal = current.toFloat() / total
                                        progressLabel = "Converting page $current of $total... (${(progressVal * 100).toInt()}%)"
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Processing complete!", Toast.LENGTH_SHORT).show()
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                tempZipFile?.delete()
                                tempZipFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = if (e is SecurityException || e.message?.contains("password", ignoreCase = true) == true) {
                                        "This PDF is password-protected. Please unlock it before converting."
                                    } else {
                                        "Conversion failed: ${e.message ?: "Invalid PDF file"}"
                                    }
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = FlippYellow,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = progressLabel.ifEmpty { "Converting pages..." },
                    stepDetails = "Rendering high-res bitmap (${scaleOption}x resolution) locally on your device"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "✓ Conversion complete!",
                    subtitle = "All pages have been converted locally to PNG images and compiled into a secure ZIP pack.",
                    primaryButtonText = "Download All as ZIP 📦",
                    onPrimaryClick = { fileSaver.launch("converted_pages.zip") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        pdfThumbnails.clear()
                        tempZipFile?.delete()
                        tempZipFile = null
                        stage = 1
                    },
                    accentColor = MintColor
                )
            }

            5 -> {
                // Error State
                ErrorCard(
                    errorMessage = errorMessage.ifEmpty { "An unexpected error occurred while processing the PDF." },
                    onDismissOrRetry = {
                        stage = 1
                    },
                    retryButtonText = "Select Another PDF"
                )
            }
        }
    }
}
