package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.itextpdf.text.pdf.PdfReader
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf("home") }

    // Navigation and back press handling
    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen == "home" || currentScreen == "privacy") {
                TopNavbar(
                    onLogoClick = { currentScreen = "home" },
                    onPrivacyClick = { currentScreen = "privacy" }
                )
            }
        },
        bottomBar = {
            if (currentScreen == "home") {
                BottomTrustBar()
            }
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(onScreenNavigate = { currentScreen = it })
                "privacy" -> PrivacyScreen()
                "pdf_png" -> PdfToPngScreen(onBack = { currentScreen = "home" })
                "image_pdf" -> ImagesToPdfScreen(onBack = { currentScreen = "home" })
                "merge_pdf" -> MergePdfScreen(onBack = { currentScreen = "home" })
                "split_pdf" -> SplitPdfScreen(onBack = { currentScreen = "home" })
                "protect_pdf" -> ProtectPdfScreen(onBack = { currentScreen = "home" })
                "add_page_num" -> AddPageNumScreen(onBack = { currentScreen = "home" })
            }
        }
    }
}

// ---------------------- HOME SCREEN ----------------------
@Composable
fun HomeScreen(onScreenNavigate: (String) -> Unit) {
    val tools = listOf(
        ToolItem("PDF → PNG", "PDF to Images", "Convert every PDF page to crisp PNG.", "pdf_png", YellowColor, "📄"),
        ToolItem("IMG → PDF", "Images to PDF", "Combine photos into one PDF file.", "image_pdf", PinkColor, "🖼️"),
        ToolItem("MERGE", "Merge PDFs", "Combine multiple PDFs into one.", "merge_pdf", MintColor, "🔗"),
        ToolItem("SPLIT", "Split PDF", "Extract any page range instantly.", "split_pdf", SkyBlueColor, "✂️"),
        ToolItem("PROTECT", "Protect PDF", "Password-encrypt with AES-256.", "protect_pdf", WhiteColor, "🔒", YellowColor),
        ToolItem("PAGES", "Page Numbers", "Stamp numbers at any position.", "add_page_num", AmberColor, "🔢")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Section
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CreamColor)
                    .bottomBorder(2.dp, BlackColor)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .background(MintColor, RoundedCornerShape(99.dp))
                        .border(2.dp, BlackColor, RoundedCornerShape(99.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "✦ no uploads. no servers. 100% local.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor
                    )
                }
                Text(
                    text = "Built for Speed,\nDesigned for Trust.",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 33.sp,
                    letterSpacing = (-0.03).em,
                    color = BlackColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Convert PDFs and images instantly — on your device. No uploads, no waiting, complete privacy.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF444444),
                    lineHeight = 19.5.sp
                )
            }
        }

        // Tools Label
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text(
                text = "TOOLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.em,
                color = Color(0xFF888888),
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
            )
        }

        // Tools Grid Items
        items(tools.size) { index ->
            val tool = tools[index]
            val isLeft = index % 2 == 0
            BrutalistShadowBox(
                modifier = Modifier
                    .padding(
                        start = if (isLeft) 16.dp else 6.dp,
                        end = if (isLeft) 6.dp else 16.dp,
                        bottom = 12.dp
                    ),
                backgroundColor = tool.badgeColor,
                cornerRadius = 16.dp,
                onClick = { onScreenNavigate(tool.screenKey) },
                testTag = "tool_card_${tool.screenKey}"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 16.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolBadge(
                        text = tool.badge,
                        backgroundColor = tool.badgeBgColor ?: WhiteColor
                    )
                    
                    Text(
                        text = tool.icon,
                        fontSize = 22.sp
                    )
                    
                    Text(
                        text = tool.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        letterSpacing = (-0.01).em
                    )
                    Text(
                        text = tool.desc,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF666666),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // How it works
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Text(
                    text = "HOW IT WORKS 🥧",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.em,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
                
                StepItem("01", "Pick your file", "Tap a tool and select a PDF or image from your device.", YellowColor)
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE8E8E8)))
                StepItem("02", "Choose options", "Set resolution, page range, or position. Everything on-device.", SkyBlueColor)
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE8E8E8)))
                StepItem("03", "Save instantly", "Download your converted file. Nothing stored or transmitted.", MintColor)
            }
        }

        // Trust Card
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            BrutalistShadowBox(
                modifier = Modifier.padding(horizontal = 16.dp),
                backgroundColor = CreamColor,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your files never leave your device. 🔒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    TrustRow(MintColor, "No uploads to any server")
                    TrustRow(YellowColor, "No account needed")
                    TrustRow(SkyBlueColor, "Works offline")
                    TrustRow(PinkColor, "Open source libraries")
                }
            }
        }
    }
}

