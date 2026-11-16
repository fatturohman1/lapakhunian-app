package com.lapakhunian.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.ChatRepository
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.feature.dashboard.ListingWizardScreen
import com.lapakhunian.app.feature.dashboard.ListingWizardViewModel
import com.lapakhunian.app.feature.favorites.FavoritesScreen
import com.lapakhunian.app.feature.favorites.FavoritesViewModel
import com.lapakhunian.app.feature.profile.ProfileScreen
import com.lapakhunian.app.feature.profile.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class GuestAndDraftTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val session = mockk<SessionManager>()
    private val repo = mockk<PropertyRepository>()
    private val token = MutableStateFlow<String?>(null)

    private fun prepareSession() {
        every { session.tokenFlow } returns token
        every { session.userFlow } returns MutableStateFlow<UserDto?>(null)
    }

    private fun guestFavoritesStore() = com.lapakhunian.app.core.data.local.GuestFavoritesStore(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("test-guest-draft", android.content.Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() },
    )
    @After fun tearDown() { compose.runOnUiThread { store.clear() } }

    @Test fun guestCanOpenSettingsAndHelp() {
        prepareSession()
        lateinit var vm: ProfileViewModel
        compose.runOnUiThread {
            vm = ProfileViewModel(repo, mockk<ChatRepository>(), session).also { store.put("profile", it) }
        }
        var settings = false
        var help = false
        compose.setContent { LapakHunianTheme {
            ProfileScreen(
                onGoLogin = {}, onGoRegister = {}, onOpenOwnerDashboard = {}, onOpenAgentDashboard = {},
                onOpenAgentLeads = {}, onOpenWizard = {}, onOpenHistory = {}, onOpenNotifications = {}, onEditProfile = {},
                onOpenSettings = { settings = true }, onOpenHelp = { help = true }, viewModel = vm,
            )
        } }
        compose.onNodeWithText("Pengaturan").performScrollTo().performClick()
        compose.onNodeWithText("Bantuan").performScrollTo().performClick()
        assertTrue(settings)
        assertTrue(help)
    }

    @Test fun guestFavoritesHasWorkingLoginAction() {
        prepareSession()
        lateinit var vm: FavoritesViewModel
        compose.runOnUiThread { vm = FavoritesViewModel(repo, session, guestFavoritesStore()).also { store.put("favorites", it) } }
        var login = false
        compose.setContent { LapakHunianTheme { FavoritesScreen({}, { login = true }, vm) } }
        compose.waitUntil(5_000) { vm.state.value.needsLogin }
        compose.onNodeWithText("Masuk").performClick()
        assertTrue(login)
    }

    @Test fun favoriteErrorIsDistinctFromEmptyAndRetryRecovers() {
        prepareSession()
        token.value = "test-session"
        every { repo.favorites() } returns flow { throw IOException("offline") }
        lateinit var vm: FavoritesViewModel
        compose.runOnUiThread { vm = FavoritesViewModel(repo, session, guestFavoritesStore()).also { store.put("favorites", it) } }
        compose.setContent { LapakHunianTheme { FavoritesScreen({}, {}, vm) } }
        compose.waitUntil(5_000) { vm.state.value.error != null }
        compose.onNodeWithText("Gagal memuat favorit").assertIsDisplayed()
        every { repo.favorites() } returns flowOf(emptyList())
        compose.onNodeWithText("Coba lagi").performClick()
        compose.waitUntil(5_000) { vm.state.value.error == null && !vm.state.value.loading }
        compose.onNodeWithText("Belum ada favorit").assertIsDisplayed()
    }

    @Test fun wizardBackVisitsPreviousStepBeforeRequestingExit() {
        prepareSession()
        val saved = SavedStateHandle()
        lateinit var vm: ListingWizardViewModel
        compose.runOnUiThread {
            vm = ListingWizardViewModel(mockk<DashboardRepository>(), repo, session, saved).also { store.put("wizard", it) }
            vm.update { it.copy(title = "Draf rumah") }
            vm.setStep(3)
        }
        var exitRequested = false
        compose.setContent { LapakHunianTheme { ListingWizardScreen({ exitRequested = true }, {}, vm) } }
        compose.onNodeWithContentDescription("Kembali").performClick()
        assertEquals(2, vm.state.value.step)
        assertFalse(exitRequested)
        compose.onNodeWithContentDescription("Kembali").performClick()
        assertEquals(1, vm.state.value.step)
        compose.onNodeWithContentDescription("Kembali").performClick()
        assertTrue(exitRequested)
        assertEquals(true, saved.get<Boolean>(ListingWizardViewModel.DIRTY_KEY))
    }
}
