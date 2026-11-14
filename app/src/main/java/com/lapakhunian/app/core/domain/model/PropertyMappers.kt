package com.lapakhunian.app.core.domain.model

import com.lapakhunian.app.core.network.dto.PropertyDto
import com.lapakhunian.app.core.util.ApiConfig

fun PropertyDto.toDomain(): Property = Property(
    id = id ?: "",
    title = title ?: "",
    slug = slug,
    description = description,
    price = price ?: 0L,
    address = address,
    city = city,
    bedrooms = bedrooms,
    bathrooms = bathrooms,
    landArea = squareFeet,
    buildingArea = buildingArea,
    floors = floors,
    carport = carport,
    certificate = certificate,
    electricity = electricity,
    furnishing = furnishing,
    yearBuilt = yearBuilt,
    category = PropertyCategory.fromApi(category),
    purpose = purpose,
    images = images.mapNotNull { ApiConfig.absoluteUrl(it) },
    amenities = amenities,
    listedBy = listedBy,
    contactName = contactName,
    isFeatured = isFeatured ?: false,
    isSold = isSold ?: false,
    viewCount = viewCount ?: 0,
    latitude = latitude?.takeIf { it != 0.0 },
    longitude = longitude?.takeIf { it != 0.0 },
    agentName = agentName ?: contactName,
    agentRating = agentRating ?: rating,
    status = ListingStatus.entries.firstOrNull { it.name == status },
)
