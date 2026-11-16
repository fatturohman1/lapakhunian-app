package com.lapakhunian.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.feature.compare.CompareScreen
import com.lapakhunian.app.feature.compare.CompareViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.After
import org.junit.Rule
import org.junit.Test

class CompareScreenTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()

    @After fun tearDown() { compose.runOnUiThread { store.clear() } }

    private fun property(id: String, title: String, price: Long) = Property(
        id = id, title = title, slug = null, description = null, price = price,
        address = null, city = "Bekasi", bedrooms = 2, bathrooms = 1, landArea = 90,
        buildingArea = 60, floors = null, carport = null, certificate = "SHM",
        electricity = null, furnishing = null, yearBuilt = null, category = null,
        purpose = "dijual", images = emptyList(), amenities = emptyList(), listedBy = null,
        contactName = null, isFeatured = false, isSold = false, viewCount = 0,
        latitude = null, longitude = null, agentName = null, agentRating = null, status = null,
    )

    @Test
    fun compareShowsSpecsAndInstallmentForEachProperty() {
        val repo = mockk<PropertyRepository>()
        coEvery { repo.detail("a") } returns Result.success(property("a", "Rumah Murah Bekasi", 500_000_000L))
        coEvery { repo.detail("b") } returns Result.success(property("b", "Rumah Mahal Depok", 700_000_000L))
        lateinit var vm: CompareViewModel
        compose.runOnUiThread {
            vm = CompareViewModel(SavedStateHandle(mapOf("ids" to "a,b")), repo).also { store.put("compare", it) }
        }
        compose.setContent {
            LapakHunianTheme { CompareScreen(onBack = {}, onOpenDetail = {}, viewModel = vm) }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Rumah Murah Bekasi").assertIsDisplayed()
        compose.onNodeWithText("Rumah Mahal Depok").assertExists()
        compose.onAllNodesWithText("Cicilan/bulan*").assertCountEquals(2)
        compose.onAllNodesWithText("SHM").assertCountEquals(2)
        compose.onAllNodesWithText("90 m²").assertCountEquals(2)
    }
}