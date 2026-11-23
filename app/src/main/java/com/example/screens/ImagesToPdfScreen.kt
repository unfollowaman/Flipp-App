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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
fun ImagesToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val imageThumbnails = remember { mutableStateMapOf<Uri, Bitmap?>() }
    val imageNames = remember { mutableStateMapOf<Uri, String>() }

    // Options
    var pageSizeOption by rememberSaveable { mutableStateOf("AUTO") } // "AUTO", "A4", "LETTER"
    var orientationOption by rememberSaveable { mutableStateOf("PORTRAIT") } // "PORTRAIT", "LANDSCAPE"
    var outputFilename by rememberSaveable { mutableStateOf("converted.pdf") }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    fun queryName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment ?: "image.png"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "image.png"
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    if (!selectedUris.contains(uri)) {
                        selectedUris.add(uri)
                        imageNames[uri] = queryName(uri)
                        coroutineScope.launch(Dispatchers.IO) {
                            val thumb = loadImageThumbnail(context, uri)
                            withContext(Dispatchers.Main) {
                                imageThumbnails[uri] = thumb
                            }
                        }
                    }
                }
                stage = 2
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
                            tempPdfFile!!.inputStream().copyTo(out)
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
                text = "Images to PDF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "IMG → PDF", backgroundColor = SkyBlueColor)
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
                            text = "Images to PDF",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Combine multiple JPG, PNG, or WEBP photos into a single clean PDF document completely on-device.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                // DropZone
                DropZone(
                    onBrowseClick = { imagePickerLauncher.launch("image/*") },
                    prompt = "Drop your images here or browse files",
                    badgeColor = SkyBlueColor
                )

                // 3-step Guide Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CreamColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How to convert images to PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload images", "Select one or more images from your gallery or file storage.", SkyBlueColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Arrange & configure", "Set page dimensions, orientation, and reorder pages.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Generate PDF", "Compile and save your final PDF document locally.", MintColor)
                    }
                }
            }

            // Stage 2: Configuration & Preview
            if (stage == 2) {
                // Options Card
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
                            text = "PAGE SIZE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        NeobrutalistPillSelector(
                            options = listOf("AUTO", "A4", "LETTER"),
                            selectedOption = pageSizeOption,
                            onOptionSelected = { pageSizeOption = it },
                            labelProvider = {
                                when (it) {
                                    "AUTO" -> "Auto (fit image)"
                                    "A4" -> "A4"
                                    else -> "US Letter"
                                }
                            }
                        )

                        Text(
                            text = "ORIENTATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        NeobrutalistPillSelector(
                            options = listOf("PORTRAIT", "LANDSCAPE"),
                            selectedOption = orientationOption,
                            onOptionSelected = { orientationOption = it },
                            labelProvider = { if (it == "PORTRAIT") "Portrait" else "Landscape" }
                        )

                        Text(
                            text = "OUTPUT FILENAME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = 0.05.em
                        )
                        OutlinedTextField(
                            value = outputFilename,
                            onValueChange = { outputFilename = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Output PDF filename" },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlackColor,
                                unfocusedBorderColor = BlackColor
                            )
                        )
                    }
                }

                // Subtitle + Action buttons
                Text(
                    text = "${selectedUris.size} images selected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrutalistButton(
                        text = "+ Add more images",
                        onClick = { imagePickerLauncher.launch("image/*") },
                        backgroundColor = YellowColor,
                        modifier = Modifier.weight(1f)
                    )
                    BrutalistButton(
                        text = "Build PDF →",
                        onClick = {
                            isProcessing = true
                            progressVal = 0.05f
                            progressLabel = "Compiling images to PDF..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("compiled_", ".pdf", context.cacheDir)
                                    tempPdfFile = tempFile
                                    FileOutputStream(tempFile).use { fos ->
                                        PdfUtils.combineImagesToPdf(
                                            context = context,
                                            imageUris = selectedUris,
                                            pageSizeOption = pageSizeOption,
                                            outputStream = fos,
                                            onProgress = { current, total ->
                                                progressVal = current.toFloat() / total
                                                progressLabel = "Processing image $current of $total..."
                                            },
                                            orientation = orientationOption
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        stage = 3
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        Toast.makeText(context, "Failed to create PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        backgroundColor = PinkColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isProcessing) {
                    StageProgressBar(progress = progressVal, label = progressLabel)
                }

                Text(
                    text = "Drag cards to reorder pages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666)
                )

                // 2-column Image Cards Grid with Order Arrows & Delete
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(selectedUris) { index, uri ->
                        val thumb = imageThumbnails[uri]
                        val name = imageNames[uri] ?: "image.png"
                        val cardBg = if (index % 2 == 0) YellowColor else SkyBlueColor

                        BrutalistShadowBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = WhiteColor,
                            cornerRadius = 12.dp
                        ) {
                            Column {
                                // Top Header Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BlackColor
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (index > 0) {
                                            IconButton(
                                                onClick = {
                                                    val temp = selectedUris[index]
                                                    selectedUris[index] = selectedUris[index - 1]
                                                    selectedUris[index - 1] = temp
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move image up",
                                                    tint = BlackColor
                                                )
                                            }
                                        }
                                        if (index < selectedUris.size - 1) {
                                            IconButton(
                                                onClick = {
                                                    val temp = selectedUris[index]
                                                    selectedUris[index] = selectedUris[index + 1]
                                                    selectedUris[index + 1] = temp
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move image down",
                                                    tint = BlackColor
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedUris.removeAt(index)
                                                if (selectedUris.isEmpty()) stage = 1
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(RedColor, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove image $name",
                                                tint = WhiteColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // Image Preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(BackgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = "Thumbnail for $name",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text("Loading...", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                // Filename bottom bar
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlackColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ PDF Created Successfully!",
                    subtitle = "${selectedUris.size} images compiled into a high-fidelity PDF.",
                    primaryButtonText = "Save PDF 📄",
                    onPrimaryClick = {
                        val safeName = if (outputFilename.endsWith(".pdf", ignoreCase = true)) outputFilename else "$outputFilename.pdf"
                        pdfSaver.launch(safeName)
                    },
                    secondaryButtonText = "Convert more images ↺",
                    onSecondaryClick = {
                        selectedUris.clear()
                        imageThumbnails.clear()
                        imageNames.clear()
                        tempPdfFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
