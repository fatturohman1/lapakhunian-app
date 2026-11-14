package com.lapakhunian.app.core.network

import com.lapakhunian.app.core.network.dto.AgentDto
import com.lapakhunian.app.core.network.dto.AgentLeadsDto
import com.lapakhunian.app.core.network.dto.ApiEnvelope
import com.lapakhunian.app.core.network.dto.ConversationDto
import com.lapakhunian.app.core.network.dto.ContactResponseDto
import com.lapakhunian.app.core.network.dto.LeadWaDto
import com.lapakhunian.app.core.network.dto.LeadWaUnlockDto
import com.lapakhunian.app.core.network.dto.LoginResponseDto
import com.lapakhunian.app.core.network.dto.MessageDto
import com.lapakhunian.app.core.network.dto.MessagesEnvelope
import com.lapakhunian.app.core.network.dto.NotificationDto
import com.lapakhunian.app.core.network.dto.PlanDto
import com.lapakhunian.app.core.network.dto.PropertyDto
import com.lapakhunian.app.core.network.dto.RegisterResponseDto
import com.lapakhunian.app.core.network.dto.SubscribeResponseDto
import com.lapakhunian.app.core.network.dto.SubscriptionDto
import com.lapakhunian.app.core.network.dto.TransactionDto
import com.lapakhunian.app.core.network.dto.UploadResponse
import com.lapakhunian.app.core.network.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

/** Filter pencarian properti. minPrice/maxPrice dalam RUPAH penuh sesuai kontrak API web. */
data class SearchFilters(
    val search: String = "",
    val purpose: String? = "dijual",
    val category: String? = null,
    val city: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val amenities: Set<String> = emptySet(),
    val listedBy: String? = null,
) {
    fun isEmpty(): Boolean = this == SearchFilters()
}

