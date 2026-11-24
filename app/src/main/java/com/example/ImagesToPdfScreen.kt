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
import com.example.ui.theme.PinkColor
import com.example.ui.theme.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImagesToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle, 2: Files Selected & Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    val selectedImageUris = remember { mutableStateListOf<Uri>() }

    var pageSizeSelection by rememberSaveable { mutableStateOf("AUTO") }
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
                selectedImageUris.addAll(uris)
                stage = 2
            } else if (selectedImageUris.isEmpty()) {
                Toast.makeText(context, "No files selected.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
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
        title = "Images to PDF",
        desc = "Combine one or more PNG or JPG images into a single PDF. Reorder pages by dragging before you convert.",
        badgeLabel = "IMG → PDF",
        onBack = onBack,
        badgeColor = PinkColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Idle
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("image/*")) },
                    prompt = "Combine multiple PNG/JPG into one PDF.",
                    badgeColor = PinkColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Select one or more photos or scans from your device",
                    step2 = "Reorder images and choose page size layout",
                    step3 = "Export combined PDF document directly to storage"
                )
            }

            2 -> {
                // State 2 & 3: Selected Files & Settings
                Text(
                    text = "Images to Combine (${selectedImageUris.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedImageUris.forEachIndexed { index, uri ->
                        MultiFileQueueCard(
                            index = index,
                            fileName = FileUtils.getFileName(context, uri),
                            fileSizeStr = FileUtils.getFileSizeString(context, uri),
                            canMoveUp = index > 0,
                            canMoveDown = index < selectedImageUris.size - 1,
                            onMoveUp = if (index > 0) {
                                {
                                    val current = selectedImageUris[index]
                                    selectedImageUris.removeAt(index)
                                    selectedImageUris.add(index - 1, current)
                                }
                            } else null,
                            onMoveDown = if (index < selectedImageUris.size - 1) {
                                {
                                    val current = selectedImageUris[index]
                                    selectedImageUris.removeAt(index)
                                    selectedImageUris.add(index + 1, current)
                                }
                            } else null,
                            onRemove = {
                                selectedImageUris.removeAt(index)
                                if (selectedImageUris.isEmpty()) {
                                    stage = 1
                                }
                            }
                        )
                    }
                }

                BrutalistButton(
                    text = "+ Add More Images",
                    onClick = { filePicker.launch(arrayOf("image/*")) },
                    backgroundColor = WhiteColor,
                    modifier = Modifier.fillMaxWidth()
                )

                CollapsibleSettingsCard(title = "Document Page Settings") {
                    NeobrutalistPillGroup(
                        title = "PAGE LAYOUT SIZE",
                        options = listOf("AUTO", "A4", "LETTER"),
                        selectedOption = pageSizeSelection,
                        onOptionSelected = { pageSizeSelection = it },
                        labelProvider = { it },
                        selectedColor = PinkColor
                    )
                }

                BrutalistButton(
                    text = "Convert All Pages →",
                    onClick = {
                        stage = 3
                        progressVal = 0.2f
                        progressLabel = "Packing images..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("combined_pdf_", ".pdf", context.cacheDir)
                                tempPdfFile = tempFile

                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.combineImagesToPdf(
                                        context = context,
                                        imageUris = selectedImageUris.toList(),
                                        pageSizeOption = pageSizeSelection,
                                        outputStream = fos
                                    ) { current, total ->
                                        progressVal = current.toFloat() / total
                                        progressLabel = "Adding image $current of $total... (${(progressVal * 100).toInt()}%)"
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Processing complete!", Toast.LENGTH_SHORT).show()
                                    stage = 4
                                }
                            } catch (e: Exception) {
                                tempPdfFile?.delete()
                                tempPdfFile = null
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Image combination failed: ${e.message ?: "Invalid image file"}"
                                    stage = 5
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = PinkColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = progressLabel.ifEmpty { "Packing images into PDF..." },
                    stepDetails = "Embedding images into PDF locally on your device"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "🎉 PDF Compiled!",
                    subtitle = "All ${selectedImageUris.size} images have been combined into a single PDF document.",
                    primaryButtonText = "Download Combined PDF 📁",
                    onPrimaryClick = { pdfSaver.launch("combined_document.pdf") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedImageUris.clear()
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
                    errorMessage = errorMessage.ifEmpty { "Failed to create PDF from selected images." },
                    onDismissOrRetry = {
                        if (selectedImageUris.isNotEmpty()) {
                            stage = 2
                        } else {
                            stage = 1
                        }
                    },
                    retryButtonText = "Try Again"
                )
            }
        }
    }
}
