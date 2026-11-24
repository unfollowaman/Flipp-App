package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

val PAGE_NUM_POSITIONS_LIST = listOf(
    Pair("top-left", "Top Left"),
    Pair("top-center", "Top Center"),
    Pair("top-right", "Top Right"),
    Pair("bottom-left", "Bottom Left"),
    Pair("bottom-center", "Bottom Center"),
    Pair("bottom-right", "Bottom Right")
)

val WATERMARK_POSITIONS_LIST = listOf(
    Pair("top-left", "Top Left"),
    Pair("top-center", "Top Center"),
    Pair("top-right", "Top Right"),
    Pair("center", "Center"),
    Pair("bottom-left", "Bottom Left"),
    Pair("bottom-center", "Bottom Center"),
    Pair("bottom-right", "Bottom Right")
)

val WATERMARK_COLORS_LIST = listOf(
    Pair("#000000", "Black"),
    Pair("#FFFFFF", "White"),
    Pair("#FF0000", "Red"),
    Pair("#0000FF", "Blue")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearStaleScratchFiles(this)
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
                "add_watermark" -> AddWatermarkScreen(onBack = { currentScreen = "home" })
                "text_pdf" -> TextToPdfScreen(onBack = { currentScreen = "home" })
                "pdf_text" -> PdfToTextScreen(onBack = { currentScreen = "home" })
            }
        }
    }
}

// ---------------------- HOME SCREEN ----------------------
@Composable
fun HomeScreen(onScreenNavigate: (String) -> Unit) {
    val tools = remember {
        listOf(
            ToolItem("PDF → PNG", "PDF to Images", "Convert every PDF page to crisp PNG.", "pdf_png", YellowColor, "📄"),
            ToolItem("IMG → PDF", "Images to PDF", "Combine photos into one PDF file.", "image_pdf", PinkColor, "🖼️"),
            ToolItem("MERGE", "Merge PDFs", "Combine multiple PDFs into one.", "merge_pdf", MintColor, "🔗"),
            ToolItem("SPLIT", "Split PDF", "Extract any page range instantly.", "split_pdf", SkyBlueColor, "✂️"),
            ToolItem("PROTECT", "Protect PDF", "Password-encrypt with AES-256.", "protect_pdf", WhiteColor, "🔒", YellowColor),
            ToolItem("PAGES", "Page Numbers", "Stamp numbers at any position.", "add_page_num", AmberColor, "🔢"),
            ToolItem("WATERMARK", "Add Watermark", "Stamp images or text.", "add_watermark", SkyBlueColor, "💧", WhiteColor),
            ToolItem("TXT → PDF", "Text to PDF", "Convert text to a PDF file.", "text_pdf", MintColor, "📝"),
            ToolItem("PDF → TXT", "PDF to Text", "Extract all text from a PDF document.", "pdf_text", SkyBlueColor, "📑")
        )
    }

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
                onClickLabel = "Open ${tool.title} tool",
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

        // Trust badge footer
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            BrutalistShadowBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                backgroundColor = WhiteColor,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Zero-Risk Guarantee",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    TrustRow(MintColor, "Files are never uploaded to any server")
                    TrustRow(SkyBlueColor, "No account or registration required")
                    TrustRow(YellowColor, "Conversions run directly on your hardware")
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
    FeaturePageScaffold(
        title = title,
        desc = desc,
        badgeLabel = badgeLabel,
        onBack = onBack,
        badgeColor = badgeColor
    ) {
        content()
    }
}

/**
 * Visual background thumbnail helper using Android PdfRenderer with strict resource hygiene.
 */
fun loadPdfThumbnails(context: Context, pdfUri: Uri, maxPages: Int = 4): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    try {
        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            try {
                PdfRenderer(pfd).use { renderer ->
                    val pagesToRender = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until pagesToRender) {
                        try {
                            renderer.openPage(i).use { page ->
                                val width = page.width / 2
                                val height = page.height / 2
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmaps.add(bitmap)
                            }
                        } catch (e: Exception) {
                            AppLogger.e("MainActivity", "Error rendering thumbnail page $i", e)
                        }
                    }
                }
            } catch (e: SecurityException) {
                // PdfRenderer throws SecurityException if the PDF is encrypted/password-protected
                return emptyList()
            }
        }
    } catch (e: Exception) {
        AppLogger.e("MainActivity", "Error generating PDF thumbnails", e)
    }
    return bitmaps
}

/**
 * Sweeps cache directory on app launch to purge any orphaned scratch files from previous runs.
 */
fun clearStaleScratchFiles(context: Context) {
    try {
        context.cacheDir.listFiles()?.forEach { file ->
            val name = file.name
            if (name.startsWith("converted_zip_") ||
                name.startsWith("combined_pdf_") ||
                name.startsWith("merged_pdf_") ||
                name.startsWith("split_") ||
                name.startsWith("protected_pdf_") ||
                name.startsWith("numbered_") ||
                name.startsWith("watermarked_") ||
                name.startsWith("text_generated_") ||
                name == "text_generated.pdf"
            ) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        AppLogger.e("MainActivity", "Error cleaning stale scratch files", e)
    }
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
        // Exception handled by returning null which is managed by the caller
        AppLogger.e("MainActivity", "Error loading image thumbnail", e)
        null
    }
}
