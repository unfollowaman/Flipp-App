package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
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

@Composable
fun PdfToTextScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var pdfFileSize by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }

    var extractMode by rememberSaveable { mutableStateOf("all") } // "all", "first"
    var extractedText by rememberSaveable { mutableStateOf("") }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("Extracting text from PDF...") }

    fun resetState() {
        thumbnails.forEach { it.recycle() }
        thumbnails.clear()
        selectedPdfUri = null
        pdfFileName = ""
        pdfFileSize = ""
        totalPages = 1
        extractedText = ""
        progressVal = 0f
        stage = 1
    }

    val txtSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(extractedText.toByteArray(Charsets.UTF_8))
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

    fun startExtraction() {
        val uri = selectedPdfUri ?: return
        stage = 2
        progressVal = 0.1f
        progressLabel = "Extracting text from PDF pages..."

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fullText = PdfUtils.extractTextFromPdf(context, uri) { current, total ->
                    progressVal = current.toFloat() / total.toFloat().coerceAtLeast(1f)
                    progressLabel = "Extracting page $current of $total..."
                }

                val finalOutput = if (extractMode == "first") {
                    val pages = fullText.split("--- Page ")
                    if (pages.size > 1) {
                        "--- Page " + pages[1]
                    } else fullText
                } else {
                    fullText
                }

                withContext(Dispatchers.Main) {
                    extractedText = finalOutput
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to extract text. Is it a valid PDF?", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    ToolScreenTemplate(
        title = "PDF to Text",
        desc = "Extract all text from any PDF document.",
        badgeLabel = "PDF → TXT",
        onBack = onBack,
        badgeColor = SkyBlueColor
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
                        text = "Extract text on-device with 100% offline privacy.",
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
                                            if (sizeIndex != -1) {
                                                val bytes = it.getLong(sizeIndex)
                                                pdfFileSize = String.format("%.2f MB", bytes / (1024f * 1024f))
                                            }
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
                                text = "HOW IT WORKS 📑",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Select PDF document", "Pick any text-based PDF file from your device.", SkyBlueColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Extract text locally", "All characters and paragraphs parsed in-memory.", MintColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Copy or export", "Copy text to clipboard or save directly to .TXT format.", YellowColor)
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
                        fileSize = pdfFileSize.ifEmpty { "PDF" },
                        accentColor = SkyBlueColor
                    )

                    if (progressVal > 0f && progressVal < 1f) {
                        StageProgressBar(progress = progressVal, label = progressLabel)
                    }

                    // Scope options
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WhiteColor,
                        cornerRadius = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Extraction Scope",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            NeobrutalistPillSelector(
                                options = listOf("all", "first"),
                                selectedOption = extractMode,
                                onOptionSelected = { extractMode = it },
                                labelProvider = { if (it == "all") "All Pages ($totalPages)" else "Page 1 Only" },
                                sublabelProvider = { if (it == "all") "Extract all text" else "Quick first page preview" },
                                activeColor = SkyBlueColor
                            )
                        }
                    }

                    // Preview thumbnail grid
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
                        text = "Extract Text ➔",
                        onClick = { startExtraction() },
                        backgroundColor = SkyBlueColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BrutalistButton(
                        text = "Change PDF ↺",
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
                    ResultsHeroBanner(
                        title = "✓ Text extracted successfully!",
                        subtitle = "${extractedText.length} characters extracted from $totalPages page(s).",
                        primaryCtaText = "Save as .TXT 💾",
                        onPrimaryCta = {
                            val defaultName = pdfFileName.substringBeforeLast(".") + "_text.txt"
                            txtSaver.launch(defaultName)
                        },
                        onReset = { resetState() },
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
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        // Monospaced text preview box
                        BrutalistShadowBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = CreamColor,
                            cornerRadius = 12.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DOCUMENT CONTENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "${extractedText.length} chars",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlackColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = extractedText,
                                    onValueChange = { extractedText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 180.dp, max = 320.dp)
                                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                        .semantics { contentDescription = "Extracted document text" },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = BlackColor,
                                        lineHeight = 18.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = WhiteColor,
                                        unfocusedContainerColor = WhiteColor,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        // High contrast action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrutalistButton(
                                text = "📋 Copy to Clipboard",
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
                                text = "💾 Save as .TXT",
                                onClick = {
                                    val defaultName = pdfFileName.substringBeforeLast(".") + "_text.txt"
                                    txtSaver.launch(defaultName)
                                },
                                backgroundColor = MintColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    BrutalistButton(
                        text = "Extract another file ↺",
                        onClick = { resetState() },
                        backgroundColor = SkyBlueColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
