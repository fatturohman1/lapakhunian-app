package com.lapakhunian.app

import com.lapakhunian.app.core.network.dto.MessageDto
import com.lapakhunian.app.core.network.dto.MessagesEnvelope
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regresi read receipt ceklis 1/2: field `read` opsional di payload server.
 * Server baru mengirim `read`; server lama tidak — keduanya wajib parse sukses.
 */
class MessageReceiptParseTest {

    private val productionJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    private val threadPayload =
        """{"success":true,"data":[{"id":"m1","content":"Halo","createdAt":"2026-11-18T10:00:00.000Z","fromMe":true,"locked":false,"read":true,"sender":{"id":"u1","name":"Pembeli","image":null}},{"id":"m2","content":"Masih ada kak","createdAt":"2026-11-18T10:05:00.000Z","fromMe":false,"locked":false,"read":false,"sender":{"id":"u2","name":"Agen","image":null}},{"id":"m3","content":"Terima kasih","createdAt":"2026-11-18T10:06:00.000Z","fromMe":true,"locked":false,"read":false,"sender":{"id":"u1","name":"Pembeli","image":null}}]}"""

    private val legacyPayload =
        """{"success":true,"data":[{"id":"m1","content":"Halo","createdAt":"2026-11-18T10:00:00.000Z","fromMe":true,"locked":false,"sender":{"id":"u1","name":"Pembeli","image":null}}]}"""

    @Test fun `pesanku dengan read true dan false terparse sesuai server`() {
        val env = productionJson.decodeFromString<MessagesEnvelope>(threadPayload)
        assertEquals(true, env.success)
        val messages: List<MessageDto> = env.data
        assertEquals(true, messages[0].read)
        assertEquals(true, messages[0].fromMe)
        assertEquals(false, messages[1].read)
        assertEquals(false, messages[1].fromMe)
        assertEquals(false, messages[2].read)
    }

    @Test fun `payload server lama tanpa field read tetap terparse dengan read null`() {
        val env = productionJson.decodeFromString<MessagesEnvelope>(legacyPayload)
        assertEquals(true, env.success)
        assertEquals(true, env.data[0].fromMe)
        assertNull(env.data[0].read)
    }
}