@Composable
fun StepItem(number: String, title: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, RoundedCornerShape(6.dp))
                .border(2.dp, BlackColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = BlackColor)
        }
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BlackColor)
            Text(text = desc, fontSize = 11.sp, color = Color(0xFF666666), lineHeight = 15.sp)
        }
    }
}

@Composable
fun TrustRow(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
                .border(1.5.dp, BlackColor, androidx.compose.foundation.shape.CircleShape)
        )
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BlackColor)
    }
}

data class ToolItem(val badge: String, val title: String, val desc: String, val screenKey: String, val badgeColor: Color, val icon: String = "", val badgeBgColor: Color? = null)


// ---------------------- PRIVACY SCREEN ----------------------
@Composable
fun PrivacyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MintColor,
                    cornerRadius = 24.dp
                ) {
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp)) {
                        Text(text = "🔒", fontSize = 28.sp, modifier = Modifier.padding(bottom = 10.dp))
                        Text(
                            text = "Your privacy is the product.",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.02).em,
                            lineHeight = 24.sp,
                            color = BlackColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "flipp was built with one rule: your files never leave your device. Ever.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF1A1A1A),
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrivacyCard("🚫", "No Uploads", "Your file is never uploaded. Everything is processed on your device using local libraries.", WhiteColor)
                    PrivacyCard("📡", "No Network Calls", "The app makes zero network requests during file processing. Works fully offline.", CreamColor)
                    PrivacyCard("👤", "No Account Needed", "No sign-up. No email. No profile. Just install and use.", WhiteColor)
                    PrivacyCard("📦", "Open Source Libraries", "Built on open-source PDF libraries. No proprietary black boxes.", YellowColor)
                }
            }

            item {
                BrutalistShadowBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BlackColor,
                    cornerRadius = 16.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "✦ no uploads. no servers. 100% local.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WhiteColor,
                            letterSpacing = 0.02.em
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyCard(icon: String, title: String, body: String, bgColor: Color) {
    BrutalistShadowBox(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = bgColor,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BlackColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = body,
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    lineHeight = 16.5.sp
                )
            }
        }
    }
}


