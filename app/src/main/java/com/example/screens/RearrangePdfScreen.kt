package com.example

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import java.io.FileOutputStream

@Composable
fun RearrangePdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var pdfFileSize by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }
    val pageOrder = remember { mutableStateListOf<Int>() } // 1-based original page indices in new order

    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempReorderedFile by remember { mutableStateOf<File?>(null) }

    fun resetState() {
        thumbnails.forEach { it.recycle() }
        thumbnails.clear()
        pageOrder.clear()
        tempReorderedFile?.delete()
        tempReorderedFile = null
        selectedPdfUri = null
        pdfFileName = ""
        pdfFileSize = ""
        totalPages = 1
        progressVal = 0f
        stage = 1
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempReorderedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempReorderedFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Reordered PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    fun startReorder() {
        val uri = selectedPdfUri ?: return
        stage = 2
        progressVal = 0.3f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "reordered_document.pdf")
                FileOutputStream(tempFile).use { fos ->
                    PdfUtils.reorderPdfPages(
                        context = context,
                        pdfUri = uri,
                        newOrder = pageOrder.toList(),
                        outputStream = fos
                    )
                }

                withContext(Dispatchers.Main) {
                    progressVal = 1f
                    tempReorderedFile = tempFile
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Reordering failed: ${e.message}", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    ToolScreenTemplate(
        title = "Rearrange PDF",
        desc = "Reorder, sort, and organize pages in your PDF.",
        badgeLabel = "REORGANIZE",
        onBack = onBack,
        badgeColor = MintColor
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
                        text = "Visually reorder pages on your device with 100% offline privacy.",
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
                                    val thumbs = loadPdfThumbnails(context, uri, maxPages = 50)
                                    withContext(Dispatchers.Main) {
                                        thumbnails.clear()
                                        thumbnails.addAll(thumbs)
                                        pageOrder.clear()
                                        pageOrder.addAll(1..count)
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
                                text = "HOW IT WORKS 🔄",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Select PDF file", "Pick any multi-page PDF document.", MintColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Reorder thumbnails", "Use arrow controls to move pages earlier or later.", YellowColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Download reordered PDF", "Export with pages re-sequenced into your custom order.", SkyBlueColor)
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
                        accentColor = MintColor
                    )

                    if (progressVal > 0f && progressVal < 1f) {
                        StageProgressBar(progress = progressVal, label = "Rebuilding PDF with new page sequence...")
                    }

                    // Ordering controls bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page Order (${pageOrder.size} pages)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackColor
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BrutalistButton(
                                text = "Reverse",
                                onClick = {
                                    val reversed = pageOrder.reversed()
                                    pageOrder.clear()
                                    pageOrder.addAll(reversed)
                                },
                                backgroundColor = CreamColor
                            )
                            BrutalistButton(
                                text = "Reset",
                                onClick = {
                                    pageOrder.clear()
                                    pageOrder.addAll(1..totalPages)
                                },
                                backgroundColor = WhiteColor
                            )
                        }
                    }

                    // 2-Column Reorderable Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(pageOrder) { positionIndex, originalPageNum ->
                            val bitmapIndex = originalPageNum - 1
                            val bitmap = thumbnails.getOrNull(bitmapIndex)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f)
                                    .background(WhiteColor, RoundedCornerShape(10.dp))
                                    .border(2.dp, BlackColor, RoundedCornerShape(10.dp))
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Page $originalPageNum (now position ${positionIndex + 1})",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 28.dp, bottom = 44.dp, start = 8.dp, end = 8.dp)
                                    )
                                }

                                // Top bar with new position index
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .background(BlackColor)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${positionIndex + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MintColor
                                        )
                                        Text(
                                            text = "Orig: p.$originalPageNum",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WhiteColor
                                        )
                                    }
                                }

                                // Bottom reordering buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(CreamColor)
                                        .border(1.dp, BlackColor)
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (positionIndex > 0) {
                                                val item = pageOrder.removeAt(positionIndex)
                                                pageOrder.add(positionIndex - 1, item)
                                            }
                                        },
                                        enabled = positionIndex > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Move earlier",
                                            tint = if (positionIndex > 0) BlackColor else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = "${positionIndex + 1}/$totalPages",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlackColor
                                    )

                                    IconButton(
                                        onClick = {
                                            if (positionIndex < pageOrder.size - 1) {
                                                val item = pageOrder.removeAt(positionIndex)
                                                pageOrder.add(positionIndex + 1, item)
                                            }
                                        },
                                        enabled = positionIndex < pageOrder.size - 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Move later",
                                            tint = if (positionIndex < pageOrder.size - 1) BlackColor else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action CTA
                    BrutalistButton(
                        text = "Save Reordered PDF ➔",
                        onClick = { startReorder() },
                        backgroundColor = MintColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BrutalistButton(
                        text = "Select Different PDF ↺",
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
                        title = "✓ PDF reordered successfully!",
                        subtitle = "New sequence [${pageOrder.take(5).joinToString(", ")}${if (pageOrder.size > 5) "..." else ""}] applied to all $totalPages page(s).",
                        primaryCtaText = "Download Reordered PDF 📄",
                        onPrimaryCta = {
                            val defaultName = pdfFileName.substringBeforeLast(".") + "_reordered.pdf"
                            pdfSaver.launch(defaultName)
                        },
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
                                text = "ORDER MAPPING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "New sequence: ${pageOrder.joinToString(" ➔ ") { "#$it" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    BrutalistButton(
                        text = "Rearrange another file ↺",
                        onClick = { resetState() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
