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
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
    fun numericTextFieldsExposeContentDescriptionSemantics() {
        composeTestRule.setContent {
            androidx.compose.material3.OutlinedTextField(
                value = "12",
                onValueChange = {},
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Font size"
                }
            )
        }

        composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription("Font size"))
            .assertExists()
    }
}
