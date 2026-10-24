package com.stayeasy.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun systemBackReturnsFromExploreToHomeInsteadOfClosingActivity() {
        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("Explore stays").assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Find a place that").assertIsDisplayed()
    }

    @Test
    fun customerCanOpenExploreAndSeeSearchControls() {
        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("Explore stays").assertIsDisplayed()
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
        composeRule.onNodeWithText("Map").assertIsDisplayed()
        composeRule.onNode(hasText("Monthly rent", substring = true)).assertIsDisplayed()
    }

    @Test
    fun customerCanChooseTenantWorkspace() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        clickControlContaining("Are you a PG owner?")
        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
        composeRule.onNodeWithText("I'm looking for a place").assertIsDisplayed()
        composeRule.onNodeWithText("I have a property to rent").assertIsDisplayed()
        composeRule.onNodeWithText("Continue as Tenant").performClick()
        composeRule.onNodeWithText("Find a place that").assertIsDisplayed()
    }

    @Test
    fun customerCanChooseOwnerWorkspace() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        clickControlContaining("Are you a PG owner?")
        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
        composeRule.onNodeWithText("Preview mode:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Continue as Owner").performClick()
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

    @Test
    fun customerCanOpenAccountAuthenticationFromProfile() {
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        clickControlContaining("Sign in / Create account")
        composeRule.onNodeWithText("StayEasy account").assertIsDisplayed()
        composeRule.onNodeWithText("Connect your StayEasy API first").assertIsDisplayed()
        composeRule.onNodeWithText("Configure API connection").assertIsDisplayed()
    }

    @Test
    fun sessionTokenIsBoundToItsApiOrigin() {
        StayApiClient.clearSessionToken()
        try {
            StayApiClient.setSessionToken("test-token", "https://api.example.com/v1")
            assertEquals("test-token", StayApiClient.currentSessionToken("https://api.example.com/v2"))
            assertNull(StayApiClient.currentSessionToken("https://other.example.com/v1"))
            assertNull(StayApiClient.currentSessionToken("http://api.example.com/v1"))
        } finally {
            StayApiClient.clearSessionToken()
        }
    }

    @Test
    fun malformedMoveInDateIsRejectedBeforeSendingARequest() {
        val error = runCatching {
            StayApiClient.createEnquiry(
                "https://api.example.invalid",
                "test-token",
                1L,
                "Private room",
                "Test enquiry",
                "15 Nov 2026 trailing text"
            )
        }.exceptionOrNull()
        assertTrue("A partial date parse must be rejected before network access", error is IllegalArgumentException)
    }
}
