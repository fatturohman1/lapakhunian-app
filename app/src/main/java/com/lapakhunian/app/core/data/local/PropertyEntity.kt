package com.lapakhunian.app.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache offline listing properti (Home fallback, favorit nanti). */
@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey val id: String,
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
    val category: String?,
    val purpose: String?,
    val imagesJson: String,
    val amenitiesJson: String,
    val listedBy: String?,
    val contactName: String?,
    val isFeatured: Boolean,
    val isSold: Boolean,
    val viewCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val agentName: String?,
    val agentRating: Double?,
    val status: String?,
    val cachedAt: Long,
)
