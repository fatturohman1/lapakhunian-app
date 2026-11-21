package com.lapakhunian.app

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.PropertySnapshot
import com.lapakhunian.app.core.data.local.RecentlyViewedStore
import com.lapakhunian.app.core.data.local.toProperty
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.feature.detail.DetailViewModel
import com.lapakhunian.app.feature.favorites.FavoritesViewModel
import com.lapakhunian.app.feature.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class GuestFavoritesTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val session = mockk<SessionManager>()
    private val token = MutableStateFlow<String?>(null)
    private val guest = GuestFavoritesStore(
        RuntimeEnvironment.getApplication().getSharedPreferences("test-guest-fav", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() },
    )
    private val recent = RecentlyViewedStore(
        RuntimeEnvironment.getApplication().getSharedPreferences("test-guest-recent", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() },
    )

    private val snapshot = PropertySnapshot(
        id = "a", title = "Rumah Tamu", slug = "rumah-tamu", price = 400_000_000L,
        city = "Bekasi", certificate = "SHM", bedrooms = 2, bathrooms = 1,
        landArea = 90, buildingArea = 60, category = "house", image = null,
    )
    private val property: Property get() = snapshot.toProperty()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { session.tokenFlow } returns token
        guest.clear()
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun guestSeesLocalFavoritesWithoutLogin() = runTest(dispatcher) {
        guest.toggle(snapshot)
        val repo = mockk<PropertyRepository>()
        val vm = FavoritesViewModel(repo, session, guest).also { store.put("fav", it) }
        runCurrent()
        assertFalse(vm.state.value.needsLogin)
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
    }

    @Test fun guestWithEmptyStoreStillGetsLoginPrompt() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>()
        val vm = FavoritesViewModel(repo, session, guest).also { store.put("fav", it) }
        runCurrent()
        assertTrue(vm.state.value.needsLogin)
    }

    @Test fun loginMergesGuestFavoritesThenClearsLocal() = runTest(dispatcher) {
        guest.toggle(snapshot)
        guest.toggle(snapshot.copy(id = "b", title = "Rumah B"))
        val repo = mockk<PropertyRepository>()
        coEvery { repo.setFavorite(any(), true) } returns Result.success(Unit)
        every { repo.favorites() } returns flowOf(listOf(property))
        val vm = FavoritesViewModel(repo, session, guest).also { store.put("fav", it) }
        runCurrent()
        token.value = "test-session"
        runCurrent()
        assertTrue(guest.items.value.isEmpty())
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
        assertNull(vm.state.value.error)
    }

    @Test fun partialMergeFailureKeepsFailedItemsLocally() = runTest(dispatcher) {
        guest.toggle(snapshot)
        guest.toggle(snapshot.copy(id = "b", title = "Rumah B"))
        val repo = mockk<PropertyRepository>()
        coEvery { repo.setFavorite("a", true) } returns Result.success(Unit)
        coEvery { repo.setFavorite("b", true) } returns Result.failure(IOException("offline"))
        every { repo.favorites() } returns flowOf(listOf(property))
        val vm = FavoritesViewModel(repo, session, guest).also { store.put("fav", it) }
        runCurrent()
        token.value = "test-session"
        runCurrent()
        assertEquals(listOf("b"), guest.items.value.map { it.id })
        assertNotNull(vm.state.value.error)
    }

    @Test fun guestUnfavoriteRemovesLocalItem() = runTest(dispatcher) {
        guest.toggle(snapshot)
        val repo = mockk<PropertyRepository>()
        val vm = FavoritesViewModel(repo, session, guest).also { store.put("fav", it) }
        runCurrent()
        vm.unfavorite("a")
        runCurrent()
        assertTrue(guest.items.value.isEmpty())
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test fun detailGuestToggleSavesLocally() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>(relaxed = true)
        coEvery { repo.detail("a") } returns Result.success(property)
        val vm = DetailViewModel(
            SavedStateHandle(mapOf("propertyId" to "a")), repo, session, recent, guest,
            com.lapakhunian.app.visitorTrackerStub(),
        ).also { store.put("detail", it) }
        runCurrent()
        assertFalse(vm.state.value.isFavorite)
        vm.toggleFavorite()
        runCurrent()
        assertTrue(vm.state.value.isFavorite)
        assertEquals(listOf("a"), guest.items.value.map { it.id })
        vm.toggleFavorite()
        runCurrent()
        assertFalse(vm.state.value.isFavorite)
        assertTrue(guest.items.value.isEmpty())
    }

    @Test fun homeGuestToggleSavesLocally() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>(relaxed = true)
        val vm = HomeViewModel(repo, session, recent, guest).also { store.put("home", it) }
        runCurrent()
        vm.toggleFavorite(property)
        runCurrent()
        assertEquals(listOf("a"), guest.items.value.map { it.id })
        assertTrue("a" in vm.favIds.value)
    }
}