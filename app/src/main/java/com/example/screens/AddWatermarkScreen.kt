package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
fun AddWatermarkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("document.pdf") }
    var isPdf by rememberSaveable { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Config options
    var watermarkType by rememberSaveable { mutableStateOf("TEXT") } // "TEXT", "IMAGE"
    var watermarkText by rememberSaveable { mutableStateOf("CONFIDENTIAL") }
    var watermarkImageUri by remember { mutableStateOf<Uri?>(null) }
    var watermarkImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var fontSize by rememberSaveable { mutableFloatStateOf(48f) } // 20 to 80
    var opacity by rememberSaveable { mutableFloatStateOf(0.5f) } // 0.1 to 1.0
    var rotationAngle by rememberSaveable { mutableFloatStateOf(45f) } // 0 to 90

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempResultFile by remember { mutableStateOf<File?>(null) }

    fun inspectUri(uri: Uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: "document.pdf"
                }
            }
        } catch (_: Exception) {
            fileName = uri.lastPathSegment ?: "document.pdf"
        }
        isPdf = fileName.endsWith(".pdf", ignoreCase = true)
    }

    val watermarkImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                watermarkImageUri = uri
                coroutineScope.launch(Dispatchers.IO) {
                    val bmp = loadImageThumbnail(context, uri)
                    withContext(Dispatchers.Main) {
                        watermarkImageBitmap = bmp
                    }
                }
            }
        }
    )

    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(if (isPdf) "application/pdf" else "image/jpeg"),
        onResult = { uri ->
            if (uri != null && tempResultFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempResultFile!!.inputStream().copyTo(out)
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
                text = "Add Watermark",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "WATERMARK", backgroundColor = SkyBlueColor)
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
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SkyBlueColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Watermark",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stamp security text or logo watermarks across PDF documents and images with full control over opacity, angle, and size.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                SingleFileDropZone(
                    mimeTypes = arrayOf("application/pdf", "image/*"),
                    prompt = "Drop your PDF or image here or browse files",
                    badgeColor = SkyBlueColor,
                    onFileSelected = { uri ->
                        selectedUri = uri
                        inspectUri(uri)
                        coroutineScope.launch {
                            val thumb = withContext(Dispatchers.IO) {
                                if (isPdf) {
                                    loadPdfThumbnails(context, uri, maxPages = 1).firstOrNull()
                                } else {
                                    loadImageThumbnail(context, uri)
                                }
                            }
                            previewBitmap = thumb
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
                            text = "How to add a watermark to a PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload your file", "Drag and drop your PDF or image file. The tool runs completely locally.", SkyBlueColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Customize the watermark", "Choose between text or image watermarks. Adjust opacity, size, and rotation in live preview.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Download watermarked file", "Generate your protected document with permanent stamp overlay.", MintColor)
                    }
                }
            }

            // Stage 2: Configuration & Preview
            if (stage == 2) {
                // Mode Selector Pills
                NeobrutalistPillSelector(
                    options = listOf("TEXT", "IMAGE"),
                    selectedOption = watermarkType,
                    onOptionSelected = { watermarkType = it },
                    labelProvider = { if (it == "TEXT") "Text Watermark" else "Image Watermark" }
                )

                // Controls Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (watermarkType == "TEXT") {
                            Text(
                                text = "WATERMARK TEXT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BlackColor,
                                letterSpacing = 0.05.em
                            )
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Watermark text" },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor
                                )
                            )

                            Text(
                                text = "FONT SIZE (${fontSize.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BlackColor,
                                letterSpacing = 0.05.em
                            )
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 20f..80f,
                                modifier = Modifier.semantics { contentDescription = "Watermark font size slider" },
                                colors = SliderDefaults.colors(
                                    thumbColor = BlackColor,
                                    activeTrackColor = BlackColor
                                )
                            )
                        } else {
                            Text(
                                text = "WATERMARK IMAGE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BlackColor,
                                letterSpacing = 0.05.em
                            )
                            BrutalistButton(
                                text = if (watermarkImageUri != null) "Change Image" else "Select Logo Image",
                                onClick = { watermarkImagePicker.launch(arrayOf("image/*")) },
                                backgroundColor = WhiteColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Opacity Slider
                        Text(
                            text = "OPACITY (${(opacity * 100).toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.semantics { contentDescription = "Watermark opacity slider" },
                            colors = SliderDefaults.colors(
                                thumbColor = BlackColor,
                                activeTrackColor = BlackColor
                            )
                        )

                        // Rotation Slider
                        Text(
                            text = "ROTATION (${rotationAngle.toInt()}°)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        Slider(
                            value = rotationAngle,
                            onValueChange = { rotationAngle = it },
                            valueRange = 0f..90f,
                            modifier = Modifier.semantics { contentDescription = "Watermark rotation slider" },
                            colors = SliderDefaults.colors(
                                thumbColor = BlackColor,
                                activeTrackColor = BlackColor
                            )
                        )
                    }
                }

                // CTA Button
                BrutalistButton(
                    text = "Download Watermarked PDF",
                    onClick = {
                        isProcessing = true
                        progressVal = 0.2f
                        progressLabel = "Applying watermark..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val extension = if (isPdf) ".pdf" else ".jpg"
                                val tempFile = File.createTempFile("watermarked_", extension, context.cacheDir)
                                tempResultFile = tempFile
                                FileOutputStream(tempFile).use { fos ->
                                    if (isPdf) {
                                        PdfUtils.addWatermarkToPdf(
                                            context = context,
                                            pdfUri = selectedUri!!,
                                            watermarkText = watermarkText,
                                            opacity = opacity,
                                            rotation = rotationAngle,
                                            fontSize = fontSize,
                                            outputStream = fos
                                        )
                                    } else {
                                        ImageUtils.addWatermark(
                                            context = context,
                                            baseImageUri = selectedUri!!,
                                            watermarkType = watermarkType.lowercase(),
                                            watermarkText = watermarkText,
                                            watermarkImageUri = watermarkImageUri,
                                            position = "center",
                                            opacity = opacity,
                                            rotation = rotationAngle,
                                            size = fontSize / 48f,
                                            colorStr = "#000000",
                                            outputStream = fos
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Watermark failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    backgroundColor = MintColor,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isProcessing) {
                    StageProgressBar(progress = progressVal, label = progressLabel)
                }

                // Live Canvas Preview Card
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
                            text = "LIVE OVERLAY PREVIEW",
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
                                .height(240.dp)
                                .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                .background(BackgroundColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Document base preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            // Overlay
                            if (watermarkType == "TEXT") {
                                Text(
                                    text = watermarkText.ifEmpty { "WATERMARK" },
                                    fontSize = (fontSize / 2).sp,
                                    fontWeight = FontWeight.Black,
                                    color = BlackColor.copy(alpha = opacity),
                                    modifier = Modifier.rotate(-rotationAngle),
                                    textAlign = TextAlign.Center
                                )
                            } else if (watermarkImageBitmap != null) {
                                Image(
                                    bitmap = watermarkImageBitmap!!.asImageBitmap(),
                                    contentDescription = "Watermark Logo Overlay",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .rotate(-rotationAngle),
                                    alpha = opacity
                                )
                            }
                        }
                    }
                }
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ Watermark Applied Successfully!",
                    subtitle = "Your document has been stamped with the watermark overlay.",
                    primaryButtonText = if (isPdf) "Download Watermarked PDF 📄" else "Save Watermarked Image 🖼️",
                    onPrimaryClick = {
                        val name = if (isPdf) "watermarked_${fileName}" else "watermarked_${fileName}.jpg"
                        fileSaver.launch(name)
                    },
                    secondaryButtonText = "Watermark another file ↺",
                    onSecondaryClick = {
                        selectedUri = null
                        previewBitmap = null
                        tempResultFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
