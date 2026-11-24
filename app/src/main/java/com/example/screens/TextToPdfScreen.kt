package com.example

import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun TextToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var textInput by rememberSaveable { mutableStateOf("") }
    var pageSizeSelection by rememberSaveable { mutableStateOf("A4") } // "A4", "LETTER"
    var marginOption by rememberSaveable { mutableStateOf("normal") } // "small", "normal", "large"
    var fontSizeStr by rememberSaveable { mutableStateOf("12") }
    var alignment by rememberSaveable { mutableStateOf("left") } // "left", "center", "right", "justified"

    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    fun resetState() {
        tempPdfFile?.delete()
        tempPdfFile = null
        textInput = ""
        stage = 1
    }

    val txtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        } ?: ""
                        withContext(Dispatchers.Main) {
                            textInput = content
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Could not read text file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
                            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    fun generatePdf() {
        val size = fontSizeStr.toFloatOrNull()
        if (size == null || size <= 0f) {
            Toast.makeText(context, "Please enter a valid font size.", Toast.LENGTH_SHORT).show()
            return
        }

        stage = 3
        progressVal = 0.4f
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val marginFloat = when (marginOption) {
                    "small" -> 18f
                    "normal" -> 36f
                    "large" -> 72f
                    else -> 36f
                }

                val cacheDir = context.cacheDir
                val file = File(cacheDir, "text_generated.pdf")
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
                    tempPdfFile = file
                    stage = 4
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    ToolScreenTemplate(
        title = "Text to PDF",
        desc = "Convert your text into a PDF document.",
        badgeLabel = "TXT → PDF",
        onBack = onBack,
        badgeColor = MintColor
    ) {
        when (stage) {
            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enter your text:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor
                        )

                        BrutalistButton(
                            text = "📂 Open File",
                            onClick = {
                                txtPicker.launch(arrayOf("text/plain", "text/markdown", "*/*"))
                            },
                            backgroundColor = CreamColor
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type or paste text here...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 260.dp)
                            .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                            .semantics { contentDescription = "Text content for PDF" },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = BlackColor,
                            lineHeight = 20.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = WhiteColor,
                            unfocusedContainerColor = WhiteColor,
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
                            text = "${textInput.length} characters • ${textInput.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF666666)
                        )

                        if (textInput.isNotEmpty()) {
                            BrutalistButton(
                                text = "Clear",
                                onClick = { textInput = "" },
                                backgroundColor = PinkColor
                            )
                        }
                    }

                    BrutalistButton(
                        text = "Next: Formatting Options ➔",
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // How it works guide
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "HOW IT WORKS 📝",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Type or paste", "Enter text directly or import a .txt or .md file.", PinkColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Configure style", "Choose page size, margins, font scale, and alignment.", SkyBlueColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Download PDF", "Generates a clean vector PDF formatted for print or sharing.", MintColor)
                        }
                    }
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document Summary
                    DocumentSummaryHeader(
                        fileName = "text_document.txt",
                        pageCount = 1,
                        fileSize = "${textInput.length} characters",
                        accentColor = MintColor
                    )

                    // Page size pills
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Page Size",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("A4", "LETTER"),
                                selectedOption = pageSizeSelection,
                                onOptionSelected = { pageSizeSelection = it },
                                labelProvider = { if (it == "A4") "A4 (Standard)" else "Letter (US)" },
                                sublabelProvider = { if (it == "A4") "210 × 297 mm" else "8.5 × 11 in" },
                                activeColor = MintColor
                            )
                        }
                    }

                    // Margins pills
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Margins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("small", "normal", "large"),
                                selectedOption = marginOption,
                                onOptionSelected = { marginOption = it },
                                labelProvider = {
                                    when (it) {
                                        "small" -> "Narrow"
                                        "normal" -> "Normal"
                                        "large" -> "Wide"
                                        else -> it
                                    }
                                },
                                sublabelProvider = {
                                    when (it) {
                                        "small" -> "0.25 in (18pt)"
                                        "normal" -> "0.5 in (36pt)"
                                        "large" -> "1.0 in (72pt)"
                                        else -> ""
                                    }
                                },
                                activeColor = MintColor
                            )
                        }
                    }

                    // Alignment pills
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Text Alignment",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("left", "center", "right", "justified"),
                                selectedOption = alignment,
                                onOptionSelected = { alignment = it },
                                labelProvider = { it.replaceFirstChar { char -> char.uppercase() } },
                                activeColor = MintColor
                            )
                        }
                    }

                    // Font Size Options
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Font Size",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("10", "12", "14", "16"),
                                selectedOption = fontSizeStr,
                                onOptionSelected = { fontSizeStr = it },
                                labelProvider = { "${it} pt" },
                                sublabelProvider = {
                                    when (it) {
                                        "10" -> "Compact"
                                        "12" -> "Standard"
                                        "14" -> "Large"
                                        "16" -> "Heading"
                                        else -> ""
                                    }
                                },
                                activeColor = YellowColor
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Custom Font Size (pt):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = fontSizeStr,
                                onValueChange = { if (it.all { char -> char.isDigit() }) fontSizeStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                    .semantics { contentDescription = "Font size in points" },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrutalistButton(
                            text = "Back",
                            onClick = { stage = 1 },
                            backgroundColor = YellowColor,
                            modifier = Modifier.weight(1f)
                        )
                        BrutalistButton(
                            text = "Generate PDF ➔",
                            onClick = { generatePdf() },
                            backgroundColor = MintColor,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }

            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StageProgressBar(progress = progressVal, label = "Compiling vector PDF document...")
                }
            }

            4 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ResultsHeroBanner(
                        title = "🎉 PDF Generated Successfully!",
                        subtitle = "Your formatted PDF document is compiled and ready to save.",
                        primaryCtaText = "Download PDF 📁",
                        onPrimaryCta = { pdfSaver.launch("text_document.pdf") },
                        onReset = { resetState() },
                        accentColor = MintColor
                    )

                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "DOCUMENT SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Page Size: $pageSizeSelection • Margin: $marginOption",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            Text(
                                text = "Font: ${fontSizeStr}pt • Alignment: ${alignment.replaceFirstChar { it.uppercase() }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            Text(
                                text = "Length: ${textInput.length} characters",
                                fontSize = 12.sp,
                                color = Color(0xFF555555)
                            )
                        }
                    }

                    BrutalistButton(
                        text = "Create Another ↺",
                        onClick = { resetState() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
