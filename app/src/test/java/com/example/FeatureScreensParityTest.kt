package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeatureScreensParityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pdfToPngScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            PdfToPngScreen(onBack = {})
        }

        // Two headers: Navbar and Tool Identity card
        composeTestRule.onAllNodesWithText("PDF to Images")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Please select a PDF file.")
            .assertExists()
        composeTestRule.onNodeWithText("HOW IT WORKS")
            .assertExists()
    }

    @Test
    fun imagesToPdfScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            ImagesToPdfScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Images to PDF")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Combine multiple PNG/JPG into one PDF.")
            .assertExists()
        composeTestRule.onNodeWithText("HOW IT WORKS")
            .assertExists()
    }

    @Test
    fun mergePdfScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            MergePdfScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Merge PDFs")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Combine multiple PDFs into one.")
            .assertExists()
        composeTestRule.onNodeWithText("HOW IT WORKS")
            .assertExists()
    }

    @Test
    fun splitPdfScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            SplitPdfScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Split PDF")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Extract specific page range into a separate file.")
            .assertExists()
    }

    @Test
    fun protectPdfScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            ProtectPdfScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Protect PDF")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("AES password-encrypt a PDF entirely on-device.")
            .assertExists()
    }

    @Test
    fun addPageNumScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            AddPageNumScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Add Page Numbers")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Stamp page numbers with customizable position and offset start page.")
            .assertExists()
    }

    @Test
    fun addWatermarkScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            AddWatermarkScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Add Watermark")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Please select an image file to watermark.")
            .assertExists()
    }

    @Test
    fun textToPdfScreenRendersInputStateCorrectly() {
        composeTestRule.setContent {
            TextToPdfScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Text to PDF")
            .assertCountEquals(2)
        composeTestRule.onNode(hasContentDescription("Text content for PDF"))
            .assertExists()
        composeTestRule.onNodeWithText("Next: Configure Options ➔")
            .assertExists()
    }

    @Test
    fun pdfToTextScreenRendersIdleStateCorrectly() {
        composeTestRule.setContent {
            PdfToTextScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("PDF to Text")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Drag files here or")
            .assertExists()
    }
}
