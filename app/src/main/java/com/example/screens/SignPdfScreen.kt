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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFileName by remember { mutableStateOf("document.pdf") }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    var currentPageIndex by rememberSaveable { mutableIntStateOf(1) }
    var pageThumb by remember { mutableStateOf<Bitmap?>(null) }

    // Sign Mode: "DRAW", "UPLOAD", "TYPE"
    var signMode by rememberSaveable { mutableStateOf("TYPE") }
    var signColorName by rememberSaveable { mutableStateOf("Black") }
    val activeInkColor = when (signColorName) {
        "Blue" -> Color(0xFF1E40AF)
        "Red" -> Color(0xFFDC2626)
        "Green" -> Color(0xFF15803D)
        else -> BlackColor
    }

    // Type signature mode
    var typedName by rememberSaveable { mutableStateOf("Alex Morgan") }
    var typeFontSize by rememberSaveable { mutableFloatStateOf(48f) }

    // Draw signature mode
    val drawPaths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath = remember { mutableStateListOf<Offset>() }

    // Upload mode
    var uploadedSignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Placed Signature on PDF
    var signaturePlaced by rememberSaveable { mutableStateOf(false) }
    var sigOffsetX by remember { mutableFloatStateOf(60f) }
    var sigOffsetY by remember { mutableFloatStateOf(120f) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempSignedFile by remember { mutableStateOf<File?>(null) }

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

    fun loadCurrentPageThumb(page: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val thumbs = loadPdfThumbnails(context, selectedPdfUri!!, maxPages = totalPages)
            withContext(Dispatchers.Main) {
                if (page in 1..thumbs.size) {
                    pageThumb = thumbs[page - 1]
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val bmp = loadImageThumbnail(context, uri)
                    withContext(Dispatchers.Main) {
                        uploadedSignatureBitmap = bmp
                    }
                }
            }
        }
    )

    val pdfSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null && tempSignedFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempSignedFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Signed PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    fun createSignatureBitmap(): Bitmap {
        val width = 400
        val height = 160
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (signMode == "DRAW") {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (signColorName) {
                    "Blue" -> android.graphics.Color.BLUE
                    "Red" -> android.graphics.Color.RED
                    "Green" -> android.graphics.Color.GREEN
                    else -> android.graphics.Color.BLACK
                }
                strokeWidth = 6f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            drawPaths.forEach { stroke ->
                for (i in 0 until stroke.size - 1) {
                    canvas.drawLine(stroke[i].x, stroke[i].y, stroke[i + 1].x, stroke[i + 1].y, paint)
                }
            }
        } else if (signMode == "UPLOAD" && uploadedSignatureBitmap != null) {
            return uploadedSignatureBitmap!!
        } else {
            // Type mode
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (signColorName) {
                    "Blue" -> android.graphics.Color.BLUE
                    "Red" -> android.graphics.Color.RED
                    "Green" -> android.graphics.Color.GREEN
                    else -> android.graphics.Color.BLACK
                }
                textSize = 48f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
            }
            canvas.drawText(typedName.ifEmpty { "Alex Morgan" }, 20f, 100f, paint)
        }
        return bitmap
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
                text = "Sign PDF",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = "SIGN", backgroundColor = YellowColor)
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
                    backgroundColor = YellowColor,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sign PDF",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            letterSpacing = (-0.02).em
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Draw, type, or upload your handwritten signature and position it cleanly on your PDF.",
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
                            totalPages = pages.coerceAtLeast(1)
                            currentPageIndex = 1
                            loadCurrentPageThumb(1)
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
                            text = "How to sign a PDF document",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        StepItem("1", "Upload your document", "Select the PDF file you wish to sign.", YellowColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("2", "Create your signature", "Draw with your finger, type in cursive font, or upload an image.", SkyBlueColor)
                        Spacer(modifier = Modifier.height(10.dp))
                        StepItem("3", "Place and download", "Drag your signature directly onto the target page and download.", MintColor)
                    }
                }
            }

            // Stage 2: Loaded / Signature Creator & Placement
            if (stage == 2) {
                // Mode Selector Pills
                NeobrutalistPillSelector(
                    options = listOf("DRAW", "UPLOAD", "TYPE"),
                    selectedOption = signMode,
                    onOptionSelected = { signMode = it },
                    labelProvider = {
                        when (it) {
                            "DRAW" -> "Draw"
                            "UPLOAD" -> "Upload Image"
                            else -> "Type"
                        }
                    }
                )

                // Ink Color Pills
                NeobrutalistPillSelector(
                    options = listOf("Black", "Blue", "Red", "Green"),
                    selectedOption = signColorName,
                    onOptionSelected = { signColorName = it },
                    labelProvider = { it }
                )

                // Creation Workspace Card
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (signMode == "DRAW") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("DRAW SIGNATURE", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                BrutalistButton(
                                    text = "Clear",
                                    onClick = { drawPaths.clear() },
                                    backgroundColor = WhiteColor,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                            // Drawing canvas box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                    .background(CreamColor, RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPath.clear()
                                                currentPath.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                currentPath.add(change.position)
                                            },
                                            onDragEnd = {
                                                drawPaths.add(currentPath.toList())
                                                currentPath.clear()
                                            }
                                        )
                                    }
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val drawColor = when (signColorName) {
                                        "Blue" -> Color(0xFF1E40AF)
                                        "Red" -> Color(0xFFDC2626)
                                        "Green" -> Color(0xFF15803D)
                                        else -> Color.Black
                                    }
                                    drawPaths.forEach { path ->
                                        for (i in 0 until path.size - 1) {
                                            drawLine(
                                                color = drawColor,
                                                start = path[i],
                                                end = path[i + 1],
                                                strokeWidth = 6f
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (signMode == "UPLOAD") {
                            Text("UPLOAD SIGNATURE IMAGE", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            BrutalistButton(
                                text = if (uploadedSignatureBitmap != null) "Signature Image Selected ✓" else "Choose Signature Image",
                                onClick = { imagePicker.launch(arrayOf("image/*")) },
                                backgroundColor = WhiteColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Type mode
                            Text("TYPE YOUR NAME", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            OutlinedTextField(
                                value = typedName,
                                onValueChange = { typedName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Typed signature name" },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor
                                )
                            )

                            // Cursive Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .border(2.dp, BlackColor, RoundedCornerShape(8.dp))
                                    .background(CreamColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typedName.ifEmpty { "Alex Morgan" },
                                    fontSize = 32.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    color = activeInkColor
                                )
                            }
                        }

                        BrutalistButton(
                            text = "Place Signature on PDF",
                            onClick = { signaturePlaced = true },
                            backgroundColor = MintColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Page Jumper Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pdfFileName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 1) {
                                    currentPageIndex--
                                    loadCurrentPageThumb(currentPageIndex)
                                }
                            },
                            enabled = currentPageIndex > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous page")
                        }
                        Text(
                            text = "Page $currentPageIndex of $totalPages",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next page")
                        }
                    }
                }

                // Document Page with Draggable Signature Overlay
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = WhiteColor,
                    cornerRadius = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(BackgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageThumb != null) {
                            Image(
                                bitmap = pageThumb!!.asImageBitmap(),
                                contentDescription = "Page $currentPageIndex Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Page $currentPageIndex", fontWeight = FontWeight.Bold)
                        }

                        // Draggable signature overlay
                        if (signaturePlaced) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(sigOffsetX.roundToInt(), sigOffsetY.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            sigOffsetX += dragAmount.x
                                            sigOffsetY += dragAmount.y
                                        }
                                    }
                                    .background(WhiteColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                    .border(1.5.dp, activeInkColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (signMode == "TYPE") typedName.ifEmpty { "Alex Morgan" } else "✍️ Signature",
                                    fontSize = 18.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    color = activeInkColor
                                )
                            }
                        }
                    }
                }

                // CTA Download Signed PDF Button
                BrutalistButton(
                    text = "Download Signed PDF",
                    onClick = {
                        isProcessing = true
                        progressVal = 0.3f
                        progressLabel = "Stamping signature onto PDF..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val tempFile = File.createTempFile("signed_", ".pdf", context.cacheDir)
                                tempSignedFile = tempFile
                                val sigBmp = createSignatureBitmap()
                                FileOutputStream(tempFile).use { fos ->
                                    PdfUtils.signPdf(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        signatureBitmap = sigBmp,
                                        pageIndex = currentPageIndex,
                                        xRatio = (sigOffsetX / 300f).coerceIn(0.1f, 0.8f),
                                        yRatio = (sigOffsetY / 340f).coerceIn(0.1f, 0.8f),
                                        widthRatio = 0.35f,
                                        heightRatio = 0.15f,
                                        outputStream = fos
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    stage = 3
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Signing failed: ${e.message}", Toast.LENGTH_LONG).show()
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
            }

            // Stage 3: Results
            if (stage == 3) {
                ResultsHeroBanner(
                    title = "✓ PDF Signed Successfully!",
                    subtitle = "Your signature has been placed and stamped onto page $currentPageIndex.",
                    primaryButtonText = "Download Signed PDF 📄",
                    onPrimaryClick = { pdfSaver.launch("signed_${pdfFileName}") },
                    secondaryButtonText = "Sign another file ↺",
                    onSecondaryClick = {
                        selectedPdfUri = null
                        pageThumb = null
                        signaturePlaced = false
                        tempSignedFile = null
                        stage = 1
                    }
                )
            }
        }
    }
}
