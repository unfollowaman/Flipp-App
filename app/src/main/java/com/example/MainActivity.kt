package com.example

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
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
                .background(color, CircleShape)
                .border(1.5.dp, BlackColor, CircleShape)
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
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Privacy Hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamColor)
                .bottomBorder(2.dp, BlackColor)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .background(MintColor, RoundedCornerShape(99.dp))
                    .border(2.dp, BlackColor, RoundedCornerShape(99.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "✦ PRIVACY POLICY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackColor
                )
            }
            Text(
                text = "Zero Data Collection.",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = BlackColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "We built this app on a simple premise: your documents are none of our business.",
                fontSize = 13.sp,
                color = Color(0xFF444444),
                lineHeight = 19.5.sp
            )
        }

        // Privacy Cards Grid/List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrivacyCard(
                icon = "🔒",
                title = "100% On-Device Processing",
                body = "Every PDF conversion, image extraction, and merge happens entirely within your device's memory. No data is sent to our servers — because we don't have any.",
                bgColor = YellowColor
            )

            PrivacyCard(
                icon = "✈️",
                title = "No Internet Required",
                body = "This app works completely offline. You can use it in airplane mode, in a basement, or anywhere without a network connection. It has no network permissions in its manifest.",
                bgColor = SkyBlueColor
            )

            PrivacyCard(
                icon = "👤",
                title = "No Accounts, No Tracking",
                body = "No sign-up. No login. No analytics SDKs. No crash reporters sending stack traces to the cloud. No cookies. We don't know who you are, and we like it that way.",
                bgColor = PinkColor
            )

            PrivacyCard(
                icon = "🗑️",
                title = "Ephemeral Storage",
                body = "Files you open are processed in memory. Converted outputs are written only where you choose to save them via the system file picker. We store nothing permanently.",
                bgColor = MintColor
            )

            // Local Guarantee Banner
            BrutalistShadowBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
