package com.stayeasy.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Are you a PG owner?", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Your business, at a glance").assertIsDisplayed()
        composeRule.onNodeWithText("Your properties", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun customerCanChangePaletteAndRestoreDefault() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        composeRule.onNodeWithText("All tools & preferences", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Your StayEasy").assertIsDisplayed()
        composeRule.onNodeWithText("Appearance & themes", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Appearance & settings").assertIsDisplayed()
        composeRule.onNodeWithText("Ocean", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Appearance & settings").assertIsDisplayed()
        composeRule.onNodeWithText("Midnight Cobalt", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Appearance & settings").assertIsDisplayed()
    }
}
