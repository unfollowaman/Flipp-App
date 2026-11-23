package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SplitPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("document.pdf") }
    var totalPages by rememberSaveable { mutableIntStateOf(0) }

    // Mode: "RANGE" or "EVERY_N"
    var splitMode by rememberSaveable { mutableStateOf("RANGE") }
    var startPageStr by rememberSaveable { mutableStateOf("1") }
    var endPageStr by rememberSaveable { mutableStateOf("1") }
    var everyNStr by rememberSaveable { mutableStateOf("1") }

    var startThumb by remember { mutableStateOf<Bitmap?>(null) }
    var endThumb by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempOutputFile by remember { mutableStateOf<File?>(null) }
    var isOutputZip by rememberSaveable { mutableStateOf(false) }

    fun inspectUri(uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    pdfFileName = cursor.getString(nameIndex) ?: "document.pdf"
                }
            }
        } catch (_: Exception) {
            pdfFileName = uri.lastPathSegment ?: "document.pdf"
        }
    }

    fun loadPreviewThumbnails(uri: Uri, startIdx: Int, endIdx: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val thumbs = loadPdfThumbnails(context, uri, maxPages = totalPages.coerceAtLeast(1))
                withContext(Dispatchers.Main) {
                    startThumb = if (startIdx in 1..thumbs.size) thumbs[startIdx - 1] else thumbs.firstOrNull()
                    endThumb = if (endIdx in 1..thumbs.size) thumbs[endIdx - 1] else thumbs.lastOrNull()
                }
            } catch (_: Exception) {}
        }
    }

    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(if (isOutputZip) "application/zip" else "application/pdf"),
        onResult = { uri ->
            if (uri != null && tempOutputFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempOutputFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // App Bar (TopNavbar title: "Split PDF")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(WhiteColor)
                .border(2.dp, BlackColor)
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(WhiteColor, RoundedCornerShape(8.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = BlackColor
                )
            }
            Text(
                text = "Split PDF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "SPLIT", backgroundColor = RedColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stage 1: File Selection
            if (stage == 1) {
                // Header card with title "Split PDF" and exact description for accessibility test
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = RedColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Split PDF",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Extract specific page range into a separate file.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Drop your PDF here or browse files",
                    badgeColor = RedColor,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        inspectUri(uri)
                        coroutineScope.launch {
                            val pages = withContext(Dispatchers.IO) {
                                PdfUtils.getPdfPageCount(context, uri)
                            }
                            if (pages <= 0) {
                                Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                            } else {
                                totalPages = pages
                                startPageStr = "1"
                                endPageStr = minOf(2, pages).toString()
                                loadPreviewThumbnails(uri, 1, minOf(2, pages))
                                stage = 2
                            }
                        }
                    }
                )

                // 3-step Guide Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CreamColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How to split a PDF file",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload your PDF", "Select a PDF file from your device. Everything processes offline.", RedColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Select the split mode", "Choose to extract specific pages manually, or split every N pages.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Split & download", "Generate your targeted PDF or ZIP archive instantly.", MintColor)
                    }
                }
            }

            // Stage 2: Configuration & Preview
            if (stage == 2) {
                Text(
                    text = "Selected: $pdfFileName ($totalPages pages)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )

                BrutalistButton(
                    text = "Split Pages →",
                    onClick = {
                        isProcessing = true
                        progressVal = 0.1f
                        progressLabel = "Starting PDF split..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                if (splitMode == "RANGE") {
                                    isOutputZip = false
                                    val start = (startPageStr.toIntOrNull() ?: 1).coerceIn(1, totalPages)
                                    val end = (endPageStr.toIntOrNull() ?: totalPages).coerceIn(start, totalPages)
                                    val tempPdf = File.createTempFile("split_", ".pdf", context.cacheDir)
                                    tempOutputFile = tempPdf
                                    FileOutputStream(tempPdf).use { fos ->
                                        PdfUtils.splitPdf(context, selectedPdfUri!!, start, end, fos)
                                    }
                                } else {
                                    isOutputZip = true
                                    val n = (everyNStr.toIntOrNull() ?: 1).coerceAtLeast(1)
                                    val tempZip = File.createTempFile("split_bundle_", ".zip", context.cacheDir)
                                    tempOutputFile = tempZip
                                    FileOutputStream(tempZip).use { fos ->
                                        PdfUtils.splitPdfEveryNPages(context, selectedPdfUri!!, n, fos) { cur, tot ->
                                            progressVal = cur.toFloat() / tot
                                            progressLabel = "Splitting part $cur of $tot..."
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Split failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = PinkColor,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isProcessing) {
                    StageProgressBar(progress = progressVal, label = progressLabel)
                }

                // Split Mode Pill Selector
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "SPLIT MODE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        NeobrutalistPillSelector(
                            options = listOf("RANGE", "EVERY_N"),
                            selectedOption = splitMode,
                            onOptionSelected = { splitMode = it },
                            labelProvider = { if (it == "RANGE") "By Page Range" else "Split Every N Pages" }
                        )

                        if (splitMode == "RANGE") {
                            Text(
                                text = "SPLIT RANGE (FIRST OUTPUT)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BlackColor,
                                letterSpacing = 0.05.em
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = startPageStr,
                                    onValueChange = {
                                        startPageStr = it.filter { c -> c.isDigit() }
                                        val s = startPageStr.toIntOrNull() ?: 1
                                        val e = endPageStr.toIntOrNull() ?: s
                                        loadPreviewThumbnails(selectedPdfUri!!, s, e)
                                    },
                                    label = { Text("Start") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { contentDescription = "Start page" },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BlackColor,
                                        unfocusedBorderColor = BlackColor
                                    )
                                )
                                Text("to", fontWeight = FontWeight.Bold, color = BlackColor)
                                OutlinedTextField(
                                    value = endPageStr,
                                    onValueChange = {
                                        endPageStr = it.filter { c -> c.isDigit() }
                                        val s = startPageStr.toIntOrNull() ?: 1
                                        val e = endPageStr.toIntOrNull() ?: s
                                        loadPreviewThumbnails(selectedPdfUri!!, s, e)
                                    },
                                    label = { Text("End") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { contentDescription = "End page" },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BlackColor,
                                        unfocusedBorderColor = BlackColor
                                    )
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Split after every", fontWeight = FontWeight.Bold, color = BlackColor)
                                IconButton(
                                    onClick = {
                                        val curr = everyNStr.toIntOrNull() ?: 1
                                        if (curr > 1) everyNStr = (curr - 1).toString()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(WhiteColor, RoundedCornerShape(6.dp))
                                        .border(2.dp, BlackColor, RoundedCornerShape(6.dp))
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(36.dp)
                                        .border(2.dp, BlackColor, RoundedCornerShape(6.dp))
                                        .background(WhiteColor, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(everyNStr, fontWeight = FontWeight.Black)
                                }
                                IconButton(
                                    onClick = {
                                        val curr = everyNStr.toIntOrNull() ?: 1
                                        if (curr < totalPages) everyNStr = (curr + 1).toString()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(WhiteColor, RoundedCornerShape(6.dp))
                                        .border(2.dp, BlackColor, RoundedCornerShape(6.dp))
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                                Text("pages", fontWeight = FontWeight.Bold, color = BlackColor)
                            }
                        }
                    }
                }

                // Range Previews
                if (splitMode == "RANGE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Page Card
                        BrutalistShadowBox(
                            modifier = Modifier.weight(1f),
                            backgroundColor = WhiteColor,
                            cornerRadius = 12.dp
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(BackgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (startThumb != null) {
                                        Image(
                                            bitmap = startThumb!!.asImageBitmap(),
                                            contentDescription = "Start Page Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text("Page $startPageStr", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BlackColor)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Page $startPageStr",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = WhiteColor
                                    )
                                }
                            }
                        }

                        // End Page Card
                        BrutalistShadowBox(
                            modifier = Modifier.weight(1f),
                            backgroundColor = WhiteColor,
                            cornerRadius = 12.dp
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(BackgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (endThumb != null) {
                                        Image(
                                            bitmap = endThumb!!.asImageBitmap(),
                                            contentDescription = "End Page Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text("Page $endPageStr", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BlackColor)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Page $endPageStr",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = WhiteColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ PDF Split Complete!",
                    subtitle = if (isOutputZip) "All pages divided into separate documents in a ZIP file." else "Extracted page range ready for saving.",
                    primaryButtonText = if (isOutputZip) "Save Split ZIP 📦" else "Save Split PDF 📄",
                    onPrimaryClick = {
                        val name = if (isOutputZip) "split_documents.zip" else "split_pages_${startPageStr}-${endPageStr}.pdf"
                        fileSaver.launch(name)
                    },
                    secondaryButtonText = "Split another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        startThumb = null
                        endThumb = null
                        tempOutputFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
