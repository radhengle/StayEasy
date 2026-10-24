package com.stayeasy.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private fun enterGuestPreview() {
        composeRule.onNodeWithText("Browse sample listings").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    /** Publishes a genuine emulator-display PNG to Downloads, outside app data cleanup. */
    private fun captureScreenshot(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        var destinationUri: android.net.Uri? = null
        try {
            val image = ByteArrayOutputStream().use { bytes ->
                assertTrue(
                    "Unable to encode emulator screenshot $name",
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                )
                bytes.toByteArray()
            }
            assertTrue("Emulator screenshot $name is unexpectedly small", image.size > 1024)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/stayeasy-screenshots"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw AssertionError("MediaStore could not create a Downloads entry for $name")
            destinationUri = uri
            resolver.openOutputStream(uri, "w").use { output ->
                assertTrue("Unable to open output stream for $name", output != null)
                output!!.write(image)
                output.flush()
            }
            val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            assertTrue("Unable to publish emulator screenshot $name", resolver.update(uri, published, null, null) == 1)
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null).use { cursor ->
                assertTrue("Screenshot metadata missing for $name", cursor != null && cursor.moveToFirst())
                assertTrue("Screenshot size missing for $name", cursor!!.getLong(0) > 1024)
            }
        } catch (error: Throwable) {
            destinationUri?.let { runCatching { resolver.delete(it, null, null) } }
            throw error
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun launchesLoginEntryWithActionsForBothRoles() {
        composeRule.onNodeWithText("Welcome home.").assertIsDisplayed()
        composeRule.onNodeWithText("Tenant / Customer").assertIsDisplayed()
        composeRule.onNodeWithText("Property Owner").assertIsDisplayed()
        composeRule.onAllNodesWithText("Login").assertCountEquals(2)
        composeRule.onAllNodesWithText("Register").assertCountEquals(2)
    }

    @Test
    fun launchesCustomerHomeFromExplicitPreviewAction() {
        enterGuestPreview()
        composeRule.onNodeWithText("StayEasy").assertIsDisplayed()
        composeRule.onNodeWithText("Find a place that").assertIsDisplayed()
        composeRule.onNodeWithText("Explore").assertIsDisplayed()
        captureScreenshot("customer-home")
    }

    @Test
    fun systemBackReturnsFromExploreToHomeInsteadOfClosingActivity() {
        enterGuestPreview()
        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("Explore stays").assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Find a place that").assertIsDisplayed()
    }

    @Test
    fun customerCanOpenExploreAndSeeSearchControls() {
        enterGuestPreview()
        composeRule.onNodeWithText("Explore").performClick()
        composeRule.onNodeWithText("Explore stays").assertIsDisplayed()
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
        composeRule.onNodeWithText("Map").assertIsDisplayed()
        composeRule.onNode(hasText("Monthly rent", substring = true)).assertIsDisplayed()
        captureScreenshot("explore-search")
    }

    @Test
    fun customerCanChooseTenantWorkspace() {
        enterGuestPreview()
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
        enterGuestPreview()
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Your profile").assertIsDisplayed()
        clickControlContaining("Are you a PG owner?")
        composeRule.onNodeWithText("Choose your workspace").assertIsDisplayed()
        composeRule.onNodeWithText("Preview mode:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Continue as Owner").performClick()
        composeRule.onNodeWithText("Your business, at a glance").assertIsDisplayed()
        composeRule.onNodeWithText("Your properties", useUnmergedTree = true).assertIsDisplayed()
        captureScreenshot("owner-workspace-preview")
    }

    @Test
    fun customerCanChangePaletteAndRestoreDefault() {
        enterGuestPreview()
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
        enterGuestPreview()
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
    fun ownerAccessReviewClientRejectsInvalidFiltersAndDecisionsBeforeNetwork() {
        val invalidFilter = runCatching {
            StayApiClient.ownerAccessRequests(
                "https://api.example.invalid", "test-token", "waiting"
            )
        }.exceptionOrNull()
        assertTrue("Only supported Owner access filters should be accepted", invalidFilter is IllegalArgumentException)

        val missingUser = runCatching {
            StayApiClient.decideOwnerAccessRequest(
                "https://api.example.invalid", "test-token", " ", "approve"
            )
        }.exceptionOrNull()
        assertTrue("An applicant must be selected before sending a decision", missingUser is IllegalArgumentException)

        val invalidDecision = runCatching {
            StayApiClient.decideOwnerAccessRequest(
                "https://api.example.invalid", "test-token", "applicant-id", "promote"
            )
        }.exceptionOrNull()
        assertTrue("Only explicit approve or reject decisions should be accepted", invalidDecision is IllegalArgumentException)
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

    @Test
    fun moveOutRequestRejectsMissingStayAndMalformedDateBeforeNetworkAccess() {
        val invalidStayId = runCatching {
            StayApiClient.requestMoveOut(
                "https://api.example.invalid", "test-token", 0L, "2026-12-15", "Moving"
            )
        }.exceptionOrNull()
        assertTrue("A valid server stay id is required", invalidStayId is IllegalArgumentException)

        val malformedDate = runCatching {
            StayApiClient.requestMoveOut(
                "https://api.example.invalid", "test-token", 1L, "2026-02-30", "Moving"
            )
        }.exceptionOrNull()
        assertTrue("Invalid calendar dates must be rejected before network access", malformedDate is IllegalArgumentException)

        val partialDate = runCatching {
            StayApiClient.requestMoveOut(
                "https://api.example.invalid", "test-token", 1L, "15 Dec 2026 trailing text", "Moving"
            )
        }.exceptionOrNull()
        assertTrue("Partial move-out dates must be rejected before network access", partialDate is IllegalArgumentException)
    }

    @Test
    fun ownerMoveOutReviewRejectsInvalidIdAndDecisionBeforeNetworkAccess() {
        val invalidId = runCatching {
            StayApiClient.reviewMoveOutRequest(
                "https://api.example.invalid", "test-token", 0L, "approve"
            )
        }.exceptionOrNull()
        assertTrue("A move-out request id must be positive", invalidId is IllegalArgumentException)

        val invalidDecision = runCatching {
            StayApiClient.reviewMoveOutRequest(
                "https://api.example.invalid", "test-token", 1L, "close"
            )
        }.exceptionOrNull()
        assertTrue("Only approve/reject decisions are accepted", invalidDecision is IllegalArgumentException)
    }

    @Test
    fun stayLifecycleRequiresNoticeAndMoveOutDatesBeforeNetworkAccess() {
        val missingNotice = runCatching {
            StayApiClient.updateOwnerStay(
                "https://api.example.invalid", "test-token", 1L, "notice_given"
            )
        }.exceptionOrNull()
        assertTrue("Notice status must require a notice date", missingNotice is IllegalArgumentException)

        val missingMoveOut = runCatching {
            StayApiClient.updateOwnerStay(
                "https://api.example.invalid", "test-token", 1L, "completed"
            )
        }.exceptionOrNull()
        assertTrue("Completed status must require a move-out date", missingMoveOut is IllegalArgumentException)

        val malformedNotice = runCatching {
            StayApiClient.updateOwnerStay(
                "https://api.example.invalid", "test-token", 1L, "notice_given",
                noticeDate = "1 Nov 2026 trailing text"
            )
        }.exceptionOrNull()
        assertTrue("Partial notice-date parses must be rejected", malformedNotice is IllegalArgumentException)

        val malformedMoveOut = runCatching {
            StayApiClient.updateOwnerStay(
                "https://api.example.invalid", "test-token", 1L, "completed",
                moveOutDate = "2026-11-31"
            )
        }.exceptionOrNull()
        assertTrue("Invalid calendar dates must be rejected", malformedMoveOut is IllegalArgumentException)
    }
}
