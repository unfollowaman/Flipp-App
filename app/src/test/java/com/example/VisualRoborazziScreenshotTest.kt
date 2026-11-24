package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FlippYellow
import com.example.ui.theme.MintColor
import com.example.ui.theme.PinkColor
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualRoborazziScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------
    // 5-STAGE LIFECYCLE CAPTURES (Phase 2 & 4 Core Primitives)
    // -------------------------------------------------------------

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureStage1_idle_standardPhone() {
        composeTestRule.setContent {
            FeaturePageScaffold(
                title = "PDF to Images",
                desc = "Convert PDF pages into high-quality PNG or JPG images on your device.",
                badgeLabel = "PDF → PNG",
                badgeColor = FlippYellow,
                onBack = {}
            ) {
                SingleFileDropZone(
                    onFileSelected = {},
                    prompt = "Drop your PDF here or"
                )
                FeatureHowItWorksCard(
                    step1 = "Upload your PDF to begin conversion",
                    step2 = "Select image format, quality, and range",
                    step3 = "Download individual images or all as ZIP"
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
    fun captureStage2_options_compactPhone() {
        composeTestRule.setContent {
            FeaturePageScaffold(
                title = "PDF to Images",
                desc = "Convert PDF pages into high-quality PNG or JPG images on your device.",
                badgeLabel = "PDF → PNG",
                badgeColor = FlippYellow,
                onBack = {}
            ) {
                SelectedFileCard(
                    fileName = "sample-document.pdf",
                    fileSizeStr = "5.62 MB",
                    pageCount = 3,
                    fileTypeBadge = "PDF",
                    badgeColor = FlippYellow,
                    onChangeFile = {}
                )

                CollapsibleSettingsCard(title = "Output Format & Resolution") {
                    NeobrutalistPillGroup(
                        title = "OUTPUT FORMAT",
                        options = listOf("PNG — Best quality", "JPG — Smaller size"),
                        selectedOption = "PNG — Best quality",
                        onOptionSelected = {},
                        labelProvider = { it },
                        selectedColor = FlippYellow
                    )

                    NeobrutalistPillGroup(
                        title = "RESOLUTION",
                        options = listOf("1x", "2x", "3x"),
                        selectedOption = "2x",
                        onOptionSelected = {},
                        labelProvider = { it },
                        selectedColor = FlippYellow
                    )
                }

                BrutalistButton(
                    text = "Convert All Pages →",
                    onClick = {},
                    backgroundColor = PinkColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureStage4_processing_standardPhone() {
        composeTestRule.setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProcessingStageCard(
                    progress = 0.65f,
                    statusText = "Converting page 2 of 3...",
                    stepDetails = "Rendering high-res bitmap (2x scale)"
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w800dp-h1280dp-xhdpi")
    fun captureStage5_completed_largeScreen() {
        composeTestRule.setContent {
            FeaturePageScaffold(
                title = "PDF to Images",
                desc = "Convert PDF pages into high-quality PNG or JPG images on your device.",
                badgeLabel = "PDF → PNG",
                badgeColor = FlippYellow,
                onBack = {}
            ) {
                CompletedHeroCard(
                    title = "✓ Conversion complete!",
                    subtitle = "3 pages rendered successfully.",
                    primaryButtonText = "Download All as ZIP 📦",
                    onPrimaryClick = {},
                    secondaryButtonText = "Convert Another ↺",
                    onSecondaryClick = {},
                    accentColor = MintColor
                )

                IndividualOutputCard(
                    title = "page-001.png",
                    bitmap = null,
                    onSave = {}
                )
                IndividualOutputCard(
                    title = "page-002.png",
                    bitmap = null,
                    onSave = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureErrorRecovery_standardPhone() {
        composeTestRule.setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ErrorCard(
                    errorMessage = "This PDF file is password protected. Please unlock it first.",
                    onDismissOrRetry = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    // -------------------------------------------------------------
    // FEATURE SCREEN LEVEL CAPTURES (Phase 3 Replicated Screens)
    // -------------------------------------------------------------

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureScreen_pdfToPng_standardPhone() {
        composeTestRule.setContent {
            PdfToPngScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureScreen_imagesToPdf_standardPhone() {
        composeTestRule.setContent {
            ImagesToPdfScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
    fun captureScreen_mergePdf_compactPhone() {
        composeTestRule.setContent {
            MergePdfScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureScreen_splitPdf_standardPhone() {
        composeTestRule.setContent {
            SplitPdfScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureScreen_protectPdf_standardPhone() {
        composeTestRule.setContent {
            ProtectPdfScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureScreen_addPageNumbers_standardPhone() {
        composeTestRule.setContent {
            AddPageNumScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w800dp-h1280dp-xhdpi")
    fun captureScreen_addWatermark_largeScreen() {
        composeTestRule.setContent {
            AddWatermarkScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
    fun captureScreen_textToPdf_standardPhone() {
        composeTestRule.setContent {
            TextToPdfScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
    fun captureScreen_pdfToText_compactPhone() {
        composeTestRule.setContent {
            PdfToTextScreen(onBack = {})
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