@Singleton
class PropertyApi @Inject constructor(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun list(
        filters: SearchFilters,
        page: Int,
        limit: Int = 12,
    ): ApiEnvelope<List<PropertyDto>> = client.get("$baseUrl/api/properties") {
        filters.search.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
        filters.purpose?.let { parameter("purpose", it) }
        filters.category?.let { parameter("category", it) }
        filters.city?.let { parameter("city", it) }
        filters.minPrice?.let { parameter("minPrice", it) }
        filters.maxPrice?.let { parameter("maxPrice", it) }
        filters.listedBy?.let { parameter("listedBy", it) }
        if (filters.amenities.isNotEmpty()) parameter("amenities", filters.amenities.joinToString(","))
        parameter("page", page)
        parameter("limit", limit)
    }.body()

    suspend fun featured(limit: Int = 6): ApiEnvelope<List<PropertyDto>> =
        client.get("$baseUrl/api/properties") {
            parameter("page", 1)
            parameter("limit", limit)
        }.body()

    suspend fun detail(id: String): ApiEnvelope<PropertyDto> =
        client.get("$baseUrl/api/properties/$id").body()

    suspend fun trackView(id: String) = client.post("$baseUrl/api/properties/$id/view").body<ApiEnvelope<Unit?>>()

    suspend fun contactAgent(
        id: String,
        budget: Long?,
        timeline: String,
        message: String,
    ): ApiEnvelope<ContactResponseDto> = client.post("$baseUrl/api/properties/$id/contact") {
        contentType(ContentType.Application.Json)
        setBody(mapOf("budget" to budget, "timeline" to timeline, "message" to message))
    }.body()

    /** Kontak publik tanpa login — nama + nomor WA (endpoint sama dengan web, anti-spam di server). */
    suspend fun guestContact(
        id: String,
        name: String,
        phone: String,
        message: String,
    ): ApiEnvelope<com.lapakhunian.app.core.network.dto.GuestContactResponseDto> =
        client.post("$baseUrl/api/properties/$id/guest-contact") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to name, "phone" to phone, "message" to message))
        }.body()

    suspend fun login(email: String, password: String): ApiEnvelope<LoginResponseDto> =
        client.post("$baseUrl/api/auth/mobile-login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email, "password" to password))
        }.body()

    /** Google Sign-In: idToken Credential Manager → server verifikasi + auto-buat akun. */
    suspend fun googleLogin(idToken: String): ApiEnvelope<LoginResponseDto> =
        client.post("$baseUrl/api/auth/mobile-google-login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("idToken" to idToken))
        }.body()

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String?,
    ): ApiEnvelope<RegisterResponseDto> = client.post("$baseUrl/api/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(
            buildMap<String, String> {
                put("email", email)
                put("password", password)
                put("role", role)
                name.takeIf { it.isNotBlank() }?.let { put("name", it) }
                phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
            },
        )
    }.body()

    // ===== Favorites (Bearer, cek SessionManager) =====

    /** Respons web nested: [{ id, property: {...} }] — parse via FavoriteItemDto. limit maks 50. */
    suspend fun listFavorites(): ApiEnvelope<List<com.lapakhunian.app.core.network.dto.FavoriteItemDto>> =
        client.get("$baseUrl/api/favorites") {
            parameter("limit", 50)
        }.body()

    suspend fun addFavorite(propertyId: String): ApiEnvelope<Unit?> = client.post("$baseUrl/api/favorites") {
        contentType(ContentType.Application.Json)
        setBody(mapOf("propertyId" to propertyId))
    }.body()

    suspend fun removeFavorite(propertyId: String): ApiEnvelope<Unit?> =
        client.delete("$baseUrl/api/favorites") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("propertyId" to propertyId))
        }.body()

    // ===== Chat (Bearer) =====

    suspend fun conversations(): ApiEnvelope<List<ConversationDto>> =
        client.get("$baseUrl/api/chat/conversations").body()

    suspend fun messages(conversationId: String): MessagesEnvelope =
        client.get("$baseUrl/api/chat/messages") {
            parameter("conversationId", conversationId)
        }.body()

    suspend fun sendMessage(conversationId: String, content: String): ApiEnvelope<MessageDto> =
        client.post("$baseUrl/api/chat/messages") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("conversationId" to conversationId, "content" to content))
        }.body()

    suspend fun markConversationRead(conversationId: String): ApiEnvelope<Unit?> =
        client.patch("$baseUrl/api/chat/conversations/$conversationId/read").body()

    suspend fun unlockConversation(conversationId: String): ApiEnvelope<Unit?> =
        client.post("$baseUrl/api/chat/conversations/$conversationId/unlock").body()

    // ===== Notifications (Bearer) =====

    suspend fun notifications(): ApiEnvelope<List<NotificationDto>> =
        client.get("$baseUrl/api/notifications").body()

    suspend fun markNotificationRead(id: String): ApiEnvelope<Unit?> =
        client.patch("$baseUrl/api/notifications/$id/read").body()

    suspend fun markAllNotificationsRead(): ApiEnvelope<Unit?> =
        client.patch("$baseUrl/api/notifications/read-all").body()

    // ===== Profil user (Bearer) =====

    /** PATCH /api/user/profile — body {name?, image?}; data kembali {name,email,image,banner}. */
    suspend fun updateProfile(body: Map<String, Any?>): ApiEnvelope<UserDto> =
        client.patch("$baseUrl/api/user/profile") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    // ===== Dashboard: upload, listing owner, agent, transaksi =====

    suspend fun uploadImage(bytes: ByteArray, filename: String, mime: String): UploadResponse {
        // Body multipart dirakit manual sebagai ByteArray (bukan MultiPartFormDataContent +
        // BinaryItem streaming) supaya panjang body diketahui → Content-Length terkirim.
        // Body streaming dikirim "Transfer-Encoding: chunked" dan ditolak server (411
        // "Content-Length header dibutuhkan"). Part mengikuti struktur lama:
        // name="file", filename=..., Content-Type per part.
        val boundary = "lapakhunian-${java.util.UUID.randomUUID().toString().replace("-", "")}"
        val head = (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n" +
                "Content-Type: $mime\r\n" +
                "\r\n"
            ).toByteArray(Charsets.UTF_8)
        val tail = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val body = head + bytes + tail

        return client.post("$baseUrl/api/upload") {
            header(HttpHeaders.ContentType, "multipart/form-data; boundary=$boundary")
            setBody(body)
        }.body()
    }

    suspend fun createListing(body: Map<String, Any?>): ApiEnvelope<PropertyDto> =
        client.post("$baseUrl/api/properties") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun updateListing(id: String, body: Map<String, Any?>): ApiEnvelope<PropertyDto> =
        client.put("$baseUrl/api/properties/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteListing(id: String): ApiEnvelope<Unit?> =
        client.delete("$baseUrl/api/properties/$id").body()

    suspend fun myListings(email: String, page: Int = 1, limit: Int = 50): ApiEnvelope<List<PropertyDto>> =
        client.get("$baseUrl/api/properties") {
            parameter("contactEmail", email)
            parameter("page", page)
            parameter("limit", limit)
        }.body()

    suspend fun renewListing(id: String): ApiEnvelope<SubscribeResponseDto> =
        client.post("$baseUrl/api/properties/$id/renew").body()

    suspend fun subscription(): ApiEnvelope<SubscriptionDto> =
        client.get("$baseUrl/api/subscription").body()

    suspend fun agentProfile(): ApiEnvelope<AgentDto> =
        client.get("$baseUrl/api/agent/profile").body()

    suspend fun agentLeads(): ApiEnvelope<AgentLeadsDto> =
        client.get("$baseUrl/api/leads").body()

    suspend fun agentPlans(): ApiEnvelope<List<PlanDto>> =
        client.get("$baseUrl/api/agent/plans").body()

    /** voucherCode opsional — diskon dihitung server (DISCOUNT_PERCENT/FIXED/BONUS_LEADS). */
    suspend fun subscribePlan(planId: String, voucherCode: String? = null): ApiEnvelope<SubscribeResponseDto> =
        client.post("$baseUrl/api/agent/subscribe") {
            contentType(ContentType.Application.Json)
            setBody(
                buildMap<String, Any?> {
                    put("planId", planId)
                    voucherCode?.takeIf { it.isNotBlank() }?.let { put("voucherCode", it) }
                },
            )
        }.body()

    suspend fun transactions(): ApiEnvelope<List<TransactionDto>> =
        client.get("$baseUrl/api/transactions").body()

    // ===== Pembelian paket lead (buat purchase pending lalu bayar) =====

    suspend fun purchaseLeadPackage(packageId: String): ApiEnvelope<com.lapakhunian.app.core.network.dto.LeadPurchaseDto> =
        client.post("$baseUrl/api/leads") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("packageId" to packageId))
        }.body()

    suspend fun payLeadPurchase(purchaseId: String): ApiEnvelope<SubscribeResponseDto> =
        client.post("$baseUrl/api/leads/$purchaseId/pay").body()

    // ===== Lead WhatsApp =====

    suspend fun leadWaList(): ApiEnvelope<List<LeadWaDto>> =
        client.get("$baseUrl/api/lead-wa").body()

    suspend fun leadWaUnlock(id: String): ApiEnvelope<LeadWaUnlockDto> =
        client.patch("$baseUrl/api/lead-wa") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("id" to id, "action" to "unlock"))
        }.body()

    // ===== Voucher =====

    suspend fun activeVouchers(): ApiEnvelope<List<com.lapakhunian.app.core.network.dto.VoucherDto>> =
        client.get("$baseUrl/api/vouchers/active").body()

    suspend fun claimVoucher(code: String): ApiEnvelope<com.lapakhunian.app.core.network.dto.VoucherClaimDto> =
        client.post("$baseUrl/api/vouchers/claim") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("code" to code))
        }.body()

    // ===== Riwayat user (pembelian lead, kompensasi, voucher, listing) =====

    suspend fun userHistory(): ApiEnvelope<List<com.lapakhunian.app.core.network.dto.UserHistoryItemDto>> =
        client.get("$baseUrl/api/user/history").body()
}
