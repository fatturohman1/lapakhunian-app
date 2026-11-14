package com.lapakhunian.app.core.domain.model

enum class PropertyCategory(val apiValue: String, val label: String) {
    HOUSE("house", "Rumah"),
    APARTMENT("apartment", "Apartemen"),
    OFFICE("office", "Office"),
    LAND("land", "Tanah"),
    COMMERCIAL("commercial", "Komersial"),
    TOWNHOME("townhome", "Townhouse");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value?.lowercase() }
    }
}

data class Property(
    val id: String,
    val title: String,
    val slug: String?,
    val description: String?,
    val price: Long,
    val address: String?,
    val city: String?,
    val bedrooms: Int?,
    val bathrooms: Int?,
    val landArea: Int?,
    val buildingArea: Int?,
    val floors: Int?,
    val carport: Int?,
    val certificate: String?,
    val electricity: String?,
    val furnishing: String?,
    val yearBuilt: Int?,
    val category: PropertyCategory?,
    val purpose: String?,
    /** URL gambar absolut (base + /uploads/...). */
    val images: List<String>,
    val amenities: List<String>,
    val listedBy: String?,
    val contactName: String?,
    val isFeatured: Boolean,
    val isSold: Boolean,
    val viewCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val agentName: String?,
    val agentRating: Double?,
    val status: ListingStatus?,
)
