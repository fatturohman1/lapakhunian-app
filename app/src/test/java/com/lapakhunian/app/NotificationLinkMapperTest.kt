package com.lapakhunian.app

import com.lapakhunian.app.core.util.NotificationLinkMapper
import com.lapakhunian.app.core.util.NotificationLinkMapper.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Semua pola link yang ditulis server (hously) untuk Notification.link. */
class NotificationLinkMapperTest {

    @Test fun `link chat dengan conversationId menuju thread`() {
        assertEquals(
            Destination.Thread("abc-123"),
            NotificationLinkMapper.resolve("/dashboard/chat/abc-123"),
        )
    }

    @Test fun `link chat tanpa id menuju tab chat`() {
        assertEquals(Destination.ChatTab, NotificationLinkMapper.resolve("/dashboard/chat"))
    }

    @Test fun `lead wa leads dispute dan pembelian lead menuju halaman agen`() {
        for (link in listOf(
            "/dashboard/lead-wa",
            "/dashboard/leads",
            "/dashboard/lead-disputes",
            "/dashboard/lead-purchases",
        )) {
            assertEquals(link, Destination.AgentLeads, NotificationLinkMapper.resolve(link))
        }
    }

    @Test fun `link properti dengan id menuju detail`() {
        assertEquals(
            Destination.Property("prop-9"),
            NotificationLinkMapper.resolve("/dashboard/properti/prop-9"),
        )
    }

    @Test fun `properti tanpa id dan dashboard tanpa segmen tidak dinavigasi`() {
        assertNull(NotificationLinkMapper.resolve("/dashboard/properti"))
        assertNull(NotificationLinkMapper.resolve("/dashboard"))
    }

    @Test fun `bantuan dan transaksi menuju help dan history`() {
        assertEquals(Destination.Help, NotificationLinkMapper.resolve("/dashboard/bantuan"))
        assertEquals(Destination.Help, NotificationLinkMapper.resolve("/dashboard/bantuan/tiket-1"))
        assertEquals(Destination.History, NotificationLinkMapper.resolve("/dashboard/transaksi"))
        assertEquals(Destination.History, NotificationLinkMapper.resolve("/dashboard/langganan"))
        assertEquals(Destination.History, NotificationLinkMapper.resolve("/dashboard/subscriptions"))
    }

    @Test fun `link kosong atau bukan dashboard ditolak`() {
        assertNull(NotificationLinkMapper.resolve(null))
        assertNull(NotificationLinkMapper.resolve(""))
        assertNull(NotificationLinkMapper.resolve("/profil"))
        assertNull(NotificationLinkMapper.resolve("https://lapakhunian.com/dashboard/chat"))
    }

    @Test fun `trailing slash dan spasi tetap dikenali`() {
        assertEquals(Destination.ChatTab, NotificationLinkMapper.resolve("/dashboard/chat/ "))
        assertEquals(Destination.Help, NotificationLinkMapper.resolve("/dashboard/bantuan/"))
    }

    @Test fun `query dan fragment dibuang sebelum mapping`() {
        assertEquals(Destination.ChatTab, NotificationLinkMapper.resolve("/dashboard/chat?x=1"))
        assertEquals(
            Destination.Thread("abc-123"),
            NotificationLinkMapper.resolve("/dashboard/chat/abc-123?x=1#frag"),
        )
    }
}
