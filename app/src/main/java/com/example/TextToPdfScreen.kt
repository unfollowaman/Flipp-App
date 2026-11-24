package com.example

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackColor
import com.example.ui.theme.FlippYellow
import com.example.ui.theme.MintColor
import com.example.ui.theme.PinkColor
import com.example.ui.theme.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun TextToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1: Idle/Input, 2: Document Settings, 3: Processing, 4: Completed, 5: Error
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var textInput by rememberSaveable { mutableStateOf("") }

    var pageSizeSelection by rememberSaveable { mutableStateOf("A4") }
    var marginOption by rememberSaveable { mutableStateOf("normal") } // small, normal, large
    var fontSizeStr by rememberSaveable { mutableStateOf("12") }
    var alignment by rememberSaveable { mutableStateOf("left") } // left, center, right, justified

    var progressVal by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tempPdfFile?.delete()
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
                            Toast.makeText(context, "Text to PDF saved successfully!", Toast.LENGTH_SHORT).show()
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
        title = "Text to PDF",
        desc = "Convert your text into a PDF document.",
        badgeLabel = "TXT → PDF",
        onBack = onBack,
        badgeColor = MintColor
    ) {
        when (stage) {
            1 -> {
                // State 1: Input / Idle
                Text(
                    text = "Enter your text:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type or paste text here...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                        .semantics { contentDescription = "Text content for PDF" },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${textInput.length} characters",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    BrutalistButton(
                        text = "Clear Text",
                        onClick = { textInput = "" },
                        backgroundColor = PinkColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FeatureHowItWorksCard(
                    step1 = "Type or paste your document text",
                    step2 = "Customize typography, page size, and margins",
                    step3 = "Generate and download print-ready PDF"
                )

                BrutalistButton(
                    text = "Next: Configure Options ➔",
                    onClick = {
                        if (textInput.isNotBlank()) {
                            stage = 2
                        } else {
                            Toast.makeText(context, "Please enter some text.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    backgroundColor = MintColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            2 -> {
                // State 2 & 3: File Selected & Settings
                SelectedFileCard(
                    fileName = "text_document.pdf",
                    fileSizeStr = "${textInput.length} characters",
                    fileTypeBadge = "TXT",
                    badgeColor = MintColor,
                    onChangeFile = { stage = 1 }
                )

                CollapsibleSettingsCard(title = "Document Formatting Options") {
                    NeobrutalistPillGroup(
                        title = "PAGE SIZE",
                        options = listOf("A4", "LETTER", "LEGAL"),
                        selectedOption = pageSizeSelection,
                        onOptionSelected = { pageSizeSelection = it },
                        labelProvider = { it },
                        selectedColor = FlippYellow
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NeobrutalistPillGroup(
                        title = "MARGINS",
                        options = listOf("small", "normal", "large"),
                        selectedOption = marginOption,
                        onOptionSelected = { marginOption = it },
                        labelProvider = {
                            when (it) {
                                "small" -> "Small (18pt)"
                                "normal" -> "Normal (36pt)"
                                "large" -> "Large (72pt)"
                                else -> it
                            }
                        },
                        selectedColor = FlippYellow
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NeobrutalistPillGroup(
                        title = "TEXT ALIGNMENT",
                        options = listOf("left", "center", "right", "justified"),
                        selectedOption = alignment,
                        onOptionSelected = { alignment = it },
                        labelProvider = { it.replaceFirstChar { c -> c.uppercase() } },
                        selectedColor = FlippYellow
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Font Size (pt):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = fontSizeStr,
                        onValueChange = { fontSizeStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlackColor,
                            unfocusedBorderColor = BlackColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Font size in points" },
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalistButton(
                        text = "← Edit Text",
                        onClick = { stage = 1 },
                        backgroundColor = WhiteColor,
                        modifier = Modifier.weight(1f)
                    )

                    BrutalistButton(
                        text = "Generate PDF →",
                        onClick = {
                            val size = fontSizeStr.toFloatOrNull()
                            if (size != null && size > 0) {
                                stage = 3
                                progressVal = 0.5f
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val marginFloat = when (marginOption) {
                                            "small" -> 18f
                                            "normal" -> 36f
                                            "large" -> 72f
                                            else -> 36f
                                        }

                                        val file = File.createTempFile("text_gen_", ".pdf", context.cacheDir)
                                        tempPdfFile = file

                                        FileOutputStream(file).use { fileOut ->
                                            PdfUtils.textToPdf(
                                                context = context,
                                                text = textInput,
                                                pageSizeOption = pageSizeSelection,
                                                fontSize = size,
                                                margin = marginFloat,
                                                alignment = alignment,
                                                outputStream = fileOut
                                            )
                                        }

                                        withContext(Dispatchers.Main) {
                                            progressVal = 1f
                                            stage = 4
                                        }
                                    } catch (e: Exception) {
                                        tempPdfFile?.delete()
                                        tempPdfFile = null
                                        withContext(Dispatchers.Main) {
                                            errorMessage = "Error: ${e.message}"
                                            stage = 5
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter a valid font size.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        backgroundColor = MintColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            3 -> {
                // State 4: Processing
                ProcessingStageCard(
                    progress = progressVal,
                    statusText = "Generating PDF...",
                    stepDetails = "Formatting typography, calculating page breaks, and rendering PDF locally"
                )
            }

            4 -> {
                // State 5: Completed
                CompletedHeroCard(
                    title = "🎉 PDF Generated Successfully!",
                    subtitle = "Your text has been converted into a formatted PDF document.",
                    primaryButtonText = "Download Combined PDF 📁",
                    onPrimaryClick = { pdfSaver.launch("text_generated.pdf") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        tempPdfFile?.delete()
                        textInput = ""
                        tempPdfFile = null
                        stage = 1
                    },
                    accentColor = MintColor
                )
            }

            5 -> {
                // Error State
                ErrorCard(
                    errorMessage = errorMessage.ifEmpty { "Failed to generate PDF from text input." },
                    onDismissOrRetry = { stage = 2 },
                    retryButtonText = "Review Options"
                )
            }
        }
    }
}
