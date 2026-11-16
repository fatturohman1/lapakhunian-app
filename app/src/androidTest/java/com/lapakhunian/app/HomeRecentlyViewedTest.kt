package com.lapakhunian.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.PropertySnapshot
import com.lapakhunian.app.core.data.local.RecentlyViewedStore
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.feature.home.HomeScreen
import com.lapakhunian.app.feature.home.HomeViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeRecentlyViewedTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()

    @After fun tearDown() { compose.runOnUiThread { store.clear() } }

    @Test
    fun homeShowsRecentlyViewedSectionAndOpensDetail() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val recent = RecentlyViewedStore(
            context.getSharedPreferences("test-home-recent", Context.MODE_PRIVATE)
                .also { it.edit().clear().commit() },
        )
        recent.record(
            PropertySnapshot(
                id = "r1", title = "Rumah Terakhir Dilihat", slug = "rumah-terakhir",
                price = 450_000_000L, city = "Bekasi", certificate = "SHM",
                bedrooms = 2, bathrooms = 1, landArea = 90, buildingArea = 60,
                category = "house", image = null,
            ),
        )
        val session = mockk<SessionManager>()
        every { session.tokenFlow } returns MutableStateFlow<String?>(null)
        every { session.userFlow } returns MutableStateFlow<UserDto?>(null)
        val repo = mockk<PropertyRepository>(relaxed = true)
        val guest = com.lapakhunian.app.core.data.local.GuestFavoritesStore(
            context.getSharedPreferences("test-home-guest", Context.MODE_PRIVATE)
                .also { it.edit().clear().commit() },
        )
        lateinit var vm: HomeViewModel
        compose.runOnUiThread {
            vm = HomeViewModel(repo, session, recent, guest).also { store.put("home", it) }
        }
        var opened: String? = null
        compose.setContent {
            LapakHunianTheme {
                HomeScreen(
                    onOpenExplore = {}, onOpenDetail = { opened = it },
                    onOpenNotifications = {}, viewModel = vm,
                )
            }
        }
        compose.onNodeWithText("Terakhir dilihat").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rumah Terakhir Dilihat").performScrollTo().performClick()
        assertEquals("r1", opened)
    }
}