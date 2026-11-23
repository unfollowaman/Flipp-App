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
fun MergePdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val pdfThumbnails = remember { mutableStateMapOf<Uri, Bitmap?>() }
    val pdfNames = remember { mutableStateMapOf<Uri, String>() }
    val pdfPageCounts = remember { mutableStateMapOf<Uri, Int>() }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempMergedFile by remember { mutableStateOf<File?>(null) }

    fun queryName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment ?: "document.pdf"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "document.pdf"
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    if (!selectedUris.contains(uri)) {
                        selectedUris.add(uri)
                        pdfNames[uri] = queryName(uri)
                        coroutineScope.launch(Dispatchers.IO) {
                            val pages = PdfUtils.getPdfPageCount(context, uri)
                            val thumbs = loadPdfThumbnails(context, uri, maxPages = 1)
                            withContext(Dispatchers.Main) {
                                pdfPageCounts[uri] = pages
                                pdfThumbnails[uri] = thumbs.firstOrNull()
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
            if (uri != null && tempMergedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempMergedFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Merged PDF saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving merged PDF: ${e.message}", Toast.LENGTH_LONG).show()
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
                text = "Merge PDF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "MERGE", backgroundColor = PinkColor)
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
                    backgroundColor = PinkColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Merge PDF",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Combine multiple PDF documents into a single organized file in any page order you choose.",
                            fontSize = 13.sp,
                            color = Color(0xFF222222),
                            lineHeight = 18.sp
                        )
                    }
                }

                // DropZone
                DropZone(
                    onBrowseClick = { pdfPickerLauncher.launch("application/pdf") },
                    prompt = "Drop your PDF files here or browse files",
                    badgeColor = PinkColor
                )

                // 3-step Guide Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = CreamColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How to merge PDF files online",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload your files", "Select two or more PDF files from your device.", PinkColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Reorder documents", "Use the order controls to arrange PDFs in your preferred sequence.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Merge & download", "Combine files into one unified document with zero cloud upload.", MintColor)
                    }
                }
            }

            // Stage 2: Loaded Queue & Controls
            if (stage == 2) {
                Text(
                    text = "${selectedUris.size} files selected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrutalistButton(
                        text = "+ Add files",
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        backgroundColor = WhiteColor,
                        modifier = Modifier.weight(1f)
                    )
                    BrutalistButton(
                        text = "Merge PDFs →",
                        onClick = {
                            if (selectedUris.size < 2) {
                                Toast.makeText(context, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                                return@BrutalistButton
                            }
                            isProcessing = true
                            progressVal = 0.05f
                            progressLabel = "Starting merge..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("merged_", ".pdf", context.cacheDir)
                                    tempMergedFile = tempFile
                                    FileOutputStream(tempFile).use { fos ->
                                        PdfUtils.mergePdfs(
                                            context = context,
                                            pdfUris = selectedUris,
                                            outputStream = fos
                                        ) { current, total ->
                                            progressVal = current.toFloat() / total
                                            progressLabel = "Merging file $current of $total..."
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        stage = 3
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        Toast.makeText(context, "Merge failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                    text = "Drag cards to reorder PDFs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666)
                )

                // 2-column Grid of PDF cards
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
                        val thumb = pdfThumbnails[uri]
                        val name = pdfNames[uri] ?: "document.pdf"
                        val pageCount = pdfPageCounts[uri] ?: 1

                        BrutalistShadowBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = WhiteColor,
                            cornerRadius = 12.dp
                        ) {
                            Column {
                                // Top Header Bar with yellow index badge & close
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(YellowColor)
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
                                                    contentDescription = "Move PDF up",
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
                                                    contentDescription = "Move PDF down",
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
                                                contentDescription = "Remove PDF $name",
                                                tint = WhiteColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // PDF Page 1 Preview
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
                                            contentDescription = "Page 1 preview for $name",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text("PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                }

                                // Bottom Details Bar
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlackColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$pageCount ${if (pageCount == 1) "page" else "pages"}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }

                    // Add more files card at the end of the grid
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .border(2.dp, BlackColor, RoundedCornerShape(12.dp))
                                .background(WhiteColor, RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button, onClickLabel = "Add more files") {
                                    pdfPickerLauncher.launch("application/pdf")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(YellowColor, CircleShape)
                                        .border(2.dp, BlackColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add files icon",
                                        tint = BlackColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Add more files",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BlackColor
                                )
                                Text(
                                    text = "Add PDF files",
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ PDFs Merged Successfully!",
                    subtitle = "${selectedUris.size} documents unified into a single offline PDF.",
                    primaryButtonText = "Save Merged PDF 📄",
                    onPrimaryClick = { pdfSaver.launch("merged_document.pdf") },
                    secondaryButtonText = "Merge more files ↺",
                    onSecondaryClick = {
                        selectedUris.clear()
                        pdfThumbnails.clear()
                        pdfNames.clear()
                        pdfPageCounts.clear()
                        tempMergedFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
