package com.lapakhunian.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.designsystem.components.PropertyListCard
import com.lapakhunian.app.core.util.formatRupiah
import com.lapakhunian.app.feature.settings.SettingsScreen
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class CompactLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun checkPrice(price: Long, fontScale: Float) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                LapakHunianTheme {
                    Box(Modifier.requiredWidth(320.dp).padding(horizontal = 16.dp)) {
                        PropertyListCard("Rumah keluarga", "Bekasi", price, null, onClick = {})
                    }
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(formatRupiah(price), useUnmergedTree = true)
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertFalse("Harga $price terpotong: size=${layout.size}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
    }

    @Test fun billionPriceFitsAt320dp() = checkPrice(1_400_000_000L, 1f)
    @Test fun priceFitsWithLargeFont() = checkPrice(3_250_000_000L, 2f)
    @Test fun largestSupportedPriceDoesNotClip() = checkPrice(Long.MAX_VALUE, 2f)
    @Test fun zeroPriceDoesNotClip() = checkPrice(0, 1f)

    private fun checkSettings(fontScale: Float) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                LapakHunianTheme {
                    Box(Modifier.requiredSize(320.dp, 420.dp)) {
                        SettingsScreen(onBack = {}, onOpenPrivacy = {}, onOpenTerms = {}, isLoggedIn = true)
                    }
                }
            }
        }
        compose.onNodeWithText("Versi Aplikasi", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(BuildConfig.VERSION_NAME, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun guestSettingsDoNotOfferAccountPasswordChanges() {
        compose.setContent {
            LapakHunianTheme {
                SettingsScreen(onBack = {}, onOpenPrivacy = {}, onOpenTerms = {}, isLoggedIn = false)
            }
        }
        compose.onNodeWithText("Ubah Password").assertDoesNotExist()
        compose.onNodeWithText("Kebijakan Privasi").assertIsDisplayed()
        compose.onNodeWithText("Versi Aplikasi").performScrollTo().assertIsDisplayed()
    }

    @Test fun settingsVersionReachableOnShortScreen() = checkSettings(1f)
    @Test fun settingsVersionReachableWithLargeFont() = checkSettings(2f)
}