// ---------------------- TOOL 1: PDF → PNG ----------------------
@Composable
fun PdfToPngScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var scaleOption by rememberSaveable { mutableIntStateOf(2) } // 1x, 2x, 3x
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    
    // Extracted thumbnails of first few pages mapping Stage 3
    val pdfThumbnails = remember { mutableStateListOf<Bitmap>() }
    var tempZipFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedPdfUri = uri
                stage = 2
                // Load thumbnails in stage background
                coroutineScope.launch {
                    pdfThumbnails.clear()
                    val thumbs = withContext(Dispatchers.IO) {
                        loadPdfThumbnails(context, uri, maxPages = 6)
                    }
                    if (thumbs.isEmpty()) {
                        Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                        stage = 1
                    } else {
                        pdfThumbnails.addAll(thumbs)
                    }
                }
            } else {
                Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Saver contract
    val fileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null && tempZipFile != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            tempZipFile!!.inputStream().copyTo(out)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "ZIP Saved successfully!", Toast.LENGTH_SHORT).show()
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

    ToolScreenTemplate(
        title = "PDF to Images",
        desc = "Convert every page of a PDF into a high-quality PNG image. Perfect for previews, thumbnails, or sharing individual pages.",
        badgeLabel = "PDF → PNG",
        onBack = onBack,
        badgeColor = YellowColor
    ) {
        when (stage) {
            1 -> {
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("application/pdf")) },
                    prompt = "Please select a PDF file.",
                    badgeColor = YellowColor
                )
            }
            2 -> {
                // Options Layout
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Conversion Options",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resolution / Scale Factor:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                listOf(1, 2, 3).forEach { scale ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = scaleOption == scale,
                                            onClick = { scaleOption = scale },
                                            colors = RadioButtonDefaults.colors(selectedColor = BlackColor)
                                        )
                                        Text("${scale}x", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    
                    BrutalistButton(
                        text = "Next ➜",
                        onClick = { stage = 3 },
                        backgroundColor = YellowColor
                    )
                }
            }
            3 -> {
                // Preview Layout
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Page Previews (${pdfThumbnails.size} pages loaded)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        items(pdfThumbnails) { bitmap ->
                            Card(
                                modifier = Modifier
                                    .height(100.dp)
                                    .border(1.dp, BlackColor, RoundedCornerShape(6.dp)),
                                colors = CardDefaults.cardColors(containerColor = WhiteColor)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "PDF Page Thumbnail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    BrutalistButton(
                        text = "Convert All Pages →",
                        onClick = {
                            stage = 4
                            progressVal = 0.1f
                            progressLabel = "Starting conversion..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("converted_zip_", ".zip", context.cacheDir)
                                    tempZipFile = tempFile
                                    
                                    val fos = FileOutputStream(tempFile)
                                    PdfUtils.convertPdfToPngZip(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        scale = scaleOption,
                                        outputStream = fos
                                    ) { current, total ->
                                        progressVal = current.toFloat() / total
                                        progressLabel = "Converting page $current of $total... (${(progressVal * 100).toInt()}%)"
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Processing complete!", Toast.LENGTH_SHORT).show()
                                        stage = 5
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                                        stage = 1
                                    }
                                }
                            }
                        },
                        backgroundColor = PinkColor
                    )
                }
            }
            4 -> {
                StageProgressBar(progress = progressVal, label = progressLabel)
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Conversion Successful!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "All pages have been converted 100% locally to PNG images and compiled into a secure ZIP compression pack.",
                        fontSize = 14.sp,
                        color = BlackColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    BrutalistButton(
                        text = "Download All as ZIP 📦",
                        onClick = { fileSaver.launch("converted_pages.zip") },
                        backgroundColor = MintColor,
                        testTag = "download_zip_button"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = {
                            selectedPdfUri = null
                            pdfThumbnails.clear()
                            tempZipFile = null
                            stage = 1
                        },
                        backgroundColor = YellowColor
                    )
                }
            }
        }
    }
}


