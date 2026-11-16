package com.lapakhunian.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.util.estimateInstallment
import com.lapakhunian.app.core.util.monthlyPayment
import com.lapakhunian.app.feature.compare.CompareViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CompareTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val session = mockk<SessionManager>()
    private val token = MutableStateFlow<String?>(null)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { session.tokenFlow } returns token
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun property(id: String, price: Long) = Property(
        id = id, title = "Rumah $id", slug = null, description = null, price = price,
        address = null, city = "Bekasi", bedrooms = 2, bathrooms = 1, landArea = 90,
        buildingArea = 60, floors = null, carport = null, certificate = "SHM",
        electricity = null, furnishing = null, yearBuilt = null, category = null,
        purpose = "dijual", images = emptyList(), amenities = emptyList(), listedBy = null,
        contactName = null, isFeatured = false, isSold = false, viewCount = 0,
        latitude = null, longitude = null, agentName = null, agentRating = null, status = null,
    )

    @Test fun compareSelectionCapsAtThreeAndTogglesOff() = runTest(dispatcher) {
        val vm = ExploreViewModel(mockk(relaxed = true), session, mockk<GuestFavoritesStore>(relaxed = true), mockk(relaxed = true))
            .also { store.put("explore", it) }
        runCurrent()
        (1..4).forEach { vm.toggleCompare(property("p$it", 100L * it)) }
        assertEquals(listOf("p1", "p2", "p3"), vm.compareSelection.value.map { it.id })
        vm.toggleCompare(property("p2", 200L))
        assertEquals(listOf("p1", "p3"), vm.compareSelection.value.map { it.id })
        vm.clearCompare()
        assertTrue(vm.compareSelection.value.isEmpty())
    }

    @Test fun compareViewModelLoadsAllRequestedProperties() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>()
        coEvery { repo.detail("a") } returns Result.success(property("a", 500_000_000L))
        coEvery { repo.detail("b") } returns Result.success(property("b", 700_000_000L))
        val vm = CompareViewModel(SavedStateHandle(mapOf("ids" to "a,b")), repo).also { store.put("compare", it) }
        runCurrent()
        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.id })
        assertNull(vm.state.value.error)
        assertTrue(!vm.state.value.loading)
    }

    @Test fun compareViewModelKeepsPartialResultsAndReportsFailure() = runTest(dispatcher) {
        val repo = mockk<PropertyRepository>()
        coEvery { repo.detail("a") } returns Result.success(property("a", 500_000_000L))
        coEvery { repo.detail("b") } returns Result.failure(IOException("offline"))
        val vm = CompareViewModel(SavedStateHandle(mapOf("ids" to "a,b")), repo).also { store.put("compare", it) }
        runCurrent()
        assertEquals(listOf("a"), vm.state.value.items.map { it.id })
        assertNotNull(vm.state.value.error)
    }

    @Test fun installmentEstimateUsesKprDefaults() {
        assertEquals(monthlyPayment(800_000_000L, 7.5, 15), estimateInstallment(1_000_000_000L))
        assertTrue(estimateInstallment(1_000_000_000L) > 0)
        assertEquals(0L, estimateInstallment(0L))
    }
}