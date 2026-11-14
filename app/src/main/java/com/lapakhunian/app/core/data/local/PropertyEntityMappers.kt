package com.lapakhunian.app.core.data.local

import com.lapakhunian.app.core.domain.model.ListingStatus
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.PropertyCategory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val listJson = Json { ignoreUnknownKeys = true }

fun Property.toEntity(): PropertyEntity = PropertyEntity(
    id = id,
    title = title,
    slug = slug,
    description = description,
    price = price,
    address = address,
    city = city,
    bedrooms = bedrooms,
    bathrooms = bathrooms,
    landArea = landArea,
    buildingArea = buildingArea,
    floors = floors,
    carport = carport,
    certificate = certificate,
    electricity = electricity,
    furnishing = furnishing,
    yearBuilt = yearBuilt,
    category = category?.apiValue,
    purpose = purpose,
    imagesJson = listJson.encodeToString(ListSerializer(String.serializer()), images),
    amenitiesJson = listJson.encodeToString(ListSerializer(String.serializer()), amenities),
    listedBy = listedBy,
    contactName = contactName,
    isFeatured = isFeatured,
    isSold = isSold,
    viewCount = viewCount,
    latitude = latitude,
    longitude = longitude,
    agentName = agentName,
    agentRating = agentRating,
    status = status?.name,
    cachedAt = System.currentTimeMillis(),
)

fun PropertyEntity.toDomain(): Property = Property(
    id = id,
    title = title,
    slug = slug,
    description = description,
    price = price,
    address = address,
    city = city,
    bedrooms = bedrooms,
    bathrooms = bathrooms,
    landArea = landArea,
    buildingArea = buildingArea,
    floors = floors,
    carport = carport,
    certificate = certificate,
    electricity = electricity,
    furnishing = furnishing,
    yearBuilt = yearBuilt,
    category = PropertyCategory.fromApi(category),
    purpose = purpose,
    images = listJson.decodeFromString(ListSerializer(String.serializer()), imagesJson),
    amenities = listJson.decodeFromString(ListSerializer(String.serializer()), amenitiesJson),
    listedBy = listedBy,
    contactName = contactName,
    isFeatured = isFeatured,
    isSold = isSold,
    viewCount = viewCount,
    latitude = latitude,
    longitude = longitude,
    agentName = agentName,
    agentRating = agentRating,
    status = ListingStatus.entries.firstOrNull { it.name == status },
)
