package com.lapakhunian.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.PropertyDao
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.GreenBright
import com.lapakhunian.app.core.designsystem.GreenDeep
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.core.network.dto.ApiEnvelope
import com.lapakhunian.app.core.notifications.NotificationPrefs
import com.lapakhunian.app.feature.dashboard.ListingWizardViewModel
import com.lapakhunian.app.feature.favorites.FavoritesViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class P2RegressionTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val session = mockk<SessionManager>()
    private val token = MutableStateFlow<String?>("test-session")

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { session.tokenFlow } returns token
        coEvery { session.token() } answers { token.value }
    }
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun `favorite API errors are not an empty successful list`() = runTest(dispatcher) {
        val api = mockk<PropertyApi>()
        coEvery { api.listFavorites() } returns ApiEnvelope(success = false, error = "Koneksi gagal")
        val repository = PropertyRepository(api, mockk<PropertyDao>(relaxed = true), session, mockk<NotificationPrefs>())
        val result = runCatching { repository.favorites().first() }
        assertTrue("Kegagalan API harus diteruskan", result.isFailure)
    }

    @Test fun `favorites reload when a guest logs in`() = runTest(dispatcher) {
        token.value = null
        val repo = mockk<PropertyRepository>()
        every { repo.favorites() } returns flowOf(emptyList())
        val vm = FavoritesViewModel(repo, session, com.lapakhunian.app.core.data.local.GuestFavoritesStore(io.mockk.mockk<android.content.SharedPreferences>(relaxed = true))).also { store.put("favorites", it) }
        runCurrent()
        assertTrue(vm.state.value.needsLogin)
        token.value = "test-session"
        runCurrent()
        assertFalse(vm.state.value.needsLogin)
        assertFalse(vm.state.value.loading)
    }

    @Test fun `wizard restores fields and step from saved state`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val repo = mockk<DashboardRepository>()
        val propertyRepo = mockk<PropertyRepository>()
        val first = ListingWizardViewModel(repo, propertyRepo, session, saved).also { store.put("first", it) }
        first.update { it.copy(title = "Rumah keluarga", price = "1400000000", amenities = setOf("AC"), uploadedPhotos = listOf("/uploads/test.webp")) }
        first.setStep(3)
        runCurrent()
        val restored = ListingWizardViewModel(repo, propertyRepo, session, saved).also { store.put("restored", it) }
        runCurrent()
        assertEquals(true, saved.get<Boolean>(ListingWizardViewModel.DIRTY_KEY))
        assertEquals("Rumah keluarga", restored.state.value.title)
        assertEquals("1400000000", restored.state.value.price)
        assertEquals(3, restored.state.value.step)
        assertEquals(listOf("/uploads/test.webp"), restored.state.value.uploadedPhotos)
        assertFalse(restored.state.value.uploading)
        assertFalse(restored.state.value.submitting)
    }

    @Test fun `gradient supports small white text including secondary labels`() {
        for (background in listOf(GreenBright, GreenDeep)) {
            val foreground = Color.White.copy(alpha = 0.85f).compositeOver(background)
            val contrast = (foreground.luminance() + 0.05f) / (background.luminance() + 0.05f)
            assertTrue("Kontras teks $contrast harus minimal 4.5", contrast >= 4.5f)
        }
    }
}
