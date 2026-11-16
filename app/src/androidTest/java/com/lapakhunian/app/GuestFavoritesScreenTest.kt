package com.lapakhunian.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.PropertySnapshot
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.feature.favorites.FavoritesScreen
import com.lapakhunian.app.feature.favorites.FavoritesViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuestFavoritesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()

    @After fun tearDown() { compose.runOnUiThread { store.clear() } }

    private fun guestStore() = GuestFavoritesStore(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("test-guest-fav-screen", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() },
    )

    private fun snapshot(id: String, title: String) = PropertySnapshot(
        id = id, title = title, slug = null, price = 400_000_000L,
        city = "Bekasi", certificate = "SHM", bedrooms = 2, bathrooms = 1,
        landArea = 90, buildingArea = 60, category = "house", image = null,
    )

    @Test
    fun guestWithLocalFavoritesSeesBannerCardsAndLoginCta() {
        val guest = guestStore()
        guest.toggle(snapshot("a", "Rumah Pilihan Tamu"))
        val session = mockk<SessionManager>()
        every { session.tokenFlow } returns MutableStateFlow<String?>(null)
        every { session.userFlow } returns MutableStateFlow<UserDto?>(null)
        lateinit var vm: FavoritesViewModel
        compose.runOnUiThread {
            vm = FavoritesViewModel(mockk<PropertyRepository>(), session, guest).also { store.put("fav", it) }
        }
        var login = false
        compose.setContent {
            LapakHunianTheme { FavoritesScreen({}, { login = true }, vm) }
        }
        compose.onNodeWithText("Favorit sementara").assertIsDisplayed()
        compose.onNodeWithText("Rumah Pilihan Tamu").assertIsDisplayed()
        compose.onNodeWithText("Masuk").performClick()
        assertTrue(login)
    }

    @Test
    fun guestHeartRemovesLocalFavorite() {
        val guest = guestStore()
        guest.toggle(snapshot("a", "Rumah Pilihan Tamu"))
        val session = mockk<SessionManager>()
        every { session.tokenFlow } returns MutableStateFlow<String?>(null)
        every { session.userFlow } returns MutableStateFlow<UserDto?>(null)
        lateinit var vm: FavoritesViewModel
        compose.runOnUiThread {
            vm = FavoritesViewModel(mockk<PropertyRepository>(), session, guest).also { store.put("fav", it) }
        }
        compose.setContent {
            LapakHunianTheme { FavoritesScreen({}, {}, vm) }
        }
        compose.onNodeWithContentDescription("Hapus dari favorit").performClick()
        compose.waitForIdle()
        assertTrue(guest.items.value.isEmpty())
        compose.onNodeWithText("Masuk dulu ya").assertIsDisplayed()
    }
}