// ---------------------- TOOL 2: IMAGES → PDF ----------------------
@Composable
fun ImagesToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var stage by rememberSaveable { mutableIntStateOf(1) }
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    val imageThumbnailsMap = remember { mutableStateMapOf<Uri, Bitmap>() }
    
    var pageSizeSelection by rememberSaveable { mutableStateOf("AUTO") } // A4, Letter, Auto
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedImageUris.addAll(uris)
                stage = 2
                
                // Load thumbnails
                coroutineScope.launch {
                    uris.forEach { uri ->
                        if (!imageThumbnailsMap.containsKey(uri)) {
                            val thumb = withContext(Dispatchers.IO) {
                                loadImageThumbnail(context, uri)
                            }
                            if (thumb != null) {
                                imageThumbnailsMap[uri] = thumb
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "No files selected.", Toast.LENGTH_SHORT).show()
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

    ToolScreenTemplate(
        title = "Images to PDF",
        desc = "Combine one or more PNG or JPG images into a single PDF. Reorder pages by dragging before you convert.",
        badgeLabel = "IMG → PDF",
        onBack = onBack,
        badgeColor = PinkColor
    ) {
        when (stage) {
            1 -> {
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("image/*")) },
                    prompt = "Combine multiple PNG/JPG into one PDF.",
                    badgeColor = PinkColor
                )
            }
            2 -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Document Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Page Layout Size:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf("AUTO", "A4", "LETTER").forEach { size ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { pageSizeSelection = size }
                                ) {
                                    RadioButton(
                                        selected = pageSizeSelection == size,
                                        onClick = { pageSizeSelection = size },
                                        colors = RadioButtonDefaults.colors(selectedColor = BlackColor)
                                    )
                                    Text(size, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    BrutalistButton(
                        text = "Proceed to Alignment ➜",
                        onClick = { stage = 3 },
                        backgroundColor = PinkColor
                    )
                }
            }
            3 -> {
                // Ordering & Preview
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Arrange Page Sequence (${selectedImageUris.size} images)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        itemsIndexed(selectedImageUris) { index, uri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(CreamColor, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, BlackColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail display
                                val bmp = imageThumbnailsMap[uri]
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .border(1.dp, BlackColor, RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(WhiteColor)
                                            .border(1.dp, BlackColor, RoundedCornerShape(4.dp))
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = "Page ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // UP arrow
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(enabled = index > 0) {
                                            val current = selectedImageUris[index]
                                            selectedImageUris.removeAt(index)
                                            selectedImageUris.add(index - 1, current)
                                        }
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                // DOWN arrow
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(enabled = index < selectedImageUris.size - 1) {
                                            val current = selectedImageUris[index]
                                            selectedImageUris.removeAt(index)
                                            selectedImageUris.add(index + 1, current)
                                        }
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))

                                // Remove Trash icon
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = RedColor,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            selectedImageUris.removeAt(index)
                                            if (selectedImageUris.isEmpty()) {
                                                stage = 1
                                            }
                                        }
                                )
                            }
                        }
                    }

                    // Add more files button inside alignment stage
                    BrutalistButton(
                        text = "+ Add More Images",
                        onClick = { filePicker.launch(arrayOf("image/*")) },
                        backgroundColor = WhiteColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    BrutalistButton(
                        text = "Convert All Pages →",
                        onClick = {
                            stage = 4
                            progressVal = 0.2f
                            progressLabel = "Packing images..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("combined_pdf_", ".pdf", context.cacheDir)
                                    tempPdfFile = tempFile
                                    
                                    val fos = FileOutputStream(tempFile)
                                    PdfUtils.combineImagesToPdf(
                                        context = context,
                                        imageUris = selectedImageUris.toList(),
                                        pageSizeOption = pageSizeSelection,
                                        outputStream = fos
                                    ) { current, total ->
                                        progressVal = current.toFloat() / total
                                        progressLabel = "Adding image $current of $total... (${(progressVal * 100).toInt()}%)"
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Processing complete!", Toast.LENGTH_SHORT).show()
                                        stage = 5
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Encryption/Packing failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        stage = 1
                                    }
                                }
                            }
                        },
                        backgroundColor = PinkColor
                    )
                }
            }
            4 -> {
                StageProgressBar(progress = progressVal, label = progressLabel)
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 PDF Compiled!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    BrutalistButton(
                        text = "Download Combined PDF 📁",
                        onClick = { pdfSaver.launch("combined_document.pdf") },
                        backgroundColor = MintColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = {
                            selectedImageUris.clear()
                            imageThumbnailsMap.clear()
                            tempPdfFile = null
                            stage = 1
                        },
                        backgroundColor = PinkColor
                    )
                }
            }
        }
    }
}


