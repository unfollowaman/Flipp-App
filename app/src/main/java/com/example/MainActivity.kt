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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
                // Phase 1: High Traffic Conversion
                "pdf_png" -> PdfToPngScreen(onBack = { currentScreen = "home" })
                "image_pdf" -> ImagesToPdfScreen(onBack = { currentScreen = "home" })
                "merge_pdf" -> MergePdfScreen(onBack = { currentScreen = "home" })
                "split_pdf" -> SplitPdfScreen(onBack = { currentScreen = "home" })
                // Phase 2: Security & Stamping
                "protect_pdf" -> ProtectPdfScreen(onBack = { currentScreen = "home" })
                "unlock_pdf" -> UnlockPdfScreen(onBack = { currentScreen = "home" })
                "add_page_num" -> AddPageNumScreen(onBack = { currentScreen = "home" })
                "add_watermark" -> AddWatermarkScreen(onBack = { currentScreen = "home" })
                "sign_pdf" -> SignPdfScreen(onBack = { currentScreen = "home" })
                // Phase 3: Content Extraction & Text Conversion
                "pdf_text" -> PdfToTextScreen(onBack = { currentScreen = "home" })
                "text_pdf" -> TextToPdfScreen(onBack = { currentScreen = "home" })
                "pdf_word" -> PdfToWordScreen(onBack = { currentScreen = "home" })
                // Phase 4: Page Management & Optimization
                "compress_pdf" -> CompressPdfScreen(onBack = { currentScreen = "home" })
                "delete_pdf" -> DeletePdfPagesScreen(onBack = { currentScreen = "home" })
                "rearrange_pdf" -> RearrangePdfScreen(onBack = { currentScreen = "home" })
                // Phase 5: Interactive Editor
                "edit_pdf" -> EditPdfScreen(onBack = { currentScreen = "home" })
            }
        }
    }
}

// ---------------------- HOME SCREEN ----------------------
@Composable
fun HomeScreen(onScreenNavigate: (String) -> Unit) {
    val tools = remember {
        listOf(
            // High-Traffic Conversion Utilities
            ToolItem("PDF → PNG", "PDF to Images", "Convert every PDF page to crisp PNG.", "pdf_png", YellowColor, "📄"),
            ToolItem("IMG → PDF", "Images to PDF", "Combine photos into one PDF file.", "image_pdf", SkyBlueColor, "🖼️"),
            ToolItem("MERGE", "Merge PDFs", "Combine multiple PDFs into one.", "merge_pdf", PinkColor, "🔗"),
            ToolItem("SPLIT", "Split PDF", "Extract any page range instantly.", "split_pdf", RedColor, "✂️"),

            // Document Security & Stamping Utilities
            ToolItem("PROTECT", "Protect PDF", "Password-encrypt with AES-256.", "protect_pdf", AmberColor, "🔒"),
            ToolItem("UNLOCK", "Unlock PDF", "Remove password protection locally.", "unlock_pdf", MintColor, "🔓"),
            ToolItem("PAGES", "Page Numbers", "Stamp numbers at any position.", "add_page_num", YellowColor, "🔢"),
            ToolItem("WATERMARK", "Add Watermark", "Stamp images or text overlays.", "add_watermark", SkyBlueColor, "💧"),
            ToolItem("SIGN", "Sign PDF", "Draw, type, or place signature.", "sign_pdf", YellowColor, "✍️"),

            // Content Extraction & Text Conversion
            ToolItem("PDF → TXT", "PDF to Text", "Extract all text from a PDF document.", "pdf_text", SkyBlueColor, "📑"),
            ToolItem("TXT → PDF", "Text to PDF", "Convert raw text to a PDF file.", "text_pdf", PinkColor, "📝"),
            ToolItem("PDF → WORD", "PDF to Word", "Convert PDF to editable DOCX format.", "pdf_word", SkyBlueColor, "📄"),

            // Page Management & Optimization
            ToolItem("COMPRESS", "Compress PDF", "Reduce PDF size with smart compression.", "compress_pdf", PinkColor, "📦"),
            ToolItem("DELETE", "Delete Pages", "Remove unwanted pages from PDF.", "delete_pdf", RedColor, "🗑️"),
            ToolItem("REORDER", "Rearrange PDF", "Reorder, sort, and organize pages.", "rearrange_pdf", MintColor, "🔄"),

            // Interactive Editor
            ToolItem("EDIT", "Edit PDF", "Draw, markup, and annotate pages.", "edit_pdf", YellowColor, "✏️")
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
                text = "ALL 16 TOOLS",
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
                        maxLines = 1
                    )

                    Text(
                        text = tool.desc,
                        fontSize = 11.sp,
                        color = Color(0xFF444444),
                        lineHeight = 15.sp,
                        maxLines = 2
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
                backgroundColor = CreamColor,
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Zero-Risk Guarantee",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlackColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    TrustRow(MintColor, "Files are never uploaded to any server")
                    TrustRow(SkyBlueColor, "No account or registration required")
                    TrustRow(PinkColor, "100% on-device local processing")
                }
            }
        }
    }
}

@Composable
fun StepItem(number: String, title: String, desc: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor, CircleShape)
                .border(2.dp, BlackColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlackColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFF555555),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun TrustRow(dotColor: Color, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
                .border(1.5.dp, BlackColor, CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )
    }
}

data class ToolItem(
    val badge: String,
    val title: String,
    val desc: String,
    val screenKey: String,
    val badgeColor: Color,
    val icon: String,
    val badgeBgColor: Color? = null
)

// ---------------------- PRIVACY POLICY SCREEN ----------------------
@Composable
fun PrivacyScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero badge
        Box(
            modifier = Modifier
                .background(MintColor, RoundedCornerShape(99.dp))
                .border(2.dp, BlackColor, RoundedCornerShape(99.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🛡️ 100% OFFLINE PRIVACY GUARANTEE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                letterSpacing = 0.05.em
            )
        }

        Text(
            text = "Your Files Never\nLeave Your Phone.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 32.sp,
            letterSpacing = (-0.03).em,
            color = BlackColor
        )

        Text(
            text = "Most file converters send your documents to a cloud server to be processed. Flipp doesn't. Every single tool runs locally on your Android device.",
            fontSize = 13.sp,
            color = Color(0xFF444444),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        PrivacyCard("📡", "Zero Network Traffic", "Flipp has no network permissions in its manifest. It cannot communicate with any server even if it wanted to.", CreamColor)
        PrivacyCard("💾", "No Local File Storage", "Files are opened via Android's Storage Access Framework (SAF). Flipp does not store, copy, or cache your documents after conversion.", WhiteColor)
        PrivacyCard("🔒", "Client-Side Encryption", "When you protect a PDF with a password, AES-256 encryption happens locally on your processor. We never see your password.", SkyBlueColor)
        PrivacyCard("🚫", "No Analytics or Telemetry", "No Google Analytics, no Firebase, no crash reporting SDKs. Your app usage is completely private.", PinkColor)

        Spacer(modifier = Modifier.height(8.dp))

        BrutalistShadowBox(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MintColor,
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "The Open Source Promise",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = BlackColor
                )
                Text(
                    text = "Flipp is completely open source. You can audit every line of Kotlin code yourself to verify that no network requests are ever made.",
                    fontSize = 12.sp,
                    color = Color(0xFF222222),
                    lineHeight = 18.sp
                )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f)) {
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
