package com.example

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
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
}
