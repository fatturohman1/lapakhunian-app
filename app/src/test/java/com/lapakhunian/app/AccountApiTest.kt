package com.lapakhunian.app

import com.lapakhunian.app.core.network.PropertyApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class AccountApiTest {
    @Test fun `recovery calls only existing endpoints with an email body`() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            paths += request.url.encodedPath
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals(setOf("email"), body.keys)
            assertEquals("user@example.test", body.getValue("email").jsonPrimitive.content)
            respond("""{"success":true,"data":null}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val api = PropertyApi(client, "https://api.example.test")
            assertTrue(api.requestPasswordReset("user@example.test").success)
            assertTrue(api.resendVerification("user@example.test").success)
            assertEquals(listOf("/api/user/reset-password", "/api/user/resend-verification"), paths)
        } finally { client.close() }
    }
}
