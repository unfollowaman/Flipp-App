package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.MintColor
import com.example.ui.theme.SkyBlueColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun SplitPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: File Selected & Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeStr by remember { mutableStateOf<String?>(null) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var previewThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    var startPageStr by remember { mutableStateOf("1") }
    var endPageStr by remember { mutableStateOf("1") }

    var progressVal by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tempPdfFile?.delete()
            previewThumbnail?.recycle()
        }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempPdfFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempPdfFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Split PDF saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    FeaturePageScaffold(
        title = "Split PDF",
        desc = "Extract a specific page range from a PDF into a separate file.",
        badgeLabel = "SPLIT",
        onBack = onBack,
        badgeColor = SkyBlueColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Extract specific page range into a separate file.",
                    badgeColor = SkyBlueColor,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        fileName = FileUtils.getFileName(context, uri)
                        fileSizeStr = FileUtils.getFileSizeString(context, uri)

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val pages = PdfUtils.getPdfPageCount(context, uri)
                                val thumbs = loadPdfThumbnails(context, uri, maxPages = 1)
                                withContext(Dispatchers.Main) {
                                    totalPages = pages
                                    startPageStr = "1"
                                    endPageStr = pages.toString()
                                    previewThumbnail = thumbs.firstOrNull()
                                    stage = 2
                                }
                            } catch (e: Exception) {
                                tempPdfFile?.delete()
                                tempPdfFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Failed to load PDF. Is it a valid, non-encrypted file?"
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select the PDF file you wish to split",
                    step2 = "Specify the start and end page range to extract",
                    step3 = "Download the newly extracted PDF locally"
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Settings
                SelectedFileCard(
                    fileName = fileName.ifEmpty { "document.pdf" },
                    fileSizeStr = fileSizeStr,
                    pageCount = totalPages,
                    fileTypeBadge = "PDF",
                    badgeColor = SkyBlueColor,
                    previewBitmap = previewThumbnail,
                    onChangeFile = {
                        selectedPdfUri = null
                        previewThumbnail = null
                        stage = 1
                    }
                )

                CollapsibleSettingsCard(title = "Split Range Configuration") {
                    Text(
                        text = "Total Document Pages: $totalPages",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Page:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = startPageStr,
                                onValueChange = { if (it.all { char -> char.isDigit() }) startPageStr = it },
                                placeholder = { Text("1") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Start page" },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor
                                ),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End Page:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = endPageStr,
                                onValueChange = { if (it.all { char -> char.isDigit() }) endPageStr = it },
                                placeholder = { Text(totalPages.toString()) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "End page" },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                BrutalistButton(
                    text = "Extract Pages →",
                    onClick = {
                        val start = startPageStr.toIntOrNull() ?: 1
                        val end = endPageStr.toIntOrNull() ?: totalPages

                        if (start < 1 || end > totalPages || start > end) {
                            Toast.makeText(context, "No valid pages in range.", Toast.LENGTH_LONG).show()
                            return@BrutalistButton
                        }

                        stage = 3
                        progressVal = 0.5f

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("split_", ".pdf", context.cacheDir)
                                tempPdfFile = tempFile

                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.splitPdf(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        startPage = start,
                                        endPage = end,
                                        outputStream = fos
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Split Completed successfully!", Toast.LENGTH_SHORT).show()
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                tempPdfFile?.delete()
                                tempPdfFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = "No valid pages in range."
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = SkyBlueColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                val rangeStart = startPageStr.toIntOrNull() ?: 1
                val rangeEnd = endPageStr.toIntOrNull() ?: totalPages
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = "Extracting pages $rangeStart to $rangeEnd...",
                    stepDetails = "Exporting selected page range locally on your device"
                )
            }

            4 -> {
                // State 5: Completed
                val rangeStart = startPageStr.toIntOrNull() ?: 1
                val rangeEnd = endPageStr.toIntOrNull() ?: totalPages
                CompletedHeroCard(
                    title = "🎉 Pages Extracted Successfully!",
                    subtitle = "Extracted pages $rangeStart to $rangeEnd into a standalone PDF document.",
                    primaryButtonText = "Download Split PDF 📁",
                    onPrimaryClick = { pdfSaver.launch("split_pages_${rangeStart}_to_${rangeEnd}.pdf") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        previewThumbnail = null
                        tempPdfFile?.delete()
                        tempPdfFile = null
                        stage = 1
                    },
                    accentColor = MintColor
                )
            }

            5 -> {
                // Error State
                ErrorCard(
                    errorMessage = errorMessage.ifEmpty { "Failed to extract specified page range." },
                    onDismissOrRetry = {
                        stage = if (selectedPdfUri != null) 2 else 1
                    },
                    retryButtonText = "Adjust Page Range"
                )
            }
        }
    }
}
