package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextToPdfScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun textToPdfScreenDisplaysPlaceholderWhenInputIsEmpty() {
        composeTestRule.setContent {
            TextToPdfScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Type or paste your text here...")
            .assertIsDisplayed()
    }
}
