package com.lapakhunian.app

import com.lapakhunian.app.core.analytics.VisitorCoords
import com.lapakhunian.app.core.analytics.VisitorSession
import com.lapakhunian.app.core.analytics.VisitorTracker
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSession(private val id: String) : VisitorSession {
    override val sessionId = id
}

private val noCoords = object : VisitorCoords {
    override suspend fun lastKnown(): Pair<Double, Double>? = null
}

private fun recordingEngine(
    onCaptured: (method: HttpMethod, path: String, body: String) -> Unit,
): MockEngine = MockEngine { request ->
    onCaptured(
        request.method,
        request.url.fullPath,
        (request.body as TextContent).text,
    )
    respond(
        content = ByteReadChannel("""{"success":true,"tracked":true,"viewId":"v-1"}"""),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}

private fun testClient(engine: MockEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }
}

/**
 * Tracker kunjungan: POST /api/visitors/track dengan body {path, sessionId,
 * source: "android-app", lat?, lng?}. Koordinat null → field tidak dikirim
 * (explicitNulls=false). Header X-Client-App dipasang NetworkModule, bukan
 * tracker — diuji terpisah lewat review/emulator.
 */
class VisitorTrackerTest {

    @Test
    fun `posts web-format path with android source and sessionId`() = runTest {
        var method: HttpMethod? = null
        var path: String? = null
        var body: String? = null
        val client = testClient(recordingEngine { m, p, b -> method = m; path = p; body = b })
        try {
            val tracker = VisitorTracker(client, "https://api.example.test", FakeSession("sess-123"), noCoords)
            tracker.trackPropertyOpen(slug = "rumah-mewah-bsd", fallbackId = "id-9")

            assertEquals(HttpMethod.Post, method)
            assertEquals("/api/visitors/track", path)
            val json = Json.parseToJsonElement(body!!).jsonObject
            assertEquals("/properti/rumah-mewah-bsd", json["path"]!!.jsonPrimitive.content)
            assertEquals("sess-123", json["sessionId"]!!.jsonPrimitive.content)
            assertEquals("android-app", json["source"]!!.jsonPrimitive.content)
            assertNull(json["lat"])
            assertNull(json["lng"])
        } finally {
            client.close()
        }
    }

    @Test
    fun `falls back to id when slug is null`() = runTest {
        var body: String? = null
        val client = testClient(recordingEngine { _, _, b -> body = b })
        try {
            val tracker = VisitorTracker(client, "https://api.example.test", FakeSession("sess-9"), noCoords)
            tracker.trackPropertyOpen(slug = null, fallbackId = "abc-id")

            val json = Json.parseToJsonElement(body!!).jsonObject
            assertEquals("/properti/abc-id", json["path"]!!.jsonPrimitive.content)
        } finally {
            client.close()
        }
    }

    @Test
    fun `includes coarse coords when available`() = runTest {
        var body: String? = null
        val client = testClient(recordingEngine { _, _, b -> body = b })
        val withCoords = object : VisitorCoords {
            override suspend fun lastKnown(): Pair<Double, Double>? = -6.85 to 107.6
        }
        try {
            val tracker = VisitorTracker(client, "https://api.example.test", FakeSession("sess-1"), withCoords)
            tracker.track("/properti/x")

            val json = Json.parseToJsonElement(body!!).jsonObject
            assertTrue(json["lat"]!!.jsonPrimitive.content.startsWith("-6.85"))
            assertTrue(json["lng"]!!.jsonPrimitive.content.startsWith("107.6"))
        } finally {
            client.close()
        }
    }
}

/** Tracker yang aman untuk VM test lain: request ditanggapi 200 tanpa efek. */
fun visitorTrackerStub(): VisitorTracker = VisitorTracker(
    testClient(recordingEngine { _, _, _ -> }),
    "https://api.example.test",
    FakeSession("stub-session"),
    noCoords,
)
