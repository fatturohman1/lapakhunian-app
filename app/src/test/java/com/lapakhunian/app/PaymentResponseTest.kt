package com.lapakhunian.app

import com.lapakhunian.app.core.network.dto.SubscribeResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentResponseTest {
    @Test
    fun `payment URL and subscription identity match the server contract`() {
        val response = Json { ignoreUnknownKeys = true }.decodeFromString<SubscribeResponseDto>(
            """{"subscriptionId":"new-sub","paymentUrl":"https://payments.example.test/snap","token":"snap-token","status":"pending"}""",
        )
        assertEquals("https://payments.example.test/snap", response.paymentRedirectUrl)
        assertEquals("snap-token", response.paymentToken)
        assertEquals("new-sub", response.subscriptionId)
    }
}