// ---------------------- TOOL 3: MERGE PDF ----------------------
@Composable
fun MergePdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var stage by rememberSaveable { mutableIntStateOf(1) }
    val selectedPdfUris = remember { mutableStateListOf<Uri>() }
    val pdfPageCounts = remember { mutableStateMapOf<Uri, Int>() }
    
    var progressVal by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("") }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedPdfUris.addAll(uris)
                stage = 2
                
                // Load PDF sizes
                coroutineScope.launch(Dispatchers.IO) {
                    uris.forEach { uri ->
                        if (!pdfPageCounts.containsKey(uri)) {
                            try {
                                val count = PdfUtils.getPdfPageCount(context, uri)
                                pdfPageCounts[uri] = count
                            } catch (e: Exception) {
                                pdfPageCounts[uri] = 0
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Merged PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    ToolScreenTemplate(
        title = "Merge PDFs",
        desc = "Combine multiple PDF files into one clean document in the order you choose.",
        badgeLabel = "MERGE",
        onBack = onBack,
        badgeColor = MintColor
    ) {
        when (stage) {
            1 -> {
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("application/pdf")) },
                    prompt = "Combine multiple PDFs into one.",
                    badgeColor = MintColor
                )
            }
            2 -> {
                // Options / Arrange
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sequence Order (${selectedPdfUris.size} PDFs)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        itemsIndexed(selectedPdfUris) { index, uri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(CreamColor, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, BlackColor, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val name = uri.path?.substringAfterLast("/") ?: "PDF File"
                                    Text(
                                        text = "${index + 1}. $name",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${pdfPageCounts[uri] ?: "?"} pages",
                                        fontSize = 12.sp,
                                        color = BlackColor.copy(alpha = 0.6f)
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(enabled = index > 0) {
                                            val cur = selectedPdfUris[index]
                                            selectedPdfUris.removeAt(index)
                                            selectedPdfUris.add(index - 1, cur)
                                        }
                                )
                                
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(enabled = index < selectedPdfUris.size - 1) {
                                            val cur = selectedPdfUris[index]
                                            selectedPdfUris.removeAt(index)
                                            selectedPdfUris.add(index + 1, cur)
                                        }
                                )

                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = RedColor,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            selectedPdfUris.removeAt(index)
                                            if (selectedPdfUris.isEmpty()) {
                                                stage = 1
                                            }
                                        }
                                )
                            }
                        }
                    }

                    BrutalistButton(
                        text = "+ Add More PDF Files",
                        onClick = { filePicker.launch(arrayOf("application/pdf")) },
                        backgroundColor = WhiteColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    BrutalistButton(
                        text = "Convert All Pages →",
                        onClick = {
                            stage = 4
                            progressVal = 0.2f
                            progressLabel = "Starting merge..."
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("merged_pdf_", ".pdf", context.cacheDir)
                                    tempPdfFile = tempFile
                                    
                                    val fos = FileOutputStream(tempFile)
                                    PdfUtils.mergePdfs(
                                        context = context,
                                        pdfUris = selectedPdfUris.toList(),
                                        outputStream = fos
                                    ) { merged, total ->
                                        progressVal = merged.toFloat() / total
                                        progressLabel = "Merged $merged of $total files... (${(progressVal * 100).toInt()}%)"
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Merge completed!", Toast.LENGTH_SHORT).show()
                                        stage = 5
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                                        stage = 1
                                    }
                                }
                            }
                        },
                        backgroundColor = MintColor
                    )
                }
            }
            4 -> {
                StageProgressBar(progress = progressVal, label = progressLabel)
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 PDFs Merged Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    BrutalistButton(
                        text = "Download Combined PDF 📁",
                        onClick = { pdfSaver.launch("merged_document.pdf") },
                        backgroundColor = MintColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = {
                            selectedPdfUris.clear()
                            pdfPageCounts.clear()
                            tempPdfFile = null
                            stage = 1
                        },
                        backgroundColor = MintColor
                    )
                }
            }
        }
    }
}


