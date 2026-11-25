package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun EditPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var pdfFileSize by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var currentPageIndex by rememberSaveable { mutableIntStateOf(1) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Editor modes: "draw", "text"
    var editorMode by rememberSaveable { mutableStateOf("draw") }

    // Drawing state
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentStrokePoints = remember { mutableStateListOf<Offset>() }
    var drawColor by remember { mutableStateOf(BlackColor) }
    var strokeWidthPx by remember { mutableFloatStateOf(8f) }

    // Text annotation state
    var textNote by rememberSaveable { mutableStateOf("") }
    var textNoteColor by remember { mutableStateOf(RedColor) }
    var textSizePt by rememberSaveable { mutableFloatStateOf(18f) }
    var textOffsetX by remember { mutableFloatStateOf(50f) }
    var textOffsetY by remember { mutableFloatStateOf(50f) }

    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempEditedFile by remember { mutableStateOf<File?>(null) }

    fun loadCurrentPageThumb(page: Int) {
        val uri = selectedPdfUri ?: return
        coroutineScope.launch(Dispatchers.IO) {
            val thumb = loadPdfPageBitmap(context, uri, page - 1)
            withContext(Dispatchers.Main) {
                currentPageBitmap?.recycle()
                currentPageBitmap = thumb
            }
        }
    }

    fun resetState() {
        currentPageBitmap?.recycle()
        currentPageBitmap = null
        strokes.clear()
        currentStrokePoints.clear()
        textNote = ""
        textOffsetX = 50f
        textOffsetY = 50f
        tempEditedFile?.delete()
        tempEditedFile = null
        selectedPdfUri = null
        pdfFileName = ""
        pdfFileSize = ""
        totalPages = 1
        currentPageIndex = 1
        progressVal = 0f
        stage = 1
    }

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempEditedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempEditedFile!!.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Edited PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    fun startExport() {
        val uri = selectedPdfUri ?: return
        val currentBitmap = currentPageBitmap ?: return
        stage = 2
        progressVal = 0.3f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Render annotations to an overlay bitmap matching the preview dimensions
                val overlayBitmap = Bitmap.createBitmap(currentBitmap.width, currentBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(overlayBitmap)

                // 1. Draw freehand strokes
                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                for (stroke in strokes) {
                    paint.color = stroke.color.toArgb()
                    paint.strokeWidth = stroke.strokeWidth
                    val pts = stroke.points
                    for (i in 0 until pts.size - 1) {
                        canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, paint)
                    }
                }

                // 2. Draw text note if present
                if (textNote.isNotBlank()) {
                    val textPaint = Paint().apply {
                        isAntiAlias = true
                        color = textNoteColor.toArgb()
                        textSize = textSizePt * 2f // Scale relative to bitmap resolution
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText(textNote, textOffsetX * 2f, textOffsetY * 2f, textPaint)
                }

                val tempFile = File(context.cacheDir, "edited_document.pdf")
                FileOutputStream(tempFile).use { fos ->
                    PdfUtils.stampOverlayOnPdfPage(
                        context = context,
                        pdfUri = uri,
                        overlayBitmap = overlayBitmap,
                        pageIndex = currentPageIndex,
                        outputStream = fos
                    )
                }
                overlayBitmap.recycle()

                withContext(Dispatchers.Main) {
                    progressVal = 1f
                    tempEditedFile = tempFile
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export edited PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    stage = 2
                }
            }
        }
    }

    ToolScreenTemplate(
        title = "Edit PDF",
        desc = "Add text, annotations, and shapes directly onto PDF pages.",
        badgeLabel = "EDIT",
        onBack = onBack,
        badgeColor = YellowColor
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
                        text = "Annotate, mark up, and edit pages 100% locally.",
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
                                    currentPageIndex = 1
                                    val thumb = loadPdfPageBitmap(context, uri, 0)
                                    withContext(Dispatchers.Main) {
                                        currentPageBitmap = thumb
                                        strokes.clear()
                                        currentStrokePoints.clear()
                                        textNote = ""
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
                                text = "HOW IT WORKS ✏️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            StepItem("01", "Select PDF file", "Pick any document from your local storage.", YellowColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("02", "Annotate & mark up", "Draw freehand or position custom text notes on any page.", PinkColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StepItem("03", "Save updated PDF", "Export high-resolution PDF with your annotations permanently embedded.", MintColor)
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
                        accentColor = YellowColor
                    )

                    if (progressVal > 0f && progressVal < 1f) {
                        StageProgressBar(progress = progressVal, label = "Rendering annotations into PDF vector streams...")
                    }

                    // Page Switcher Navigation Bar
                    BrutalistShadowBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = CreamColor,
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentPageIndex > 1) {
                                        currentPageIndex--
                                        loadCurrentPageThumb(currentPageIndex)
                                    }
                                },
                                enabled = currentPageIndex > 1
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous page",
                                    tint = if (currentPageIndex > 1) BlackColor else Color.Gray
                                )
                            }

                            Text(
                                text = "Page $currentPageIndex of $totalPages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BlackColor
                            )

                            IconButton(
                                onClick = {
                                    if (currentPageIndex < totalPages) {
                                        currentPageIndex++
                                        loadCurrentPageThumb(currentPageIndex)
                                    }
                                },
                                enabled = currentPageIndex < totalPages
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next page",
                                    tint = if (currentPageIndex < totalPages) BlackColor else Color.Gray
                                )
                            }
                        }
                    }

                    // Mode Selection: Draw vs Text
                    NeobrutalistPillSelector(
                        options = listOf("draw", "text"),
                        selectedOption = editorMode,
                        onOptionSelected = { editorMode = it },
                        labelProvider = { if (it == "draw") "✏️ Freehand Draw" else "🔤 Add Text Note" },
                        sublabelProvider = { if (it == "draw") "Pen markup & highlights" else "Type labels & annotations" },
                        activeColor = YellowColor
                    )

                    if (editorMode == "draw") {
                        // Drawing Controls
                        BrutalistShadowBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = WhiteColor,
                            cornerRadius = 12.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Pen Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    if (strokes.isNotEmpty()) {
                                        BrutalistButton(
                                            text = "Undo Last",
                                            onClick = { strokes.removeLastOrNull() },
                                            backgroundColor = CreamColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "Black" to BlackColor,
                                        "Red" to RedColor,
                                        "Blue" to SkyBlueColor,
                                        "Mint" to MintColor
                                    ).forEach { (name, color) ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(color, RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (drawColor == color) 3.dp else 1.5.dp,
                                                    color = if (drawColor == color) BlackColor else Color.Gray,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { drawColor = color }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Text Annotation Controls
                        BrutalistShadowBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = WhiteColor,
                            cornerRadius = 12.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Annotation Text:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = textNote,
                                    onValueChange = { textNote = it },
                                    placeholder = { Text("Enter text to overlay on page...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                        .semantics { contentDescription = "Annotation text" },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Text Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            RedColor,
                                            BlackColor,
                                            SkyBlueColor
                                        ).forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(color, RoundedCornerShape(6.dp))
                                                .border(
                                                    width = if (textNoteColor == color) 2.5.dp else 1.dp,
                                                    color = BlackColor,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { textNoteColor = color }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Page Canvas
                    Text(
                        text = "Page Canvas (Draw or drag note)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .background(WhiteColor, RoundedCornerShape(12.dp))
                            .border(2.dp, BlackColor, RoundedCornerShape(12.dp))
                            .pointerInput(editorMode, drawColor, strokeWidthPx) {
                                if (editorMode == "draw") {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStrokePoints.clear()
                                            currentStrokePoints.add(offset)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentStrokePoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                strokes.add(
                                                    DrawStroke(
                                                        points = currentStrokePoints.toList(),
                                                        color = drawColor,
                                                        strokeWidth = strokeWidthPx
                                                    )
                                                )
                                                currentStrokePoints.clear()
                                            }
                                        }
                                    )
                                }
                            }
                    ) {
                        // Background page image
                        if (currentPageBitmap != null) {
                            Image(
                                bitmap = currentPageBitmap!!.asImageBitmap(),
                                contentDescription = "Page $currentPageIndex preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Text Note overlay draggable
                        if (textNote.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(textOffsetX.roundToInt(), textOffsetY.roundToInt()) }
                                    .background(WhiteColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                    .border(1.5.dp, textNoteColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            textOffsetX += dragAmount.x
                                            textOffsetY += dragAmount.y
                                        }
                                    }
                            ) {
                                Text(
                                    text = textNote,
                                    fontSize = textSizePt.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textNoteColor
                                )
                            }
                        }
                    }

                    // Action CTA
                    BrutalistButton(
                        text = "Export Edited PDF ➔",
                        onClick = { startExport() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BrutalistButton(
                        text = "Select Different File ↺",
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
                        title = "✓ Edited PDF ready!",
                        subtitle = "Annotations and text note stamped onto page $currentPageIndex of $totalPages.",
                        primaryCtaText = "Download Edited PDF 📄",
                        onPrimaryCta = {
                            val defaultName = pdfFileName.substringBeforeLast(".") + "_edited.pdf"
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
                                text = "CHANGES APPLIED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Document: $pdfFileName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            Text(
                                text = "Annotated Page: #$currentPageIndex (of $totalPages pages)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            if (strokes.isNotEmpty()) {
                                Text(
                                    text = "Drawings: ${strokes.size} stroke(s) embedded",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                            if (textNote.isNotBlank()) {
                                Text(
                                    text = "Text Note: \"$textNote\"",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                        }
                    }

                    BrutalistButton(
                        text = "Edit another document ↺",
                        onClick = { resetState() },
                        backgroundColor = YellowColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
