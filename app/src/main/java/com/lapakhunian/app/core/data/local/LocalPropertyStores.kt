package com.lapakhunian.app.core.data.local

import android.content.Context
import android.content.SharedPreferences
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.PropertyCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Ringkasan properti untuk daftar lokal (riwayat & favorit tamu) — cukup untuk kartu dan pembanding. */
@Serializable
data class PropertySnapshot(
    val id: String,
    val title: String,
    val slug: String? = null,
    val price: Long,
    val city: String? = null,
    val certificate: String? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val landArea: Int? = null,
    val buildingArea: Int? = null,
    val category: String? = null,
    val image: String? = null,
)

fun Property.toSnapshot(): PropertySnapshot = PropertySnapshot(
    id = id,
    title = title,
    slug = slug,
    price = price,
    city = city,
    certificate = certificate,
    bedrooms = bedrooms,
    bathrooms = bathrooms,
    landArea = landArea,
    buildingArea = buildingArea,
    category = category?.apiValue,
    image = images.firstOrNull(),
)

fun PropertySnapshot.toProperty(): Property = Property(
    id = id,
    title = title,
    slug = slug,
    description = null,
    price = price,
    address = null,
    city = city,
    bedrooms = bedrooms,
    bathrooms = bathrooms,
    landArea = landArea,
    buildingArea = buildingArea,
    floors = null,
    carport = null,
    certificate = certificate,
    electricity = null,
    furnishing = null,
    yearBuilt = null,
    category = PropertyCategory.fromApi(category),
    purpose = null,
    images = listOfNotNull(image),
    amenities = emptyList(),
    listedBy = null,
    contactName = null,
    isFeatured = false,
    isSold = false,
    viewCount = 0,
    latitude = null,
    longitude = null,
    agentName = null,
    agentRating = null,
    status = null,
)

private val json = Json { ignoreUnknownKeys = true }
private val listSerializer = ListSerializer(PropertySnapshot.serializer())

private fun SharedPreferences.readSnapshots(key: String): List<PropertySnapshot> =
    runCatching { json.decodeFromString(listSerializer, getString(key, null).orEmpty()) }
        .getOrDefault(emptyList())

private fun SharedPreferences.writeSnapshots(key: String, items: List<PropertySnapshot>) {
    edit().putString(key, json.encodeToString(listSerializer, items)).apply()
}

/** Riwayat "Terakhir dilihat" — lokal saja, tanpa login, terbaru di depan, maks 10. */
@Singleton
class RecentlyViewedStore internal constructor(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences("lapak_recently_viewed", Context.MODE_PRIVATE),
    )

    private val _items = MutableStateFlow(prefs.readSnapshots(KEY))
    val items: StateFlow<List<PropertySnapshot>> = _items.asStateFlow()

    @Synchronized
    fun record(snapshot: PropertySnapshot) {
        val updated = (listOf(snapshot) + _items.value.filterNot { it.id == snapshot.id }).take(MAX)
        _items.value = updated
        prefs.writeSnapshots(KEY, updated)
    }

    private companion object {
        const val KEY = "items"
        const val MAX = 10
    }
}

/** Favorit sementara perangkat untuk tamu; digabung ke server setelah login. */
@Singleton
class GuestFavoritesStore internal constructor(private val prefs: SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences("lapak_guest_favorites", Context.MODE_PRIVATE),
    )

    private val _items = MutableStateFlow(prefs.readSnapshots(KEY))
    val items: StateFlow<List<PropertySnapshot>> = _items.asStateFlow()

    @Synchronized
    fun toggle(snapshot: PropertySnapshot) {
        val updated = if (_items.value.any { it.id == snapshot.id }) {
            _items.value.filterNot { it.id == snapshot.id }
        } else {
            listOf(snapshot) + _items.value
        }
        _items.value = updated
        prefs.writeSnapshots(KEY, updated)
    }

    @Synchronized
    fun remove(id: String) = replaceAll(_items.value.filterNot { it.id == id })

    @Synchronized
    fun replaceAll(items: List<PropertySnapshot>) {
        _items.value = items
        prefs.writeSnapshots(KEY, items)
    }

    @Synchronized
    fun clear() = replaceAll(emptyList())

    private companion object {
        const val KEY = "items"
    }
}