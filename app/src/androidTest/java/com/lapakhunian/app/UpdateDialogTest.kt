package com.lapakhunian.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.ui.UpdateAvailableDialog
import org.junit.Rule
import org.junit.Test

class UpdateDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dialogShowsReleaseNotesAndInstallGuide() {
        compose.setContent {
            LapakHunianTheme {
                UpdateAvailableDialog(
                    version = "1.0.0-beta.6",
                    notes = "• fix login\n• add zoom",
                    onDismiss = {},
                )
            }
        }
        compose.onNodeWithText("Yang baru:").assertIsDisplayed()
        compose.onNodeWithText("fix login", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cara pasang:").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("izinkan pemasangan", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Unduh Update").assertIsDisplayed()
    }

    @Test
    fun dialogWithoutNotesStillShowsInstallGuide() {
        compose.setContent {
            LapakHunianTheme {
                UpdateAvailableDialog(version = "1.0.0-beta.6", notes = "", onDismiss = {})
            }
        }
        compose.onNodeWithText("Yang baru:").assertDoesNotExist()
        compose.onNodeWithText("Cara pasang:").performScrollTo().assertIsDisplayed()
    }
}