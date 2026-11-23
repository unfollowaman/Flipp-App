package com.example

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfToPngScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("document.pdf") }
    var pdfFileSize by remember { mutableStateOf("0 KB") }
    var totalPages by rememberSaveable { mutableIntStateOf(0) }

    // Options
    var formatOption by rememberSaveable { mutableStateOf("PNG") } // "PNG" or "JPG"
    var scaleOption by rememberSaveable { mutableIntStateOf(2) } // 1, 2, 3
    var pageScopeOption by rememberSaveable { mutableStateOf("ALL") } // "ALL" or "CUSTOM"
    var customFromPage by rememberSaveable { mutableStateOf("1") }
    var customToPage by rememberSaveable { mutableStateOf("1") }

    // Thumbnails loaded in Stage 2
    val pageThumbnails = remember { mutableStateListOf<Bitmap>() }

    // Results in Stage 3
    var conversionResults by remember { mutableStateOf<List<ConvertedPageResult>>(emptyList()) }
    var tempZipFile by remember { mutableStateOf<File?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }

    // Pending file for individual save
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }

    val zipSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null && tempZipFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempZipFile!!.inputStream().copyTo(out)
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

    val imageSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(if (formatOption == "JPG") "image/jpeg" else "image/png"),
        onResult = { uri ->
            if (uri != null && pendingSaveFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            pendingSaveFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    )

    fun shareImageFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (formatOption == "JPG") "image/jpeg" else "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to query filename and size
    fun inspectUri(uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) pdfFileName = cursor.getString(nameIndex) ?: "document.pdf"
                    if (sizeIndex != -1) {
                        val sizeBytes = cursor.getLong(sizeIndex)
                        pdfFileSize = when {
                            sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024f * 1024f))
                            sizeBytes >= 1024 -> "${sizeBytes / 1024} KB"
                            else -> "$sizeBytes B"
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // App Bar
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
                text = "PDF to Images",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "PDF → PNG", backgroundColor = YellowColor)
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
                // Hero card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = YellowColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PDF to PNG",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Convert every page of a PDF into high-resolution PNG or JPG images with 100% offline local privacy.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                // DropZone
                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf"),
                    prompt = "Drop your PDF here or browse files",
                    badgeColor = YellowColor,
                    onFileSelected = { uri ->
                        selectedPdfUri = uri
                        inspectUri(uri)
                        coroutineScope.launch {
                            val pages = withContext(Dispatchers.IO) {
                                PdfUtils.getPdfPageCount(context, uri)
                            }
                            totalPages = pages
                            customToPage = pages.coerceAtLeast(1).toString()
                            val thumbs = withContext(Dispatchers.IO) {
                                loadPdfThumbnails(context, uri, maxPages = 6)
                            }
                            if (thumbs.isEmpty()) {
                                Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                            } else {
                                pageThumbnails.clear()
                                pageThumbnails.addAll(thumbs)
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
                            text = "How to convert PDF to PNG",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload your PDF", "Select a PDF file from your device. Processing happens 100% locally.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Select options", "Choose between PNG or JPG and set resolution scale (1x, 2x, 3x).", SkyBlueColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Download images", "Save individual page images or download all as a ZIP archive.", MintColor)
                    }
                }
            }

            // Stage 2: Configuration & Preview
            if (stage == 2) {
                DocumentSummaryHeader(
                    filename = pdfFileName,
                    pageCount = totalPages,
                    fileSizeString = pdfFileSize,
                    onRemoveClick = {
                        selectedPdfUri = null
                        pageThumbnails.clear()
                        stage = 1
                    }
                )

                // Options card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Format Pills
                        Text(
                            text = "OUTPUT FORMAT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        NeobrutalistPillSelector(
                            options = listOf("PNG", "JPG"),
                            selectedOption = formatOption,
                            onOptionSelected = { formatOption = it },
                            labelProvider = { opt ->
                                if (opt == "PNG") "PNG — Best quality" else "JPG — Smaller size"
                            }
                        )

                        // Resolution Pills
                        Text(
                            text = "RESOLUTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        NeobrutalistPillSelector(
                            options = listOf(1, 2, 3),
                            selectedOption = scaleOption,
                            onOptionSelected = { scaleOption = it },
                            labelProvider = { "${it}x" }
                        )

                        // Page Range Pills
                        Text(
                            text = "PAGES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        NeobrutalistPillSelector(
                            options = listOf("ALL", "CUSTOM"),
                            selectedOption = pageScopeOption,
                            onOptionSelected = { pageScopeOption = it },
                            labelProvider = { if (it == "ALL") "All pages" else "Custom range" }
                        )

                        if (pageScopeOption == "CUSTOM") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customFromPage,
                                    onValueChange = { customFromPage = it.filter { c -> c.isDigit() } },
                                    label = { Text("From") },
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
                                    value = customToPage,
                                    onValueChange = { customToPage = it.filter { c -> c.isDigit() } },
                                    label = { Text("To") },
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
                        }
                    }
                }

                // CTA Button
                BrutalistButton(
                    text = "Convert All Pages →",
                    onClick = {
                        isProcessing = true
                        progressVal = 0.05f
                        progressLabel = "Starting image conversion..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val range = if (pageScopeOption == "CUSTOM") {
                                    val from = (customFromPage.toIntOrNull() ?: 1).coerceAtLeast(1) - 1
                                    val to = (customToPage.toIntOrNull() ?: totalPages).coerceAtMost(totalPages) - 1
                                    if (from <= to) from..to else 0 until totalPages
                                } else {
                                    0 until totalPages
                                }

                                val result = PdfUtils.convertPdfToImages(
                                    context = context,
                                    pdfUri = selectedPdfUri!!,
                                    format = formatOption,
                                    scale = scaleOption,
                                    pageRange = range
                                ) { current, total ->
                                    progressVal = current.toFloat() / total
                                    progressLabel = "Converting page $current of $total... (${(progressVal * 100).toInt()}%)"
                                }

                                withContext(Dispatchers.Main) {
                                    conversionResults = result.pages
                                    tempZipFile = result.zipFile
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Conversion failed: ${e.message}", Toast.LENGTH_LONG).show()
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

                // 2-column Page Previews Grid
                Text(
                    text = "Page Previews (${pageThumbnails.size} loaded)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )
                PageThumbnailGrid(
                    thumbnails = pageThumbnails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }

            // Stage 3: Results & Downloads
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ Conversion complete!",
                    subtitle = "All pages rendered locally to $formatOption format. Ready to save or share.",
                    primaryButtonText = "Download All as ZIP 📦",
                    onPrimaryClick = { zipSaver.launch("converted_${formatOption.lowercase()}_pages.zip") },
                    secondaryButtonText = "Convert another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        pageThumbnails.clear()
                        conversionResults = emptyList()
                        tempZipFile = null
                        stage = 1
                    }
                )

                Text(
                    text = "Generated Image Pages (${conversionResults.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Individual cards
                conversionResults.forEach { pageResult ->
                    IndividualOutputCard(
                        pageIndex = pageResult.pageIndex,
                        bitmap = pageResult.bitmap,
                        label = pageResult.filename,
                        onSaveClick = {
                            pendingSaveFile = pageResult.file
                            imageSaver.launch(pageResult.filename)
                        },
                        onShareClick = {
                            shareImageFile(pageResult.file)
                        }
                    )
                }
            }
        }
    }
}
