package com.lapakhunian.app.core.util

/**
 * Peta link notifikasi (bentuk web, mis. "/dashboard/chat/abc") → tujuan native app.
 * Link tidak dikenal → null: notifikasi tetap ditandai dibaca tanpa navigasi.
 */
object NotificationLinkMapper {

    /** Tujuan dalam app. Route string dipakai AppRoot langsung untuk navigate. */
    sealed interface Destination {
        /** Percakapan chat spesifik (route "thread/{conversationId}"). */
        data class Thread(val conversationId: String) : Destination
        /** Tab chat (daftar percakapan). */
        object ChatTab : Destination
        /** Halaman agen: lead WA / leads / dispute / pembelian lead. */
        object AgentLeads : Destination
        /** Detail properti (route "detail/{propertyId}"). */
        data class Property(val propertyId: String) : Destination
        /** Tiket bantuan. */
        object Help : Destination
        /** Riwayat transaksi/langganan. */
        object History : Destination
    }

    private const val DASH = "/dashboard"

    fun resolve(link: String?): Destination? {
        val path = link?.trim()?.trimEnd('/')
            ?.substringBefore('?')   // query tidak pernah ada di link web, tapi jaga-jaga
            ?.substringBefore('#')
            ?: return null
        if (!path.startsWith(DASH)) return null
        val parts = path.removePrefix(DASH).split('/').filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        return when (parts[0]) {
            "chat" -> when {
                parts.size >= 2 -> Destination.Thread(parts[1])
                else -> Destination.ChatTab
            }
            "lead-wa", "leads", "lead-disputes", "lead-purchases" -> Destination.AgentLeads
            "properti" -> parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { Destination.Property(it) }
            "bantuan" -> Destination.Help
            "transaksi", "langganan", "subscriptions" -> Destination.History
            else -> null
        }
    }
}
