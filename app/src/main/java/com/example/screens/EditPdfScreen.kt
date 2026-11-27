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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BlackColor
import com.example.ui.theme.CreamColor
import com.example.ui.theme.MintColor
import com.example.ui.theme.PinkColor
import com.example.ui.theme.RedColor
import com.example.ui.theme.SkyBlueColor
import com.example.ui.theme.WhiteColor
import com.example.ui.theme.YellowColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun EditPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pdfFileName by rememberSaveable { mutableStateOf("") }
    var pdfFileSize by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var currentPageIndex by rememberSaveable { mutableIntStateOf(1) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Editor modes: "draw", "text"
    var editorMode by rememberSaveable { mutableStateOf("draw") }

    // Annotations are page-scoped and stored as page-relative coordinates so they survive resizing and rotation.
    val annotationState: EditPdfAnnotationViewModel = viewModel()
    val annotationsByPage = annotationState.annotationsByPage
    val currentPageAnnotations = annotationsByPage[currentPageIndex] ?: PageAnnotationSet()
    val strokes = currentPageAnnotations.strokes
    val currentTextAnnotation = currentPageAnnotations.text
    val annotatedPageCount = annotationsByPage.values.count { page ->
        page.strokes.any { it.points.isNotEmpty() } || !page.text?.text.isNullOrBlank()
    }
    val currentStrokePoints = remember { mutableStateListOf<PagePoint>() }
    var activeStrokePage by remember { mutableIntStateOf(0) }
    var activeStrokeColor by remember { mutableIntStateOf(Color.Black.toArgb()) }
    var activeStrokeWidth by remember { mutableFloatStateOf(0f) }
    var drawColor by remember { mutableStateOf(BlackColor) }
    var strokeWidthPx by remember { mutableFloatStateOf(8f) }

    // Text annotation state
    var textNoteColor by remember { mutableStateOf(RedColor) }

    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempEditedFile by remember { mutableStateOf<File?>(null) }

    fun updateCurrentPageAnnotations(transform: (PageAnnotationSet) -> PageAnnotationSet) {
        val updated = transform(annotationsByPage[currentPageIndex] ?: PageAnnotationSet())
        if (updated.strokes.isEmpty() && updated.text == null) {
            annotationsByPage.remove(currentPageIndex)
        } else {
            annotationsByPage[currentPageIndex] = updated
        }
    }

    fun clearActiveStroke() {
        currentStrokePoints.clear()
        activeStrokePage = 0
        activeStrokeWidth = 0f
    }

    fun changePage(page: Int) {
        if (page !in 1..totalPages || page == currentPageIndex) return
        currentPageIndex = page
        clearActiveStroke()
        val oldBitmap = currentPageBitmap
        currentPageBitmap = null
        oldBitmap?.recycle()
    }

    fun resetState() {
        val oldBitmap = currentPageBitmap
        currentPageBitmap = null
        oldBitmap?.recycle()
        annotationsByPage.clear()
        clearActiveStroke()
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

    LaunchedEffect(stage, selectedPdfUri, currentPageIndex) {
        val uri = selectedPdfUri
        if (stage != 2 || uri == null) return@LaunchedEffect

        val oldBitmap = currentPageBitmap
        currentPageBitmap = null
        oldBitmap?.recycle()
        try {
            val bitmap = withContext(Dispatchers.IO) {
                loadPdfPageBitmap(context, uri, currentPageIndex - 1)
                    ?: throw IllegalStateException("Could not render this PDF page")
            }
            currentPageBitmap = bitmap
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Toast.makeText(context, "Error rendering PDF page: ${error.message}", Toast.LENGTH_LONG).show()
        }
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
        val annotations = annotationsByPage.toMap()
        stage = 2
        progressVal = 0.3f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "edited_document.pdf")
                FileOutputStream(tempFile).use { fos ->
                    PdfUtils.stampAnnotationsOnPdfPages(
                        context = context,
                        pdfUri = uri,
                        annotationsByPage = annotations,
                        outputStream = fos
                    )
                }

                withContext(Dispatchers.Main) {
                    progressVal = 1f
                    tempEditedFile = tempFile
                    stage = 3
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export edited PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    progressVal = 0f
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
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    var selectedName = "document.pdf"
                                    var selectedSize = "PDF"
                                    try {
                                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                                        cursor?.use {
                                            if (it.moveToFirst()) {
                                                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                                                if (nameIndex != -1) selectedName = it.getString(nameIndex) ?: "document.pdf"
                                                if (sizeIndex != -1) {
                                                    val bytes = it.getLong(sizeIndex)
                                                    selectedSize = String.format("%.2f MB", bytes / (1024f * 1024f))
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                    val count = PdfUtils.getPdfPageCount(context, uri)
                                    withContext(Dispatchers.Main) {
                                        val oldBitmap = currentPageBitmap
                                        currentPageBitmap = null
                                        oldBitmap?.recycle()
                                        annotationsByPage.clear()
                                        clearActiveStroke()
                                        selectedPdfUri = uri
                                        pdfFileName = selectedName
                                        pdfFileSize = selectedSize
                                        totalPages = count
                                        currentPageIndex = 1
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
                                onClick = { changePage(currentPageIndex - 1) },
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
                                onClick = { changePage(currentPageIndex + 1) },
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
                                            onClick = {
                                                updateCurrentPageAnnotations { page ->
                                                    page.copy(strokes = page.strokes.dropLast(1))
                                                }
                                            },
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
                                    value = currentTextAnnotation?.text.orEmpty(),
                                    onValueChange = { value ->
                                        updateCurrentPageAnnotations { page ->
                                            val updatedText = if (value.isBlank()) {
                                                null
                                            } else {
                                                page.text?.copy(text = value) ?: PdfTextAnnotation(
                                                    text = value,
                                                    position = PagePoint(0.08f, 0.08f),
                                                    colorArgb = textNoteColor.toArgb(),
                                                    fontSize = DEFAULT_TEXT_FONT_SIZE_RATIO
                                                )
                                            }
                                            page.copy(text = updatedText)
                                        }
                                    },
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
                                                .clickable {
                                                    textNoteColor = color
                                                    updateCurrentPageAnnotations { page ->
                                                        page.copy(text = page.text?.copy(colorArgb = color.toArgb()))
                                                    }
                                                }
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

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .background(WhiteColor, RoundedCornerShape(12.dp))
                            .border(2.dp, BlackColor, RoundedCornerShape(12.dp))
                    ) {
                        val density = LocalDensity.current
                        val bitmap = currentPageBitmap
                        val pageViewport = bitmap?.let {
                            PageViewport.fit(
                                containerWidth = with(density) { maxWidth.toPx() },
                                containerHeight = with(density) { maxHeight.toPx() },
                                pageWidth = it.width.toFloat(),
                                pageHeight = it.height.toFloat()
                            )
                        }

                        // Background page image
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page $currentPageIndex preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(
                                        editorMode,
                                        currentPageIndex,
                                        bitmap.width,
                                        bitmap.height,
                                        drawColor,
                                        strokeWidthPx
                                    ) {
                                        if (editorMode == "draw") {
                                            val viewport = PageViewport.fit(
                                                containerWidth = size.width.toFloat(),
                                                containerHeight = size.height.toFloat(),
                                                pageWidth = bitmap.width.toFloat(),
                                                pageHeight = bitmap.height.toFloat()
                                            )
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    currentStrokePoints.clear()
                                                    activeStrokePage = 0
                                                    viewport.screenToPage(offset.x, offset.y)?.let { start ->
                                                        activeStrokePage = currentPageIndex
                                                        activeStrokeColor = drawColor.toArgb()
                                                        activeStrokeWidth = viewport.normalizedStrokeWidth(strokeWidthPx)
                                                        currentStrokePoints.add(start)
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    if (activeStrokePage == currentPageIndex) {
                                                        change.consume()
                                                        viewport.screenToPage(change.position.x, change.position.y)?.let {
                                                            currentStrokePoints.add(it)
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (activeStrokePage == currentPageIndex && currentStrokePoints.isNotEmpty()) {
                                                        val completedStroke = PdfStroke(
                                                            points = currentStrokePoints.toList(),
                                                            colorArgb = activeStrokeColor,
                                                            width = activeStrokeWidth
                                                        )
                                                        updateCurrentPageAnnotations { page ->
                                                            page.copy(strokes = page.strokes + completedStroke)
                                                        }
                                                    }
                                                    clearActiveStroke()
                                                },
                                                onDragCancel = { clearActiveStroke() }
                                            )
                                        }
                                    }
                            ) {
                                val viewport = PageViewport.fit(
                                    containerWidth = size.width,
                                    containerHeight = size.height,
                                    pageWidth = bitmap.width.toFloat(),
                                    pageHeight = bitmap.height.toFloat()
                                )
                                clipRect(viewport.left, viewport.top, viewport.right, viewport.bottom) {
                                    strokes.forEach { drawNormalizedStroke(it, viewport) }
                                    if (activeStrokePage == currentPageIndex && currentStrokePoints.isNotEmpty()) {
                                        drawNormalizedStroke(
                                            PdfStroke(
                                                points = currentStrokePoints.toList(),
                                                colorArgb = activeStrokeColor,
                                                width = activeStrokeWidth
                                            ),
                                            viewport
                                        )
                                    }
                                }
                            }
                        }

                        // The text annotation is clipped to the fitted page rectangle and stores page-relative position.
                        if (bitmap != null && pageViewport != null && currentTextAnnotation != null) {
                            val viewport = pageViewport
                            val pageWidthDp = with(density) { viewport.width.toDp() }
                            val pageHeightDp = with(density) { viewport.height.toDp() }
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(viewport.left.roundToInt(), viewport.top.roundToInt()) }
                                    .size(pageWidthDp, pageHeightDp)
                                    .clipToBounds()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (currentTextAnnotation.position.x * viewport.width).roundToInt(),
                                                (currentTextAnnotation.position.y * viewport.height).roundToInt()
                                            )
                                        }
                                        .background(WhiteColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                        .border(1.5.dp, Color(currentTextAnnotation.colorArgb), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .pointerInput(currentPageIndex, currentTextAnnotation.position, viewport) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val updatedPosition = PagePoint(
                                                    x = (currentTextAnnotation.position.x + dragAmount.x / viewport.width)
                                                        .coerceIn(0f, 1f),
                                                    y = (currentTextAnnotation.position.y + dragAmount.y / viewport.height)
                                                        .coerceIn(0f, 1f)
                                                )
                                                updateCurrentPageAnnotations { page ->
                                                    page.copy(text = page.text?.copy(position = updatedPosition))
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = currentTextAnnotation.text,
                                        fontSize = (currentTextAnnotation.fontSize * viewport.width / density.density).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(currentTextAnnotation.colorArgb)
                                    )
                                }
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
                        subtitle = "Annotations saved on $annotatedPageCount of $totalPages pages.",
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
                                text = "Pages with annotations: $annotatedPageCount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackColor
                            )
                            val totalStrokeCount = annotationsByPage.values.sumOf { it.strokes.size }
                            if (totalStrokeCount > 0) {
                                Text(
                                    text = "Drawings: $totalStrokeCount stroke(s) embedded",
                                    fontSize = 12.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                            val textAnnotationCount = annotationsByPage.values.count { !it.text?.text.isNullOrBlank() }
                            if (textAnnotationCount > 0) {
                                Text(
                                    text = "Text notes: $textAnnotationCount",
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

private fun DrawScope.drawNormalizedStroke(stroke: PdfStroke, viewport: PageViewport) {
    if (stroke.points.isEmpty()) return
    val color = Color(stroke.colorArgb)
    val width = viewport.screenStrokeWidth(stroke.width).coerceAtLeast(0.5f)
    if (stroke.points.size == 1) {
        val point = viewport.pageToScreen(stroke.points.first())
        drawCircle(color = color, radius = width / 2f, center = androidx.compose.ui.geometry.Offset(point.x, point.y))
        return
    }

    val path = Path().apply {
        val first = viewport.pageToScreen(stroke.points.first())
        moveTo(first.x, first.y)
        stroke.points.drop(1).forEach { pagePoint ->
            val point = viewport.pageToScreen(pagePoint)
            lineTo(point.x, point.y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
