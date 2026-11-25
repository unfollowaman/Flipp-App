package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.ui.theme.AmberColor
import com.example.ui.theme.BlackColor
import com.example.ui.theme.MintColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddPageNumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: File Selected & Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeStr by remember { mutableStateOf<String?>(null) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var previewThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    var position by rememberSaveable { mutableStateOf("bottom-center") }
    var startNumStr by remember { mutableStateOf("1") }

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
                            Toast.makeText(context, "Page numbered PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    FeaturePageScaffold(
        title = "Add Page Numbers",
        desc = "Add page numbers to an existing PDF. Choose the page to start from and exactly where the numbers should appear.",
        badgeLabel = "PAGES",
        onBack = onBack,
        badgeColor = AmberColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Stamp page numbers with customizable position and offset start page.",
                    badgeColor = MintColor,
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
                                    previewThumbnail = thumbs.firstOrNull()
                                    stage = 2
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    stage = 2
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select PDF document to stamp",
                    step2 = "Choose anchor position and starting page number",
                    step3 = "Export document with sequential numbering"
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Settings
                SelectedFileCard(
                    fileName = fileName.ifEmpty { "document.pdf" },
                    fileSizeStr = fileSizeStr,
                    pageCount = totalPages,
                    fileTypeBadge = "PDF",
                    badgeColor = AmberColor,
                    previewBitmap = previewThumbnail,
                    onChangeFile = {
                        selectedPdfUri = null
                        previewThumbnail = null
                        stage = 1
                    }
                )

                CollapsibleSettingsCard(title = "Page Number Configuration") {
                    Text(
                        text = "Page Number Index Start Offset:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = startNumStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) startNumStr = it },
                        placeholder = { Text("1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlackColor,
                            unfocusedBorderColor = BlackColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Page number start offset" },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PositionGridSelector(
                        selectedPosition = position,
                        onPositionSelected = { position = it },
                        positions = PAGE_NUM_POSITIONS_LIST
                    )
                }

                BrutalistButton(
                    text = "Convert All Pages →",
                    onClick = {
                        stage = 3
                        progressVal = 0.5f

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("numbered_", ".pdf", context.cacheDir)
                                tempPdfFile = tempFile

                                val startNum = startNumStr.toIntOrNull() ?: 1

                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.addPageNumbers(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        position = position,
                                        startNumber = startNum,
                                        outputStream = fos
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Page Stamping complete!", Toast.LENGTH_SHORT).show()
                                    stage = 4
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
                    },
                    backgroundColor = AmberColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = "Stamping page digits sequentially...",
                    stepDetails = "Calculating coordinate anchors and rendering text locally on your device"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "🎉 PDF Stamped Successfully!",
                    subtitle = "Page digits have been stamped sequentially across your document.",
                    primaryButtonText = "Download Combined PDF 📁",
                    onPrimaryClick = { pdfSaver.launch("page_numbered_document.pdf") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        startNumStr = "1"
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
                    errorMessage = errorMessage.ifEmpty { "Failed to stamp page numbers on the document." },
                    onDismissOrRetry = {
                        stage = if (selectedPdfUri != null) 2 else 1
                    },
                    retryButtonText = "Try Again"
                )
            }
        }
    }
}
