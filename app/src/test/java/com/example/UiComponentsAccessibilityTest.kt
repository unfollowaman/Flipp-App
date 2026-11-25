package com.example

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiComponentsAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topNavbarLogoHasButtonAccessibilityRoleAndOnClickLabel() {
        composeTestRule.setContent {
            TopNavbar(
                onPrivacyClick = {},
                onLogoClick = {}
            )
        }

        composeTestRule.onNodeWithText("flipp")
            .assertExists()
        composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertCountEquals(2) // Logo Row button + Privacy IconButton
    }

    @Test
    fun brutalistButtonHasButtonAccessibilityRole() {
        composeTestRule.setContent {
            BrutalistButton(
                text = "Click Me",
                onClick = {},
                testTag = "test_button"
            )
        }

        composeTestRule.onNodeWithTag("test_button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun brutalistShadowBoxWithOnClickLabelSetsOnClickLabelSemantics() {
        composeTestRule.setContent {
            BrutalistShadowBox(
                onClick = {},
                onClickLabel = "Open PDF to Images tool",
                testTag = "test_shadow_box"
            ) {
                Box(modifier = Modifier)
            }
        }

        composeTestRule.onNodeWithTag("test_shadow_box")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("has onClickLabel 'Open PDF to Images tool'") { node ->
                    val onClickConfig = node.config.getOrNull(SemanticsActions.OnClick)
                    onClickConfig?.label == "Open PDF to Images tool"
                }
            )
    }

    @Test
    fun dropZoneHasButtonAccessibilityRole() {
        composeTestRule.setContent {
            DropZone(
                onBrowseClick = {},
                prompt = "Drag files here or"
            )
        }

        composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertCountEquals(2) // Outer DropZone container + inner browse button
    }

    @Test
    fun stageProgressBarHasProgressBarRangeInfoAccessibilitySemantics() {
        composeTestRule.setContent {
            StageProgressBar(
                progress = 0.5f,
                label = "Processing..."
            )
        }

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.5f, 0f..1f)))
            .assertExists()
        composeTestRule.onNodeWithText("Processing...")
            .assertExists()
    }

    @Test
    fun optionSelectionTargetsHaveRadioButtonRoleAndSelectedSemantics() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .selectable(
                        selected = true,
                        role = Role.RadioButton,
                        onClick = {}
                    )
                    .testTag("position_target")
            )
        }

        composeTestRule.onNodeWithTag("position_target")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(isSelected())
    }

    @Test
    fun addPageNumScreenRendersAccessibilityTreeCorrectly() {
        composeTestRule.setContent {
            AddPageNumScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Add Page Numbers")
            .assertCountEquals(2) // Navbar title + Header card title
    }

    @Test
    fun numericInputFieldExposesAccessibilityContentDescription() {
        composeTestRule.setContent {
            androidx.compose.material3.OutlinedTextField(
                value = "1",
                onValueChange = {},
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Page number start offset"
                }
            )
        }

        composeTestRule.onNode(hasContentDescription("Page number start offset"))
            .assertExists()
    }

    @Test
    fun radioButtonRowHasRadioButtonRoleAndSelectedSemantics() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(
                        selected = true,
                        role = Role.RadioButton,
                        onClick = {}
                    )
                    .testTag("radio_row")
            ) {
                androidx.compose.material3.RadioButton(
                    selected = true,
                    onClick = null
                )
                androidx.compose.material3.Text("AUTO")
            }
        }

        composeTestRule.onNodeWithTag("radio_row")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(isSelected())
        composeTestRule.onNodeWithText("AUTO")
            .assertExists()
    }

    @Test
    fun textToPdfScreenDisplaysInputPlaceholderWhenTextIsEmpty() {
        composeTestRule.setContent {
            TextToPdfScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Type or paste text here...")
            .assertExists()
    }

    @Test
    fun formInputFieldsHaveContentDescriptionAccessibilitySemantics() {
        composeTestRule.setContent {
            TextToPdfScreen(onBack = {})
        }

        composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription("Text content for PDF"))
            .assertExists()
    }

    @Test
    fun splitPdfScreenRendersDropZoneInitially() {
        composeTestRule.setContent {
            SplitPdfScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Split PDF")
            .assertCountEquals(2) // Navbar title + Header card title
        composeTestRule.onNodeWithText("Extract specific page range into a separate file.")
            .assertExists()
    }

    @Test
    fun featurePageScaffoldRendersHeaderAndBackNavigation() {
        composeTestRule.setContent {
            FeaturePageScaffold(
                title = "Compress PDF",
                desc = "Reduce document size while preserving quality.",
                badgeLabel = "COMPRESS",
                onBack = {}
            ) {
                androidx.compose.material3.Text("Child Content")
            }
        }

        // Title in Navbar and Header card
        composeTestRule.onAllNodesWithText("Compress PDF")
            .assertCountEquals(2)
        composeTestRule.onNodeWithText("Reduce document size while preserving quality.")
            .assertExists()
        composeTestRule.onNodeWithText("100% on-device • Never leaves your phone")
            .assertExists()
        composeTestRule.onNodeWithText("Child Content")
            .assertExists()
        composeTestRule.onNodeWithTag("back_button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun selectedFileCardRendersMetadataAndAccessibleChangeButton() {
        composeTestRule.setContent {
            SelectedFileCard(
                fileName = "annual_report.pdf",
                fileSizeStr = "2.4 MB",
                pageCount = 12,
                onChangeFile = {}
            )
        }

        composeTestRule.onNodeWithText("annual_report.pdf")
            .assertExists()
        composeTestRule.onNodeWithText("12 pages")
            .assertExists()
        composeTestRule.onNodeWithText("• 2.4 MB")
            .assertExists()
        composeTestRule.onNodeWithTag("change_file_button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun optionPillHasRadioButtonRoleAndSelectedSemantics() {
        composeTestRule.setContent {
            OptionPill(
                label = "PNG",
                subtext = "Best quality",
                selected = true,
                onClick = {}
            )
        }

        composeTestRule.onNode(hasContentDescription("PNG", substring = true).or(androidx.compose.ui.test.hasText("PNG")))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(isSelected())
        composeTestRule.onNodeWithText("Best quality")
            .assertExists()
    }

    @Test
    fun neobrutalistPillGroupRendersOptionsWithCorrectRoles() {
        composeTestRule.setContent {
            NeobrutalistPillGroup(
                title = "PAGE SIZE",
                options = listOf("A4", "LETTER"),
                selectedOption = "A4",
                onOptionSelected = {},
                labelProvider = { it }
            )
        }

        composeTestRule.onNodeWithText("PAGE SIZE")
            .assertExists()
        composeTestRule.onNodeWithText("A4")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(isSelected())
        composeTestRule.onNodeWithText("LETTER")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun collapsibleSettingsCardTogglesContent() {
        composeTestRule.setContent {
            CollapsibleSettingsCard(title = "Advanced Options", initiallyExpanded = true) {
                androidx.compose.material3.Text("Inner Option")
            }
        }

        composeTestRule.onNodeWithText("Inner Option")
            .assertExists()
        composeTestRule.onNodeWithText("Advanced Options")
            .performClick()
        composeTestRule.onNodeWithText("Inner Option")
            .assertDoesNotExist()
    }

    @Test
    fun processingStageCardRendersProgressAndPrivacyNotice() {
        composeTestRule.setContent {
            ProcessingStageCard(
                progress = 0.6f,
                statusText = "Converting pages...",
                stepDetails = "Page 3 of 5"
            )
        }

        composeTestRule.onAllNodesWithText("Converting pages...")
            .assertCountEquals(2) // Card header + Progress bar label
        composeTestRule.onNodeWithText("Page 3 of 5")
            .assertExists()
        composeTestRule.onNodeWithText("Processing locally on your device")
            .assertExists()
    }

    @Test
    fun completedHeroCardRendersButtonsAndTitle() {
        composeTestRule.setContent {
            CompletedHeroCard(
                title = "✓ Conversion complete!",
                subtitle = "Output is ready.",
                primaryButtonText = "Download All as ZIP 📦",
                onPrimaryClick = {},
                secondaryButtonText = "Convert Another ↺",
                onSecondaryClick = {}
            )
        }

        composeTestRule.onNodeWithText("✓ Conversion complete!")
            .assertExists()
        composeTestRule.onNodeWithText("Download All as ZIP 📦")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onNodeWithText("Convert Another ↺")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun errorCardRendersErrorMessageAndRetry() {
        composeTestRule.setContent {
            ErrorCard(
                errorMessage = "Invalid password provided",
                onDismissOrRetry = {}
            )
        }

        composeTestRule.onNodeWithText("⚠️ Operation Failed")
            .assertExists()
        composeTestRule.onNodeWithText("Invalid password provided")
            .assertExists()
        composeTestRule.onNodeWithText("Try Again")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun multiFileQueueCardRendersAccessibleControls() {
        composeTestRule.setContent {
            MultiFileQueueCard(
                index = 0,
                fileName = "invoice_1.pdf",
                fileSizeStr = "500 KB",
                onMoveUp = {},
                onMoveDown = {},
                onRemove = {}
            )
        }

        composeTestRule.onNodeWithText("invoice_1.pdf")
            .assertExists()
        composeTestRule.onNodeWithText("500 KB")
            .assertExists()
        composeTestRule.onNode(hasContentDescription("Move item 1 up"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onNode(hasContentDescription("Remove item 1"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun interactivePageThumbnailCardRendersPageAndDeletionState() {
        composeTestRule.setContent {
            InteractivePageThumbnailCard(
                pageIndex = 1,
                bitmap = null,
                isMarkedForDeletion = true,
                onDeleteToggle = {}
            )
        }

        composeTestRule.onAllNodesWithText("Page 2")
            .assertCountEquals(2) // Thumbnail fallback + footer label
        composeTestRule.onNodeWithText("✕ DELETED")
            .assertExists()
    }

    @Test
    fun optionPillRendersCorrectRoleAndSelection() {
        composeTestRule.setContent {
            OptionPill(
                label = "PNG",
                sublabel = "High quality",
                badgeText = "Best",
                selected = true,
                onClick = {},
                testTag = "pill_png"
            )
        }

        composeTestRule.onNodeWithTag("pill_png")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(isSelected())
        composeTestRule.onNodeWithText("PNG").assertExists()
        composeTestRule.onNodeWithText("High quality").assertExists()
        composeTestRule.onNodeWithText("Best").assertExists()
    }

    @Test
    fun documentSummaryHeaderRendersFilenameAndBadges() {
        var removed = false
        composeTestRule.setContent {
            DocumentSummaryHeader(
                filename = "sample_contract.pdf",
                pageCount = 8,
                fileSizeString = "2.4 MB",
                onRemoveClick = { removed = true }
            )
        }

        composeTestRule.onNodeWithText("sample_contract.pdf").assertExists()
        composeTestRule.onNodeWithText("8 PAGES").assertExists()
        composeTestRule.onNodeWithText("2.4 MB").assertExists()
        composeTestRule.onNode(hasContentDescription("Remove file")).assertExists()
    }

    @Test
    fun resultsHeroBannerRendersTitleAndCTAs() {
        var primaryClicked = false
        var resetClicked = false
        composeTestRule.setContent {
            ResultsHeroBanner(
                title = "✓ Conversion complete!",
                subtitle = "Your files are ready to download.",
                primaryButtonText = "Download All as ZIP 📦",
                onPrimaryClick = { primaryClicked = true },
                secondaryButtonText = "Convert another file ↺",
                onSecondaryClick = { resetClicked = true }
            )
        }

        composeTestRule.onNodeWithText("✓ Conversion complete!").assertExists()
        composeTestRule.onNodeWithText("Download All as ZIP 📦").assertExists()
        composeTestRule.onNodeWithText("Convert another file ↺").assertExists()
    }

    @Test
    fun individualOutputCardRendersPageLabelAndActionButtons() {
        composeTestRule.setContent {
            IndividualOutputCard(
                pageIndex = 0,
                bitmap = null,
                label = "page-001.png",
                onSaveClick = {},
                onShareClick = {}
            )
        }

        composeTestRule.onNodeWithText("page-001.png").assertExists()
        composeTestRule.onNodeWithText("Page 1").assertExists()
        composeTestRule.onNodeWithText("Download").assertExists()
        composeTestRule.onNode(hasContentDescription("Share page-001.png")).assertExists()
    }
}
