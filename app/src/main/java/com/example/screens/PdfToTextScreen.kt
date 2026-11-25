package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.CreamColor
import com.example.ui.theme.FlippYellow
import com.example.ui.theme.MintColor
import com.example.ui.theme.SkyBlueColor
import com.example.ui.theme.WhiteColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfToTextScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: File Selected, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeStr by remember { mutableStateOf<String?>(null) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var previewThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewThumbnail?.recycle()
        }
    }

    var extractedText by rememberSaveable { mutableStateOf("") }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }

    val txtSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(extractedText.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Text saved successfully!", Toast.LENGTH_SHORT).show()
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
        title = "PDF to Text",
        desc = "Extract all text from a PDF document.",
        badgeLabel = "PDF → TXT",
        onBack = onBack,
        badgeColor = SkyBlueColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Drag files here or",
                    onCancelledMessage = "",
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
                    step1 = "Select unprotected PDF document from your device",
                    step2 = "Extract local text streams without cloud upload",
                    step3 = "Copy text directly or save as a .txt file"
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Action
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

                BrutalistButton(
                    text = "Extract Text Now →",
                    onClick = {
                        stage = 3
                        progressVal = 0f
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val text = PdfUtils.extractTextFromPdf(context, selectedPdfUri!!) { current, total ->
                                    progressVal = current.toFloat() / total.toFloat()
                                }
                                withContext(Dispatchers.Main) {
                                    extractedText = text
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Failed to extract text. Is it a valid PDF?"
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
                    statusText = "Extracting text from PDF pages...",
                    stepDetails = "Parsing text stream locally using iTextG"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "🎉 Text Extracted Successfully!",
                    subtitle = "${extractedText.length} characters extracted from $totalPages pages.",
                    primaryButtonText = "Download as .TXT 📁",
                    onPrimaryClick = { txtSaver.launch("extracted_text.txt") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        extractedText = ""
                        previewThumbnail = null
                        stage = 1
                    },
                    accentColor = MintColor
                )

                if (extractedText.isBlank()) {
                    BrutalistShadowBox(
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No text could be extracted. 😕", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This usually means the PDF is scanned or consists entirely of images. OCR is required to extract text from image-based PDFs, which is not currently supported in this app.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { extractedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                            .semantics { contentDescription = "Extracted document text" },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalistButton(
                            text = "Copy Text",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Extracted Text", extractedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            backgroundColor = WhiteColor,
                            modifier = Modifier.weight(1f)
                        )

                        BrutalistButton(
                            text = "Save as .TXT",
                            onClick = { txtSaver.launch("extracted_text.txt") },
                            backgroundColor = MintColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            5 -> {
                // Error State
                ErrorCard(
                    errorMessage = errorMessage.ifEmpty { "Failed to extract text from the selected PDF." },
                    onDismissOrRetry = { stage = 1 },
                    retryButtonText = "Select Another PDF"
                )
            }
        }
    }
}
