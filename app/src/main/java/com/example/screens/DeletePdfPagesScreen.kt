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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
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
import com.example.ui.theme.RedColor
import com.example.ui.theme.SkyBlueColor
import com.example.ui.theme.WhiteColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun DeletePdfPagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var pdfFileSize by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }
    val markedForDeletion = remember { mutableStateListOf<Int>() } // 1-based page numbers

    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempPrunedFile by remember { mutableStateOf<File?>(null) }

    fun resetState() {
        thumbnails.forEach { it.recycle() }
        thumbnails.clear()
        markedForDeletion.clear()
        tempPrunedFile?.delete()
        tempPrunedFile = null
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
            if (uri != null && tempPrunedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempPrunedFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Pruned PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    fun startDeletion() {
        val uri = selectedPdfUri ?: return
        if (markedForDeletion.isEmpty()) {
            Toast.makeText(context, "Please select at least one page to delete.", Toast.LENGTH_SHORT).show()
            return
        }
        if (markedForDeletion.size >= totalPages) {
            Toast.makeText(context, "Cannot delete all pages. At least 1 page must remain.", Toast.LENGTH_LONG).show()
            return
        }

        stage = 2
        progressVal = 0.3f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "pruned_document.pdf")
                FileOutputStream(tempFile).use { fos ->
                    PdfUtils.deletePdfPages(
                        context = context,
                        pdfUri = uri,
                        pagesToDelete = markedForDeletion.toSet(),
                        outputStream = fos
                    )
                }

                withContext(Dispatchers.Main) {
                    progressVal = 1f
                    tempPrunedFile = tempFile
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Deletion failed: ${e.message}", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    ToolScreenTemplate(
        title = "Delete PDF Pages",
        desc = "Remove unwanted pages from your PDF document.",
        badgeLabel = "DELETE",
        onBack = onBack,
        badgeColor = RedColor
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
                        text = "Remove pages without quality loss or server uploads.",
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
                                        markedForDeletion.clear()
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
                                text = "HOW IT WORKS ✂️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Select PDF file", "Pick any document from your device.", RedColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Tap pages to delete", "Tap any page thumbnail to flag it for removal.", YellowColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Save updated PDF", "Download your trimmed document with remaining pages intact.", MintColor)
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
                        accentColor = RedColor
                    )

                    if (progressVal > 0f && progressVal < 1f) {
                        StageProgressBar(progress = progressVal, label = "Extracting remaining pages into new document...")
                    }

                    // Deletion summary chip
                    val remainingCount = (totalPages - markedForDeletion.size).coerceAtLeast(0)
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (markedForDeletion.isEmpty()) CreamColor else RedColor,
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (markedForDeletion.isEmpty()) {
                                        "Tap any page below to mark for deletion"
                                    } else {
                                        "${markedForDeletion.size} page(s) marked for deletion"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (markedForDeletion.isEmpty()) BlackColor else WhiteColor
                                )
                                Text(
                                    text = "$remainingCount of $totalPages pages will remain",
                                    fontSize = 11.sp,
                                    color = if (markedForDeletion.isEmpty()) Color(0xFF666666) else WhiteColor.copy(alpha = 0.85f)
                                )
                            }

                            if (markedForDeletion.isNotEmpty()) {
                                BrutalistButton(
                                    text = "Clear",
                                    onClick = { markedForDeletion.clear() },
                                    backgroundColor = WhiteColor
                                )
                            }
                        }
                    }

                    // 2-Column Interactive Page Grid
                    Text(
                        text = "Pages Grid (Tap to toggle delete)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 380.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(thumbnails) { index, bitmap ->
                            val pageNum = index + 1
                            val isDeleted = markedForDeletion.contains(pageNum)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f)
                                    .background(WhiteColor, RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isDeleted) 3.dp else 2.dp,
                                        color = if (isDeleted) RedColor else BlackColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isDeleted) {
                                            markedForDeletion.remove(pageNum)
                                        } else {
                                            markedForDeletion.add(pageNum)
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page $pageNum thumbnail",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .alpha(if (isDeleted) 0.35f else 1.0f)
                                )

                                // Page label chip at top left
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .background(BlackColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Page $pageNum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhiteColor
                                    )
                                }

                                // Delete indicator banner or badge
                                if (isDeleted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.Center)
                                            .background(RedColor)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✕ DELETE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = WhiteColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action CTA
                    BrutalistButton(
                        text = if (markedForDeletion.isEmpty()) "Select Pages to Delete" else "Delete ${markedForDeletion.size} Page(s) & Save ➔",
                        onClick = { startDeletion() },
                        backgroundColor = if (markedForDeletion.isEmpty()) CreamColor else RedColor,
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
                    val remainingCount = totalPages - markedForDeletion.size
                    ResultsHeroBanner(
                        title = "✓ Pages deleted successfully!",
                        subtitle = "Removed ${markedForDeletion.size} page(s). New PDF contains $remainingCount page(s).",
                        primaryCtaText = "Download Modified PDF 📄",
                        onPrimaryCta = {
                            val defaultName = pdfFileName.substringBeforeLast(".") + "_pruned.pdf"
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
                                text = "CHANGES SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Original Pages: $totalPages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            Text(
                                text = "Pages Deleted: ${markedForDeletion.sorted().joinToString(", ") { "#$it" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedColor
                            )
                            Text(
                                text = "Remaining Pages: $remainingCount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintColor
                            )
                        }
                    }

                    BrutalistButton(
                        text = "Delete pages from another file ↺",
                        onClick = { resetState() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
