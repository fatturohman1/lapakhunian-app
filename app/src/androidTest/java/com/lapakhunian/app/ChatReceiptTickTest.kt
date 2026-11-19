package com.lapakhunian.app

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.ChatRepository
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.feature.chat.ThreadScreen
import com.lapakhunian.app.feature.chat.ThreadViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Regresi ikon status pesan ala WhatsApp pada bubble pesanku:
 * jam saat mengirim, ceklis 1 setelah terkirim, ceklis 2 (dibaca) setelah server melaporkan read.
 * Semua lewat MockEngine — tidak menyentuh server produksi.
 */
class ChatReceiptTickTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private lateinit var client: HttpClient

    @After fun tearDown() {
        compose.runOnUiThread { store.clear() }
        if (::client.isInitialized) client.close()
    }

    @Test fun bubbleShowsClockThenOneCheckThenDoubleCheck() {
        val releaseSend = CompletableDeferred<Unit>()
        val polls = AtomicInteger()
        val sentRead = java.util.concurrent.atomic.AtomicBoolean(false)
        client = HttpClient(MockEngine { request ->
            val response = when {
                request.method == HttpMethod.Post && request.url.encodedPath == "/api/chat/messages" -> {
                    releaseSend.await()
                    """{"success":true,"data":{"id":"sent-a","content":"Pesan A","createdAt":"2026-11-18T21:57:00.000Z","fromMe":true,"read":false}}"""
                }
                request.url.encodedPath == "/api/chat/messages" -> {
                    val poll = polls.incrementAndGet()
                    // Poll pertama sebelum kirim: hanya pesan lama — sent-a muncul setelah POST sukses
                    val list = if (poll == 1) {
                        """[{"id":"m0","content":"Pesan lama","createdAt":"2026-11-18T20:00:00.000Z","fromMe":true,"locked":false,"read":false,"sender":{"id":"u1","name":"Aku","image":null}}]"""
                    } else {
                        """[{"id":"m0","content":"Pesan lama","createdAt":"2026-11-18T20:00:00.000Z","fromMe":true,"locked":false,"read":false,"sender":{"id":"u1","name":"Aku","image":null}},{"id":"sent-a","content":"Pesan A","createdAt":"2026-11-18T21:57:00.000Z","fromMe":true,"locked":false,"read":${sentRead.get()},"sender":{"id":"u1","name":"Aku","image":null}}]"""
                    }
                    """{"success":true,"data":$list,"locked":false}"""
                }
                request.url.encodedPath == "/api/chat/conversations" -> """{"success":true,"data":[]}"""
                request.method == HttpMethod.Patch -> """{"success":true,"data":null}"""
                else -> error("Unexpected test request: ${request.url.encodedPath}")
            }
            respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        lateinit var vm: ThreadViewModel
        compose.runOnUiThread {
            compose.activity.enableEdgeToEdge()
            vm = ThreadViewModel(
                SavedStateHandle(mapOf("conversationId" to "test-thread")),
                ChatRepository(PropertyApi(client, "https://api.example.test")),
            )
            store.put("thread", vm)
        }
        compose.setContent { LapakHunianTheme {
            Scaffold(bottomBar = { NavigationBar { } }) { padding ->
                Box(Modifier.padding(padding)) { ThreadScreen(onBack = {}, viewModel = vm) }
            }
        } }
        compose.waitUntil(5_000) { !vm.state.value.loading }

        // Jam: pesan dalam penerbangan (optimistic temp) → ikon jam di bubble
        compose.onNode(hasSetTextAction()).performTextInput("Pesan A")
        compose.onNodeWithContentDescription("Kirim").performClick()
        compose.waitUntil(5_000) { vm.state.value.sending }
        compose.onNodeWithContentDescription("Sedang mengirim")
            .performScrollTo()
            .assertIsDisplayed()

        // Ceklis 1: server menerima pesan — pastikan sent-a sudah masuk list (transisi jam→ceklis)
        releaseSend.complete(Unit)
        compose.waitUntil(5_000) {
            !vm.state.value.sending && vm.state.value.messages.any { it.id == "sent-a" }
        }
        compose.onAllNodesWithContentDescription("Terkirim")[0].performScrollTo().assertIsDisplayed()

        // Ceklis 2: polling berikutnya melaporkan pesan sudah dibaca
        sentRead.set(true)
        compose.waitUntil(20_000) {
            vm.state.value.messages.firstOrNull { it.id == "sent-a" }?.read == true
        }
        compose.onAllNodesWithContentDescription("Dibaca")[0].performScrollTo().assertIsDisplayed()
    }
}
