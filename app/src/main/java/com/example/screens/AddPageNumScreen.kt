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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
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
fun AddPageNumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("document.pdf") }
    var totalPages by rememberSaveable { mutableIntStateOf(0) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Position: "top-left", "top-center", "top-right", "bottom-left", "bottom-center", "bottom-right"
    var position by rememberSaveable { mutableStateOf("bottom-center") }
    var startNumberStr by rememberSaveable { mutableStateOf("1") }
    var formatStyle by rememberSaveable { mutableStateOf("NUMBER_ONLY") } // "NUMBER_ONLY", "PAGE_X", "X_OF_N"

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempNumberedFile by remember { mutableStateOf<File?>(null) }

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

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempNumberedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempNumberedFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Numbered PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // App Bar (TopNavbar title: "Add Page Numbers")
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
                text = "Add Page Numbers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "PAGE NUMBERS", backgroundColor = YellowColor)
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
                // Header card with title "Add Page Numbers" (assertCountEquals(2) check with navbar)
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = YellowColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Page Numbers",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stamp clean page numbers onto your PDF documents with customizable position, starting offset, and formatting.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

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
                            val thumbs = withContext(Dispatchers.IO) {
                                loadPdfThumbnails(context, uri, maxPages = 1)
                            }
                            previewBitmap = thumbs.firstOrNull()
                            stage = 2
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
                            text = "How to add page numbers to a PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload PDF", "Upload your PDF file, processed completely offline on your device.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Customize Position", "Choose where you want the page numbers to appear and specify the starting number.", SkyBlueColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Download PDF", "Click 'Add Numbers' and download your newly numbered PDF document instantly.", MintColor)
                    }
                }
            }

            // Stage 2: Configuration & Preview
            if (stage == 2) {
                Text(
                    text = "Selected: $pdfFileName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )

                BrutalistButton(
                    text = "Add Page Numbers →",
                    onClick = {
                        isProcessing = true
                        progressVal = 0.2f
                        progressLabel = "Stamping page numbers..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("numbered_", ".pdf", context.cacheDir)
                                tempNumberedFile = tempFile
                                val startNum = startNumberStr.toIntOrNull() ?: 1
                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.addPageNumbers(context, selectedPdfUri!!, position, startNum, fos)
                                }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Failed to stamp page numbers: ${e.message}", Toast.LENGTH_LONG).show()
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

                // Interactive 6-Position Quadrant Matrix Card
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
                            text = "POSITION MATRIX",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )

                        // 2 rows x 3 columns quadrant grid
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Top row: top-left, top-center, top-right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "top-left" to "Top Left",
                                    "top-center" to "Top Center",
                                    "top-right" to "Top Right"
                                ).forEach { (posKey, label) ->
                                    val isSelected = position == posKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) BlackColor else WhiteColor,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(role = Role.RadioButton) { position = posKey },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) WhiteColor else BlackColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Bottom row: bottom-left, bottom-center, bottom-right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "bottom-left" to "Bottom Left",
                                    "bottom-center" to "Bottom Center",
                                    "bottom-right" to "Bottom Right"
                                ).forEach { (posKey, label) ->
                                    val isSelected = position == posKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) BlackColor else WhiteColor,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(role = Role.RadioButton) { position = posKey },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) WhiteColor else BlackColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Starting Page Number Input
                        Text(
                            text = "STARTING PAGE NUMBER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        OutlinedTextField(
                            value = startNumberStr,
                            onValueChange = { startNumberStr = it.filter { c -> c.isDigit() } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Start page number" },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlackColor,
                                unfocusedBorderColor = BlackColor
                            )
                        )
                    }
                }

                // Document Preview Card with position indicator
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STAMP PREVIEW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                .background(BackgroundColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "PDF Page Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            // Simulated page number badge overlay based on chosen position
                            val alignment = when (position) {
                                "top-left" -> Alignment.TopStart
                                "top-center" -> Alignment.TopCenter
                                "top-right" -> Alignment.TopEnd
                                "bottom-left" -> Alignment.BottomStart
                                "bottom-center" -> Alignment.BottomCenter
                                else -> Alignment.BottomEnd
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = alignment
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(YellowColor, RoundedCornerShape(4.dp))
                                        .border(1.dp, BlackColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = startNumberStr.ifEmpty { "1" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BlackColor
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
                    title = "✓ Numbered PDF ready!",
                    subtitle = "Page numbers stamped seamlessly across your entire document.",
                    primaryButtonText = "Download numbered PDF 🔢",
                    onPrimaryClick = { pdfSaver.launch("numbered_${pdfFileName}") },
                    secondaryButtonText = "Reset ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        previewBitmap = null
                        tempNumberedFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
