package com.lapakhunian.app.core.network.di

import com.lapakhunian.app.BuildConfig
import com.lapakhunian.app.core.data.repository.ApiException
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.PropertyApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideHttpClient(json: Json, session: SessionManager, baseUrl: String): HttpClient {
        val auth = createClientPlugin("LapakAuth") {
            onRequest { request, _ ->
                session.tokenFlow.value?.let { token ->
                    request.headers.append("Authorization", "Bearer $token")
                }
            }
        }
        // Guard CSRF di server mengizinkan request yang membawa Origin = host sendiri.
        // Browser selalu mengirim Origin; app native tidak, sehingga request tanpa
        // Bearer (mis. guest-contact "Hubungi Agen") ditolak 403 "CSRF validation failed".
        // Set Origin dari baseUrl agar request app dikenali sebagai asal-usul sah.
        val baseUri = java.net.URI(baseUrl.trimEnd('/'))
        val origin = buildString {
            append(baseUri.scheme)
            append("://")
            append(baseUri.host)
            if (baseUri.port > 0) append(":").append(baseUri.port)
        }
        return HttpClient(OkHttp) {
            expectSuccess = false // status error ditangani via envelope success=false
            install(ContentNegotiation) { json(json) }
            install(auth)
            // Token expired/invalid → invalidate session agar user diminta login ulang.
            // Endpoint /api/auth/ dikecualikan: 401 di situ berarti kredensial salah
            // (mis. "Email atau password salah") — jangan hapus sesi / timpa pesannya.
            HttpResponseValidator {
                validateResponse { response ->
                    val isAuthPath = response.call.request.url.encodedPath.startsWith("/api/auth/")
                    if (response.status == HttpStatusCode.Unauthorized && !isAuthPath) {
                        session.clearSessionImmediate()
                    }
                }
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                header("Origin", origin)
                // Penanda asal klien — dipakai analytics server (source
                // "android-app" di /api/visitors/track) dan debug sisi server.
                header("X-Client-App", "android/${com.lapakhunian.app.BuildConfig.VERSION_NAME}")
            }
            engine {
                config {
                    connectTimeout(20, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                }
            }
        }
    }

    @Provides
    @Singleton
    fun providePropertyApi(client: HttpClient, baseUrl: String): PropertyApi =
        PropertyApi(client, baseUrl)
}
