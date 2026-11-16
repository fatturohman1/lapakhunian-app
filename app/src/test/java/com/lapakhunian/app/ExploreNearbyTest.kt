package com.lapakhunian.app

import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.location.DeviceLocator
import com.lapakhunian.app.feature.explore.ExploreViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreNearbyTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val session = mockk<SessionManager>()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { session.tokenFlow } returns MutableStateFlow<String?>(null)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun property(id: String, lat: Double?, lon: Double?) = Property(
        id = id, title = "Rumah $id", slug = null, description = null, price = 100L,
        address = null, city = "Bekasi", bedrooms = 2, bathrooms = 1, landArea = 90,
        buildingArea = 60, floors = null, carport = null, certificate = "SHM",
        electricity = null, furnishing = null, yearBuilt = null, category = null,
        purpose = "dijual", images = emptyList(), amenities = emptyList(), listedBy = null,
        contactName = null, isFeatured = false, isSold = false, viewCount = 0,
        latitude = lat, longitude = lon, agentName = null, agentRating = null, status = null,
    )

    private fun viewModel(repo: PropertyRepository, locator: DeviceLocator) =
        ExploreViewModel(repo, session, mockk<GuestFavoritesStore>(relaxed = true), locator)
            .also { store.put("explore", it) }

    @Test fun `nearby sorts by distance and puts listings without coordinates last`() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>()
        coEvery { repo.nearbyCandidates(any()) } returns Result.success(
            listOf(
                property("far", -6.9, 107.6),
                property("none", null, null),
                property("near", -6.25, 106.85),
            ),
        )
        val locator = mockk<DeviceLocator>()
        coEvery { locator.lastKnown() } returns (-6.2 to 106.8)
        val vm = viewModel(repo, locator)
        runCurrent()
        vm.toggleNearby()
        runCurrent()
        assertEquals(listOf("near", "far", "none"), vm.nearbyItems.value?.map { it.id })
        vm.toggleNearby()
        assertNull(vm.nearbyItems.value)
    }

    @Test fun `missing device location reports error and keeps normal mode`() = runTest(dispatcher) {
        val locator = mockk<DeviceLocator>()
        coEvery { locator.lastKnown() } returns null
        val vm = viewModel(mockk(relaxed = true), locator)
        runCurrent()
        vm.toggleNearby()
        runCurrent()
        assertNull(vm.nearbyItems.value)
        assertNotNull(vm.nearbyError.value)
    }

    @Test fun `network failure keeps normal mode and reports error`() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>()
        coEvery { repo.nearbyCandidates(any()) } returns Result.failure(IOException("offline"))
        val locator = mockk<DeviceLocator>()
        coEvery { locator.lastKnown() } returns (-6.2 to 106.8)
        val vm = viewModel(repo, locator)
        runCurrent()
        vm.toggleNearby()
        runCurrent()
        assertNull(vm.nearbyItems.value)
        assertNotNull(vm.nearbyError.value)
    }
}