// ---------------------- TOOL 4: SPLIT PDF ----------------------
@Composable
fun SplitPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var totalPages by rememberSaveable { mutableIntStateOf(1) }
    
    // Inputs
    var startPageStr by remember { mutableStateOf("1") }
    var endPageStr by remember { mutableStateOf("1") }
    
    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedPdfUri = uri
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val pages = PdfUtils.getPdfPageCount(context, uri)
                        totalPages = pages
                        startPageStr = "1"
                        endPageStr = pages.toString()
                        withContext(Dispatchers.Main) {
                            stage = 2
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Split PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    ToolScreenTemplate(
        title = "Split PDF",
        desc = "Extract a specific page range from a PDF into a separate file.",
        badgeLabel = "SPLIT",
        onBack = onBack,
        badgeColor = SkyBlueColor
    ) {
        when (stage) {
            1 -> {
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("application/pdf")) },
                    prompt = "Extract specific page range into a separate file.",
                    badgeColor = SkyBlueColor
                )
            }
            2 -> {
                // Set Range UI
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Specify Page Range (1 to $totalPages)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("From Page:", fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = startPageStr,
                                        onValueChange = { startPageStr = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BlackColor,
                                            unfocusedBorderColor = BlackColor
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("To Page:", fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = endPageStr,
                                        onValueChange = { endPageStr = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BlackColor,
                                            unfocusedBorderColor = BlackColor
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                    
                    BrutalistButton(
                        text = "Next ➜",
                        onClick = {
                            val start = startPageStr.toIntOrNull() ?: 1
                            val end = endPageStr.toIntOrNull() ?: totalPages
                            if (start < 1 || end > totalPages || start > end) {
                                Toast.makeText(context, "No valid pages in range.", Toast.LENGTH_LONG).show()
                            } else {
                                stage = 3
                            }
                        },
                        backgroundColor = SkyBlueColor
                    )
                }
            }
            3 -> {
                // Confirmation / Review
                val rangeStart = startPageStr.toIntOrNull() ?: 1
                val rangeEnd = endPageStr.toIntOrNull() ?: totalPages
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Review Extraction Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total source document pages: $totalPages", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Extracted set: Page $rangeStart to Page $rangeEnd", fontWeight = FontWeight.Medium)
                            Text("Total extracted pages: ${rangeEnd - rangeStart + 1}", fontWeight = FontWeight.Medium)
                        }
                    }

                    BrutalistButton(
                        text = "Convert All Pages →",
                        onClick = {
                            stage = 4
                            progressVal = 0.5f // Quick transition for direct range extraction
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("split_", ".pdf", context.cacheDir)
                                    tempPdfFile = tempFile
                                    
                                    val fos = FileOutputStream(tempFile)
                                    PdfUtils.splitPdf(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        startPage = rangeStart,
                                        endPage = rangeEnd,
                                        outputStream = fos
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Split Completed successfully!", Toast.LENGTH_SHORT).show()
                                        stage = 5
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "No valid pages in range.", Toast.LENGTH_LONG).show()
                                        stage = 1
                                    }
                                }
                            }
                        },
                        backgroundColor = SkyBlueColor
                    )
                }
            }
            4 -> {
                StageProgressBar(progress = progressVal, label = "Splitting PDF... extracting ranges...")
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Pages Extracted Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    BrutalistButton(
                        text = "Download Combined PDF 📁",
                        onClick = { pdfSaver.launch("split_document.pdf") },
                        backgroundColor = MintColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = {
                            selectedPdfUri = null
                            totalPages = 1
                            tempPdfFile = null
                            stage = 1
                        },
                        backgroundColor = SkyBlueColor
                    )
                }
            }
        }
    }
}


