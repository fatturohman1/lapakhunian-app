package com.lapakhunian.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.network.SearchFilters
import com.lapakhunian.app.feature.auth.AuthUiState
import com.lapakhunian.app.feature.auth.RegistrationConfirmation
import com.lapakhunian.app.feature.explore.FilterSheet
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class FilterAndVerificationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelDoesNotApplyTheSelectedCity() {
        var applied = SearchFilters()
        var dismissed = false
        compose.setContent { LapakHunianTheme { FilterSheet(applied, { applied = it }, { dismissed = true }) } }
        compose.onNodeWithText("Bekasi").performScrollTo().performClick()
        assertNull(applied.city)
        compose.onNodeWithText("Batal").performScrollTo().performClick()
        assertTrue(dismissed)
        assertNull(applied.city)
    }

    @Test fun applyCommitsTheCityAndResetRemainsPendingUntilApplied() {
        var applied = SearchFilters(city = "Bekasi", search = "rumah", category = "house")
        compose.setContent { LapakHunianTheme { FilterSheet(applied, { applied = it }, {}) } }
        compose.onNodeWithText("Reset").performScrollTo().performClick()
        assertEquals("Bekasi", applied.city)
        compose.onNodeWithText("Terapkan").performScrollTo().performClick()
        assertEquals(SearchFilters(), applied)
    }

    @Test fun invalidRangeAndOverflowCannotBeApplied() {
        var applied: SearchFilters? = null
        compose.setContent { LapakHunianTheme { FilterSheet(SearchFilters(minPrice = 200, maxPrice = 100), { applied = it }, {}) } }
        compose.onNodeWithText("Terapkan").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Harga minimum (Rp)").performScrollTo().performTextReplacement("999999999999999999999999")
        compose.onNodeWithText("Terapkan").performScrollTo().assertIsNotEnabled()
        assertNull(applied)
    }

    @Test fun verificationInstructionsRemainVisibleUntilContinue() {
        var continued = false
        var resend = false
        compose.setContent { LapakHunianTheme {
            RegistrationConfirmation(AuthUiState(registerSuccess = true, registeredEmail = "user@example.test"), { resend = true }, { continued = true })
        } }
        compose.onNodeWithText("Periksa email kamu").assertIsDisplayed()
        assertFalse(continued)
        compose.onNodeWithText("Kirim ulang verifikasi").performScrollTo().performClick()
        assertTrue(resend)
        compose.onNodeWithText("Ke halaman login").performScrollTo().performClick()
        assertTrue(continued)
    }
}
