package com.lapakhunian.app

import android.content.Context
import com.lapakhunian.app.core.data.local.GuestFavoritesStore
import com.lapakhunian.app.core.data.local.PropertySnapshot
import com.lapakhunian.app.core.data.local.RecentlyViewedStore
import com.lapakhunian.app.core.data.local.toProperty
import com.lapakhunian.app.core.data.local.toSnapshot
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.PropertyCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class LocalPropertyStoresTest {

    private fun prefs(name: String) =
        RuntimeEnvironment.getApplication().getSharedPreferences(name, Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }

    private fun snapshot(id: String) = PropertySnapshot(
        id = id,
        title = "Rumah $id",
        slug = "rumah-$id",
        price = 500_000_000L,
        city = "Bekasi",
        certificate = "SHM",
        bedrooms = 2,
        bathrooms = 1,
        landArea = 90,
        buildingArea = 60,
        category = "house",
        image = "https://example.com/$id.webp",
    )

    @Test
    fun recentlyViewedOrdersNewestFirstAndDedupes() {
        val store = RecentlyViewedStore(prefs("recent-order"))
        store.record(snapshot("a"))
        store.record(snapshot("b"))
        store.record(snapshot("a"))
        assertEquals(listOf("a", "b"), store.items.value.map { it.id })
    }

    @Test
    fun recentlyViewedCapsAtTen() {
        val store = RecentlyViewedStore(prefs("recent-cap"))
        repeat(12) { store.record(snapshot("p$it")) }
        assertEquals(10, store.items.value.size)
        assertEquals("p11", store.items.value.first().id)
        assertFalse(store.items.value.any { it.id == "p0" })
    }

    @Test
    fun recentlyViewedSurvivesNewInstance() {
        val prefs = prefs("recent-persist")
        RecentlyViewedStore(prefs).record(snapshot("a"))
        val reopened = RecentlyViewedStore(prefs)
        assertEquals(listOf("a"), reopened.items.value.map { it.id })
    }

    @Test
    fun guestFavoritesToggleAddRemoveAndPersist() {
        val prefs = prefs("guest-fav")
        val store = GuestFavoritesStore(prefs)
        store.toggle(snapshot("a"))
        store.toggle(snapshot("b"))
        assertEquals(listOf("b", "a"), store.items.value.map { it.id })
        store.toggle(snapshot("a"))
        assertEquals(listOf("b"), store.items.value.map { it.id })
        store.remove("b")
        assertTrue(store.items.value.isEmpty())
        store.toggle(snapshot("c"))
        assertEquals(listOf("c"), GuestFavoritesStore(prefs).items.value.map { it.id })
    }

    @Test
    fun guestFavoritesReplaceAllKeepsOnlyGivenIds() {
        val store = GuestFavoritesStore(prefs("guest-replace"))
        store.toggle(snapshot("a"))
        store.toggle(snapshot("b"))
        store.replaceAll(listOf(snapshot("b")))
        assertEquals(listOf("b"), store.items.value.map { it.id })
        store.clear()
        assertTrue(store.items.value.isEmpty())
    }

    @Test
    fun snapshotRoundTripKeepsCardAndCompareFields() {
        val property = Property(
            id = "x1", title = "Rumah Uji", slug = "rumah-uji", description = null,
            price = 750_000_000L, address = null, city = "Depok", bedrooms = 3, bathrooms = 2,
            landArea = 120, buildingArea = 80, floors = 2, carport = 1, certificate = "SHM",
            electricity = null, furnishing = null, yearBuilt = null,
            category = PropertyCategory.HOUSE, purpose = "dijual",
            images = listOf("https://example.com/1.webp", "https://example.com/2.webp"),
            amenities = emptyList(), listedBy = null, contactName = null,
            isFeatured = false, isSold = false, viewCount = 0,
            latitude = null, longitude = null, agentName = null, agentRating = null, status = null,
        )
        val restored = property.toSnapshot().toProperty()
        assertEquals(property.id, restored.id)
        assertEquals(property.title, restored.title)
        assertEquals(property.price, restored.price)
        assertEquals(property.city, restored.city)
        assertEquals(property.certificate, restored.certificate)
        assertEquals(property.bedrooms, restored.bedrooms)
        assertEquals(property.landArea, restored.landArea)
        assertEquals(property.buildingArea, restored.buildingArea)
        assertEquals(PropertyCategory.HOUSE, restored.category)
        assertEquals(listOf("https://example.com/1.webp"), restored.images)
    }
}