// ---------------------- TOOL 5: PROTECT PDF ----------------------
@Composable
fun ProtectPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    
    var isPasswordVisible by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedPdfUri = uri
                stage = 2
            } else {
                Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Encrypted PDF protected successfully!", Toast.LENGTH_SHORT).show()
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

    ToolScreenTemplate(
        title = "Protect PDF",
        desc = "Password-encrypt a PDF file. Encrypts documents to prevent unauthorized access.",
        badgeLabel = "PROTECT",
        onBack = onBack,
        badgeColor = YellowColor
    ) {
        when (stage) {
            1 -> {
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("application/pdf")) },
                    prompt = "AES password-encrypt a PDF entirely on-device.",
                    badgeColor = YellowColor
                )
            }
            2 -> {
                // Password Options
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Configure Password Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Set Document Password (AES-128):", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock icon",
                                        modifier = Modifier.clickable { isPasswordVisible = !isPasswordVisible }
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor
                                )
                            )
                        }
                    }
                    
                    BrutalistButton(
                        text = "Next ➜",
                        onClick = {
                            if (password.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter a password.", Toast.LENGTH_SHORT).show()
                            } else {
                                stage = 3
                            }
                        },
                        backgroundColor = YellowColor
                    )
                }
            }
            3 -> {
                // Confirmation screen
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Verify Cryptography Protection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🔐 Ready to encrypt PDF file", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Security Mode: 128-bit AES Encryption Block", fontSize = 14.sp)
                            Text("This password can never be retrieved by any servers. Save it carefully.", fontSize = 12.sp, color = RedColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    BrutalistButton(
                        text = "Convert All Pages →",
                        onClick = {
                            stage = 4
                            progressVal = 0.5f
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("protected_pdf_", ".pdf", context.cacheDir)
                                    tempPdfFile = tempFile
                                    
                                    val fos = FileOutputStream(tempFile)
                                    PdfUtils.protectPdf(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        password = java.lang.String(password),
                                        outputStream = fos
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Document Protected!", Toast.LENGTH_SHORT).show()
                                        stage = 5
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                                        stage = 1
                                    }
                                }
                            }
                        },
                        backgroundColor = YellowColor
                    )
                }
            }
            4 -> {
                StageProgressBar(progress = progressVal, label = "Signing keys... Encrypting pages natively...")
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 PDF Locked Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    BrutalistButton(
                        text = "Download Combined PDF 📁",
                        onClick = { pdfSaver.launch("encrypted_document.pdf") },
                        backgroundColor = MintColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = {
                            selectedPdfUri = null
                            password = ""
                            tempPdfFile = null
                            stage = 1
                        },
                        backgroundColor = YellowColor
                    )
                }
            }
        }
    }
}


