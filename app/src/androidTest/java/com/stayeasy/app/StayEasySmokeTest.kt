package com.stayeasy.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke tests for core navigation. These are intentionally independent of
 * production credentials and a deployed API so CI can catch app-launch and UI regressions.
 */
@RunWith(AndroidJUnit4::class)
class StayEasySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun clickControlContaining(label: String) {
        composeRule.onNode(hasText(label, substring = true) and hasClickAction()).performScrollTo().performClick()
    }

    @Test
    fun launchesCustomerHome() {
        composeRule.onNodeWithText("StayEasy").assertIsDisplayed()
        composeRule.onNodeWithText("Find a place that").assertIsDisplayed()
        composeRule.onNodeWithText("Explore").assertIsDisplayed()
    }

    @Test
    fun customerCanOpenExploreAndSeeSearchControls() {
        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("Explore stays").assertIsDisplayed()
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
        composeRule.onNodeWithText("Map").assertIsDisplayed()
    }

    @Test
    fun customerCanOpenOwnerWorkspace() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        clickControlContaining("Are you a PG owner?")
        composeRule.onNodeWithText("Your business, at a glance").assertIsDisplayed()
        composeRule.onNodeWithText("Your properties", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun customerCanChangePaletteAndRestoreDefault() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        clickControlContaining("All tools & preferences")
        composeRule.onNodeWithText("Your StayEasy").assertIsDisplayed()
        clickControlContaining("Appearance & themes")
        composeRule.onNodeWithText("Appearance & settings").assertIsDisplayed()
        clickControlContaining("Ocean")
        composeRule.onNodeWithText("Appearance & settings").assertIsDisplayed()
        clickControlContaining("Midnight Cobalt")
        composeRule.onNodeWithText("Appearance & settings").assertIsDisplayed()
    }
}
