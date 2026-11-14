package com.lapakhunian.app.core.domain.model

/**
 * Status listing — dari enum backend PropertyStatus + state turunan web
 * (isActive/isSold/isDeleted/expiry) yang dihitung di UI.
 */
enum class ListingStatus(val label: String) {
    PENDING("Menunggu Review"),
    APPROVED("Tayang"),
    REJECTED("Ditolak"),
    EXPIRED("Kedaluwarsa"),
    SOLD("Terjual"),
    PENDING_PAYMENT("Menunggu Bayar"),
}
