package com.lapakhunian.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class ChatRetryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val store = ViewModelStore()
    private lateinit var client: HttpClient

    @After fun tearDown() {
        compose.runOnUiThread { store.clear() }
        if (::client.isInitialized) client.close()
    }

    @Test fun failedSendAndRetryPreserveTheNewDraft() {
        val releaseFailure = CompletableDeferred<Unit>()
        val sends = AtomicInteger()
        val sentMessage = """{"id":"sent-a","content":"Pesan A","fromMe":true}"""
        client = HttpClient(MockEngine { request ->
            val response = when {
                request.method == HttpMethod.Post && request.url.encodedPath == "/api/chat/messages" -> {
                    if (sends.incrementAndGet() == 1) {
                        releaseFailure.await()
                        throw IOException("Koneksi terputus")
                    }
                    """{"success":true,"data":$sentMessage}"""
                }
                request.url.encodedPath == "/api/chat/messages" -> {
                    val messages = if (sends.get() > 1) sentMessage else ""
                    """{"success":true,"data":[$messages],"locked":false}"""
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
        compose.onNode(hasSetTextAction()).performTextInput("Pesan A")
        compose.onNodeWithContentDescription("Kirim").performClick()
        compose.waitUntil(5_000) { sends.get() == 1 }
        compose.onNode(hasSetTextAction()).performTextInput("Draf B")
        releaseFailure.complete(Unit)
        compose.waitUntil(5_000) { vm.state.value.failedMessages.isNotEmpty() }
        compose.onNode(hasSetTextAction()).assertTextEquals("Draf B")
        try {
            compose.onNodeWithText("Gagal dikirim").performScrollTo().assertIsDisplayed()
        } catch (failure: AssertionError) {
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "chat-retry-failure.png").outputStream().use {
                screenshot?.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            throw failure
        }
        compose.onNodeWithText("Coba kirim lagi").performScrollTo().performClick()
        compose.waitUntil(5_000) { !vm.state.value.sending && vm.state.value.failedMessages.isEmpty() }
        compose.onNode(hasSetTextAction()).assertTextEquals("Draf B")
        assertEquals(2, sends.get())
        assertEquals(listOf("sent-a"), vm.state.value.messages.map { it.id })
    }
}
