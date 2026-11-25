package com.example

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.MintColor
import com.example.ui.theme.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun MergePdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: Files Selected & Queue, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    val selectedPdfUris = remember { mutableStateListOf<Uri>() }
    val pdfPageCounts = remember { mutableStateMapOf<Uri, Int>() }

    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tempPdfFile?.delete()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedPdfUris.addAll(uris)
                stage = 2

                coroutineScope.launch(Dispatchers.IO) {
                    uris.forEach { uri ->
                        if (!pdfPageCounts.containsKey(uri)) {
                            try {
                                val count = PdfUtils.getPdfPageCount(context, uri)
                                pdfPageCounts[uri] = count
                            } catch (_: Exception) {
                                pdfPageCounts[uri] = 0
                            }
                        }
                    }
                }
            } else if (selectedPdfUris.isEmpty()) {
                Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
            }
        }
    )

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
                            Toast.makeText(context, "Merged PDF saved successfully!", Toast.LENGTH_SHORT).show()
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
        title = "Merge PDFs",
        desc = "Combine multiple PDF files into one clean document in the order you choose.",
        badgeLabel = "MERGE",
        onBack = onBack,
        badgeColor = MintColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("application/pdf")) },
                    prompt = "Combine multiple PDFs into one.",
                    badgeColor = MintColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select two or more PDF files from your device",
                    step2 = "Arrange documents into desired sequence",
                    step3 = "Merge and download unified document directly"
                )
            }

            2 -> {
                // State 2: Selected Files & Queue
                Text(
                    text = "Sequence Order (${selectedPdfUris.size} PDFs)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedPdfUris.forEachIndexed { index, uri ->
                        val pages = pdfPageCounts[uri]
                        val pageStr = if (pages != null && pages > 0) "$pages pages" else "Reading pages..."
                        val sizeStr = FileUtils.getFileSizeString(context, uri)
                        val metaStr = if (sizeStr != null) "$pageStr • $sizeStr" else pageStr

                        MultiFileQueueCard(
                            index = index,
                            fileName = FileUtils.getFileName(context, uri),
                            fileSizeStr = metaStr,
                            canMoveUp = index > 0,
                            canMoveDown = index < selectedPdfUris.size - 1,
                            onMoveUp = if (index > 0) {
                                {
                                    val cur = selectedPdfUris[index]
                                    selectedPdfUris.removeAt(index)
                                    selectedPdfUris.add(index - 1, cur)
                                }
                            } else null,
                            onMoveDown = if (index < selectedPdfUris.size - 1) {
                                {
                                    val cur = selectedPdfUris[index]
                                    selectedPdfUris.removeAt(index)
                                    selectedPdfUris.add(index + 1, cur)
                                }
                            } else null,
                            onRemove = {
                                selectedPdfUris.removeAt(index)
                                if (selectedPdfUris.isEmpty()) {
                                    stage = 1
                                }
                            }
                        )
                    }
                }

                BrutalistButton(
                    text = "+ Add More PDFs",
                    onClick = { filePicker.launch(arrayOf("application/pdf")) },
                    backgroundColor = WhiteColor,
                    modifier = Modifier.fillMaxWidth()
                )

                BrutalistButton(
                    text = "Merge PDFs Now →",
                    onClick = {
                        if (selectedPdfUris.size < 2) {
                            Toast.makeText(context, "Please select at least 2 PDFs to merge.", Toast.LENGTH_SHORT).show()
                            return@BrutalistButton
                        }

                        stage = 3
                        progressVal = 0.2f
                        progressLabel = "Starting merge..."

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("merged_pdf_", ".pdf", context.cacheDir)
                                tempPdfFile = tempFile

                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.mergePdfs(
                                        context = context,
                                        pdfUris = selectedPdfUris.toList(),
                                        outputStream = fos
                                    ) { current, total ->
                                        progressVal = current.toFloat() / total
                                        progressLabel = "Merging PDF $current of $total... (${(progressVal * 100).toInt()}%)"
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Merge complete!", Toast.LENGTH_SHORT).show()
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                tempPdfFile?.delete()
                                tempPdfFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Merge failed: ${e.message ?: "Invalid or protected PDF"}"
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = MintColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = progressLabel.ifEmpty { "Merging PDF files..." },
                    stepDetails = "Appending document streams locally with zero cloud transfer"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "🎉 PDFs Merged Successfully!",
                    subtitle = "All ${selectedPdfUris.size} documents have been combined into a unified PDF.",
                    primaryButtonText = "Download Combined PDF 📁",
                    onPrimaryClick = { pdfSaver.launch("merged_document.pdf") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUris.clear()
                        pdfPageCounts.clear()
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
                    errorMessage = errorMessage.ifEmpty { "Failed to merge selected PDF files." },
                    onDismissOrRetry = {
                        if (selectedPdfUris.isNotEmpty()) {
                            stage = 2
                        } else {
                            stage = 1
                        }
                    },
                    retryButtonText = "Review PDF Queue"
                )
            }
        }
    }
}
