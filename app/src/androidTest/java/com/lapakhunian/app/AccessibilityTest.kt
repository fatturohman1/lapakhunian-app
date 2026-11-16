package com.lapakhunian.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorCodepoints
import com.lapakhunian.app.core.designsystem.components.FilterChip
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filterChipsExposeSelection() {
        compose.setContent { LapakHunianTheme {
            Column {
                FilterChip("Dijual", selected = true, onClick = {})
                FilterChip("Disewa", selected = false, onClick = {})
            }
        } }
        compose.onNodeWithText("Dijual").assertIsSelected()
        compose.onNodeWithText("Disewa").assertIsNotSelected()
    }

    @Test fun glyphTextIsNotExposedToAccessibility() {
        compose.setContent { LapakHunianTheme {
            Column {
                PhIcon(AppIcons.Search)
                PhIcon(AppIcons.Favorite, contentDescription = "Tambah ke favorit")
            }
        } }
        compose.onNodeWithContentDescription("Tambah ke favorit").assertExists()
        compose.onAllNodesWithText(requireNotNull(PhosphorCodepoints.REGULAR[AppIcons.Search]), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText(requireNotNull(PhosphorCodepoints.REGULAR[AppIcons.Favorite]), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun primaryButtonTextMeetsContrast() {
        var contrast = 0f
        compose.setContent { LapakHunianTheme {
            contrast = (Color.White.luminance() + 0.05f) / (MaterialTheme.colorScheme.primary.luminance() + 0.05f)
        } }
        compose.waitForIdle()
        assertTrue("Kontras tombol $contrast harus minimal 4.5", contrast >= 4.5f)
    }
}
