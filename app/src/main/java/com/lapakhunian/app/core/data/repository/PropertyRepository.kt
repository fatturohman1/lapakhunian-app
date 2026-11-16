package com.lapakhunian.app.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.lapakhunian.app.core.data.local.PropertyDao
import com.lapakhunian.app.core.data.local.toDomain
import com.lapakhunian.app.core.data.local.toEntity
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.toDomain
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.core.network.SearchFilters
import com.lapakhunian.app.core.network.dto.BannerDto
import com.lapakhunian.app.core.network.dto.ContactResponseDto
import com.lapakhunian.app.core.network.dto.GuestContactResponseDto
import com.lapakhunian.app.core.network.dto.UserDto
import com.lapakhunian.app.core.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(val statusMessage: String) : Exception(statusMessage)

/**
 * Repository properti: paging jaringan + cache Room untuk fallback offline.
 */
@Singleton
class PropertyRepository @Inject constructor(
    private val api: PropertyApi,
    private val dao: PropertyDao,
    private val session: SessionManager,
    private val notifPrefs: com.lapakhunian.app.core.notifications.NotificationPrefs,
) {
    fun search(filters: SearchFilters): Flow<PagingData<Property>> = Pager(
        config = PagingConfig(pageSize = 12, prefetchDistance = 6, enablePlaceholders = false),
        pagingSourceFactory = { PropertyPagingSource(api, filters, dao) },
    ).flow

    /** Featured untuk carousel. Gagal jaringan → cache Room (offline). */
    suspend fun featured(limit: Int = 6): List<Property> = try {
        val res = api.featured(limit)
        require(res.success) { res.error ?: "Gagal memuat" }
        val items = res.data.orEmpty().map { it.toDomain() }
        dao.upsertAll(items.map { it.toEntity() })
        items
    } catch (e: Exception) {
        dao.featured(limit).firstOrNull().orEmpty().map { it.toDomain() }
    }

    /** Listing terbaru. Gagal jaringan → cache Room. */
    suspend fun latest(limit: Int = 4): List<Property> = try {
        val res = api.list(SearchFilters(search = ""), page = 1, limit = limit)
        require(res.success) { res.error ?: "Gagal memuat" }
        val items = res.data.orEmpty().map { it.toDomain() }
        dao.upsertAll(items.map { it.toEntity() })
        items
    } catch (e: Exception) {
        dao.latest(limit).firstOrNull().orEmpty().map { it.toDomain() }
    }

    suspend fun detail(id: String, allowCacheFallback: Boolean = true): Result<Property> = try {
        val res = api.detail(id)
        if (!res.success || res.data == null) throw ApiException(res.error ?: "Properti tidak ditemukan")
        val property = res.data.toDomain()
        dao.upsertAll(listOf(property.toEntity()))
        Result.success(property)
    } catch (e: Exception) {
        // allowCacheFallback=false dipakai pengecekan status pembayaran —
        // cache stale bisa false "Pembayaran Berhasil".
        if (allowCacheFallback) {
            val cached = dao.byId(id)?.toDomain()
            cached?.let { Result.success(it) } ?: Result.failure(e)
        } else {
            Result.failure(e)
        }
    }

    suspend fun trackView(id: String) = runCatching { api.trackView(id) }

    suspend fun contactAgent(
        id: String,
        budget: Long?,
        timeline: String,
        message: String,
    ): Result<ContactResponseDto> = runCatching {
        val res = api.contactAgent(id, budget, timeline, message)
        if (!res.success) throw ApiException(res.error ?: "Gagal menghubungi agen")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    /** Kontak guest TANPA login — nama + nomor WhatsApp (endpoint publik web). */
    suspend fun guestContact(
        id: String,
        name: String,
        phone: String,
        message: String,
    ): Result<GuestContactResponseDto> = runCatching {
        val res = api.guestContact(id, name, phone, message)
        if (!res.success) throw ApiException(res.error ?: "Gagal mengirim pesan")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    // ===== Favorites =====

    /** Daftar favorit user (Bearer). Respons web nested → ambil `property` per item. */
    fun favorites(): Flow<List<Property>> = flow {
        val res = api.listFavorites()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat favorit")
        val items = res.data.orEmpty().mapNotNull { it.property?.toDomain() }
        dao.upsertAll(items.map { it.toEntity() })
        emit(items)
    }

    suspend fun setFavorite(propertyId: String, favorite: Boolean): Result<Unit> = runCatching {
        val res = if (favorite) api.addFavorite(propertyId) else api.removeFavorite(propertyId)
        if (!res.success) throw ApiException(res.error ?: "Gagal menyimpan favorit")
        Unit
    }

    suspend fun login(email: String, password: String): Result<UserDto> = runCatching {
        withFriendlyErrors {
            val res = api.login(email, password)
            if (!res.success || res.data?.sessionToken == null) {
                throw ApiException(res.error ?: "Email atau password salah")
            }
            val user = res.data.user ?: UserDto()
            session.saveSession(res.data.sessionToken, user)
            user
        }
    }

    /** Google Sign-In — idToken dari Credential Manager; server auto-buat akun bila belum ada. */
    suspend fun googleLogin(idToken: String): Result<GoogleLoginOutcome> = runCatching {
        withFriendlyErrors {
            val res = api.googleLogin(idToken)
            if (!res.success || res.data?.sessionToken == null) {
                throw ApiException(res.error ?: "Login Google gagal")
            }
            val user = res.data.user ?: UserDto()
            session.saveSession(res.data.sessionToken, user)
            GoogleLoginOutcome(user, needsOnboarding = res.data.needsOnboarding == true)
        }
    }

    /** Onboarding akun Google: server set role; agent → buat record Agent (3 lead gratis + trial 30 hari). */
    suspend fun completeOnboarding(role: String): Result<Unit> = runCatching {
        withFriendlyErrors {
            val res = api.completeOnboarding(role)
            if (!res.success) throw ApiException(res.error ?: "Gagal menyimpan pilihan")
            Unit
        }
    }

    data class GoogleLoginOutcome(
        val user: UserDto,
        /** true → tampilkan role picker (akun Google baru belum pilih owner/agent). */
        val needsOnboarding: Boolean,
    )

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String?,
    ): Result<Unit> = runCatching {
        withFriendlyErrors {
            val res = api.register(name, email, password, role, phone)
            if (!res.success) throw ApiException(res.error ?: "Registrasi gagal")
            Unit
        }
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        withFriendlyErrors {
            val res = api.requestPasswordReset(email)
            if (!res.success) throw ApiException(res.error ?: "Gagal meminta reset password")
            Unit
        }
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun resendVerification(email: String): Result<Unit> = runCatching {
        withFriendlyErrors {
            val res = api.resendVerification(email)
            if (!res.success) throw ApiException(res.error ?: "Gagal mengirim ulang verifikasi")
            Unit
        }
    }.onFailure { if (it is CancellationException) throw it }

    /** Banner promo aktif (position "homepage") — non-kritis: gagal jaringan → kosong, tanpa error UI. */
    suspend fun activeBanners(position: String = "homepage"): List<BannerDto> = runCatching {
        val res = api.banners(position)
        if (!res.success) emptyList() else res.data.orEmpty()
    }.getOrDefault(emptyList())

    /** Registrasi token FCM ke server (dipanggil saat login / token baru). Non-kritis. */
    suspend fun registerDeviceToken(token: String): Result<Unit> = runCatching {
        val res = api.registerDeviceToken(token)
        if (!res.success) throw ApiException(res.error ?: "Gagal registrasi token")
        Unit
    }

    // ===== Pengaturan akun =====

    /** Ganti password akun email — currentPassword wajib kecuali akun Google (tanpa password). */
    suspend fun changePassword(currentPassword: String?, newPassword: String): Result<Unit> =
        runCatching {
            withFriendlyErrors {
                val res = api.changePassword(currentPassword, newPassword)
                if (!res.success) throw ApiException(res.error ?: "Gagal mengubah password")
                Unit
            }
        }

    // ===== Tiket bantuan =====

    suspend fun supportTickets(): Result<List<com.lapakhunian.app.core.network.dto.SupportTicketDto>> =
        runCatching {
            if (session.tokenFlow.value == null) throw ApiException("Harap login terlebih dahulu")
            val res = api.supportTickets()
            if (!res.success) throw ApiException(res.error ?: "Gagal memuat tiket")
            res.data.orEmpty()
        }

    suspend fun createSupportTicket(subject: String, message: String): Result<Unit> = runCatching {
        withFriendlyErrors {
            if (session.tokenFlow.value == null) throw ApiException("Harap login terlebih dahulu")
            val res = api.createSupportTicket(subject, message)
            if (!res.success) throw ApiException(res.error ?: "Gagal mengirim tiket")
            Unit
        }
    }

    // ===== Lapor properti =====

    suspend fun reportProperty(propertyId: String, reason: String): Result<Unit> = runCatching {
        withFriendlyErrors {
            if (session.tokenFlow.value == null) throw ApiException("Harap login terlebih dahulu")
            val res = api.reportProperty(propertyId, reason)
            if (!res.success) throw ApiException(res.error ?: "Gagal mengirim laporan")
            Unit
        }
    }

    // ===== Titip jual (konsinyasi — publik tanpa login) =====

    /** Upload foto titip jual — endpoint publik, rate limit di server (5/15 menit/IP). */
    suspend fun titipJualUploadPhoto(bytes: ByteArray, filename: String, mime: String): Result<String> =
        runCatching {
            val res = api.titipJualUploadPhoto(bytes, filename, mime)
            if (!res.success) throw ApiException(res.error ?: "Upload foto gagal")
            res.url ?: throw ApiException("Upload gagal: URL kosong")
        }

    suspend fun submitTitipJual(
        name: String,
        phone: String,
        propertyTitle: String,
        address: String,
        estimatedPrice: String,
        photos: List<String> = emptyList(),
    ): Result<Unit> = runCatching {
        withFriendlyErrors {
            val res = api.titipJual(
                com.lapakhunian.app.core.network.dto.TitipJualRequest(
                    name = name,
                    phone = phone,
                    propertyTitle = propertyTitle,
                    address = address,
                    estimatedPrice = estimatedPrice,
                    photos = photos,
                ),
            )
            if (!res.success) throw ApiException(res.error ?: "Gagal mengirim data")
            Unit
        }
    }

    /** Logout: bersihkan cache properti (biar user berikutnya tidak lihat cache user lama) + sesi. */
    suspend fun logout() {
        // hapus registrasi push device ini dulu (server berhenti kirim push ke user lama)
        notifPrefs.fcmToken?.let { token ->
            runCatching { api.deleteDeviceToken(token) }
            notifPrefs.fcmToken = null
        }
        dao.clear()
        session.clearSession()
    }

    companion object {
        /**
         * Pesan ramah untuk error tak terduga. ApiException (pesan server, mis.
         * "Email atau password salah") diteruskan apa adanya; CancellationException
         * dilempar ulang; parsing gagal → server bermasalah; sisanya → koneksi.
         */
        private suspend fun <T> withFriendlyErrors(block: suspend () -> T): T = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: SerializationException) {
            throw ApiException("Server bermasalah, coba lagi nanti")
        } catch (e: Exception) {
            throw ApiException("Tidak bisa terhubung ke server. Periksa koneksi internet kamu.")
        }
    }
}

/** PagingSource jaringan; sambil load refresh, tulis cache ke Room. */
private class PropertyPagingSource(
    private val api: PropertyApi,
    private val filters: SearchFilters,
    private val dao: PropertyDao,
) : androidx.paging.PagingSource<Int, Property>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Property> {
        val page = params.key ?: 1
        return try {
            val res = api.list(filters, page)
            if (!res.success) throw ApiException(res.error ?: "Gagal memuat properti")
            val items = res.data.orEmpty().map { it.toDomain() }
            if (page == 1) dao.upsertAll(items.map { it.toEntity() })
            val totalPages = res.meta?.totalPages ?: page
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page < totalPages && items.isNotEmpty()) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Property>): Int? =
        state.anchorPosition?.let { anchor -> (anchor / PAGE_SIZE) + 1 }

    companion object { const val PAGE_SIZE = 12 }
}