// ---------------------- TOOL 6: ADD PAGE NUMBERS ----------------------
@Composable
fun AddPageNumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var stage by rememberSaveable { mutableIntStateOf(1) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    
    // Config values
    var position by rememberSaveable { mutableStateOf("bottom-center") } // top-left ... bottom-right
    var startNumStr by remember { mutableStateOf("1") }
    
    var progressVal by remember { mutableFloatStateOf(0f) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedPdfUri = uri
                stage = 2
            } else {
                Toast.makeText(context, "Please select a PDF file.", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Page numbered PDF saved successfully!", Toast.LENGTH_SHORT).show()
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

    val positionsList = listOf(
        Pair("top-left", "Top Left"),
        Pair("top-center", "Top Center"),
        Pair("top-right", "Top Right"),
        Pair("bottom-left", "Bottom Left"),
        Pair("bottom-center", "Bottom Center"),
        Pair("bottom-right", "Bottom Right")
    )

    ToolScreenTemplate(
        title = "Add Page Numbers",
        desc = "Add page numbers to an existing PDF. Choose the page to start from and exactly where the numbers should appear.",
        badgeLabel = "PAGES",
        onBack = onBack,
        badgeColor = AmberColor
    ) {
        when (stage) {
            1 -> {
                DropZone(
                    onBrowseClick = { filePicker.launch(arrayOf("application/pdf")) },
                    prompt = "Stamp page numbers with customizable position and offset start page.",
                    badgeColor = MintColor
                )
            }
            2 -> {
                // Select positions grid & start offset
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Customize Styling & Anchors",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Page Number Index Start Offset:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = startNumStr,
                                onValueChange = { startNumStr = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlackColor,
                                    unfocusedBorderColor = BlackColor
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Physical Stamp Alignment:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // 2x3 Brutalist grid for position targets
                            Column {
                                listOf(0, 1).forEach { rowIdx ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(0, 1, 2).forEach { colIdx ->
                                            val index = rowIdx * 3 + colIdx
                                            val posItem = positionsList[index]
                                            val isSelected = position == posItem.first
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .padding(vertical = 4.dp)
                                                    .background(
                                                        if (isSelected) MintColor else WhiteColor,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        BlackColor,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { position = posItem.first }
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = posItem.second,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BlackColor,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    BrutalistButton(
                        text = "Next ➜",
                        onClick = { stage = 3 },
                        backgroundColor = MintColor
                    )
                }
            }
            3 -> {
                // Confirmation screen
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Review Stamp Layout",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    BrutalistShadowBox(
                        backgroundColor = CreamColor,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ready to stamp page numbers. 🔢", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Stamp positioning target: ${position.uppercase()}", fontSize = 14.sp)
                            Text("Starting sequence format: ${startNumStr.toIntOrNull() ?: 1}", fontSize = 14.sp)
                        }
                    }

                    BrutalistButton(
                        text = "Convert All Pages →",
                        onClick = {
                            stage = 4
                            progressVal = 0.5f
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val tempFile = File.createTempFile("numbered_", ".pdf", context.cacheDir)
                                    tempPdfFile = tempFile
                                    
                                    val fos = FileOutputStream(tempFile)
                                    val startNum = startNumStr.toIntOrNull() ?: 1
                                    
                                    PdfUtils.addPageNumbers(
                                        context = context,
                                        pdfUri = selectedPdfUri!!,
                                        position = position,
                                        startNumber = startNum,
                                        outputStream = fos
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Page Stamping complete!", Toast.LENGTH_SHORT).show()
                                        stage = 5
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to load PDF. Is it a valid, non-encrypted file?", Toast.LENGTH_LONG).show()
                                        stage = 1
                                    }
                                }
                            }
                        },
                        backgroundColor = MintColor
                    )
                }
            }
            4 -> {
                StageProgressBar(progress = progressVal, label = "Stamping page digits sequentially...")
            }
            5 -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 PDF Stamped Successfully!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    BrutalistButton(
                        text = "Download Combined PDF 📁",
                        onClick = { pdfSaver.launch("page_numbered_document.pdf") },
                        backgroundColor = MintColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BrutalistButton(
                        text = "Convert another file ↺",
                        onClick = {
                            selectedPdfUri = null
                            startNumStr = "1"
                            tempPdfFile = null
                            stage = 1
                        },
                        backgroundColor = MintColor
                    )
                }
            }
        }
    }
}


// ---------------------- UTILS: SCREEN TEMPLATE & PREVIEW RENDERING ----------------------
@Composable
fun ToolScreenTemplate(
    title: String,
    desc: String,
    badgeLabel: String,
    onBack: () -> Unit,
    badgeColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(WhiteColor)
                .bottomBorder(2.dp, BlackColor)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(WhiteColor, RoundedCornerShape(6.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(6.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = BlackColor
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlackColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.02).em
            )
            ToolBadge(text = badgeLabel, backgroundColor = badgeColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            BrutalistShadowBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = badgeColor,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = BlackColor,
                        letterSpacing = (-0.02).em,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = Color(0xFF333333),
                        lineHeight = 18.sp
                    )
                }
            }

            content()
        }
    }
}

/**
 * Visual background thumbnail helper using Android PdfRenderer
 */
fun loadPdfThumbnails(context: Context, pdfUri: Uri, maxPages: Int = 4): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            val reader = PdfReader(context.contentResolver.openInputStream(pdfUri))
            // Check if encrypted before feeding to PdfRenderer to prevent system crashes
            if (reader.isEncrypted) {
                reader.close()
                return emptyList()
            }
            reader.close()
            
            val renderer = PdfRenderer(pfd)
            val pagesToRender = minOf(renderer.pageCount, maxPages)
            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val width = page.width / 2
                val height = page.height / 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return bitmaps
}

/**
 * Responsive thumbnail sizing helper
 */
fun loadImageThumbnail(context: Context, uri: Uri): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 8
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
