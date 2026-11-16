package com.lapakhunian.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.local.PropertyDao
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.core.network.dto.ApiEnvelope
import com.lapakhunian.app.core.network.dto.ContactResponseDto
import com.lapakhunian.app.core.notifications.NotificationPrefs
import com.lapakhunian.app.feature.chat.ContactViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Payload persis dari respons server saat bug "Illegal input … $.data.deducted" dilaporkan. */
private const val CONTACT_PAYLOAD =
    """{"success":true,"data":{"conversationId":"2ec15817-8fba-4122-95a9-b2b562f6ab2a","leadCost":1,"deducted":true,"hasUnlimitedChat":false},"message":"Percakapan dimulai dan 1 lead telah dipotong."}"""

@OptIn(ExperimentalCoroutinesApi::class)
class ContactResponseParseTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private val productionJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    @Test fun `server boolean deducted parses with production json config`() {
        val res = productionJson.decodeFromString<ApiEnvelope<ContactResponseDto>>(CONTACT_PAYLOAD)
        assertTrue(res.success)
        assertEquals("2ec15817-8fba-4122-95a9-b2b562f6ab2a", res.data?.conversationId)
        assertEquals(1, res.data?.leadCost)
        assertEquals(true, res.data?.deducted)
        assertEquals(false, res.data?.hasUnlimitedChat)
    }

    @Test fun `successful contact opens the thread instead of showing a parse error`() = runTest(dispatcher) {
        val client = HttpClient(MockEngine(io.ktor.client.engine.mock.MockEngineConfig().apply {
            dispatcher = this@ContactResponseParseTest.dispatcher
            addHandler {
                respond(CONTACT_PAYLOAD, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })) { install(ContentNegotiation) { json(productionJson) } }
        try {
            val session = mockk<SessionManager>()
            every { session.tokenFlow } returns MutableStateFlow<String?>("token")
            val repo = PropertyRepository(
                PropertyApi(client, "https://api.example.test"),
                mockk<PropertyDao>(relaxed = true),
                session,
                mockk<NotificationPrefs>(relaxed = true),
            )
            val vm = ContactViewModel(SavedStateHandle(mapOf("propertyId" to "p1")), repo)
                .also { store.put("contact", it) }
            var opened: String? = null
            vm.send("test ct", "soon", "test chat apk") { opened = it }
            runCurrent()
            assertNull(vm.state.value.error)
            assertTrue(vm.state.value.success)
            assertEquals("2ec15817-8fba-4122-95a9-b2b562f6ab2a", opened)
        } finally { client.close() }
    }
}