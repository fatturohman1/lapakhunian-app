package com.lapakhunian.app.core.data.repository

import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.domain.model.toDomain
import com.lapakhunian.app.core.network.dto.AgentDto
import com.lapakhunian.app.core.network.dto.AgentLeadsDto
import com.lapakhunian.app.core.network.dto.LeadWaDto
import com.lapakhunian.app.core.network.dto.LeadWaUnlockDto
import com.lapakhunian.app.core.network.dto.VoucherClaimDto
import com.lapakhunian.app.core.network.dto.VoucherDto
import com.lapakhunian.app.core.network.dto.UserHistoryItemDto
import com.lapakhunian.app.core.network.dto.PlanDto
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.core.network.dto.SubscribeResponseDto
import com.lapakhunian.app.core.network.dto.SubscriptionDto
import com.lapakhunian.app.core.network.dto.TransactionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val api: PropertyApi,
    private val session: SessionManager,
) {
    private suspend fun requireAuth() {
        if (session.token() == null) throw ApiException("Harap login terlebih dahulu")
    }

    suspend fun myListings(): Result<List<Property>> = runCatching {
        requireAuth()
        val email = session.userFlow.value?.email
            ?: throw ApiException("Email akun tidak ditemukan")
        val res = api.myListings(email)
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat listing")
        res.data.orEmpty().map { it.toDomain() }
    }

    suspend fun deleteListing(id: String): Result<Unit> = runCatching {
        requireAuth()
        val res = api.deleteListing(id)
        if (!res.success) throw ApiException(res.error ?: "Gagal menghapus")
        Unit
    }

    suspend fun markSold(id: String): Result<Unit> = runCatching {
        requireAuth()
        val res = api.updateListing(id, mapOf("isSold" to true))
        if (!res.success) throw ApiException(res.error ?: "Gagal menandai terjual")
        Unit
    }

    suspend fun renewListing(id: String): Result<SubscribeResponseDto> = runCatching {
        requireAuth()
        val res = api.renewListing(id)
        if (!res.success) throw ApiException(res.error ?: "Gagal memperpanjang")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun uploadImage(bytes: ByteArray, filename: String, mime: String): Result<String> = runCatching {
        requireAuth()
        val res = api.uploadImage(bytes, filename, mime)
        if (!res.success) throw ApiException(res.error ?: "Upload gagal")
        res.url ?: throw ApiException("Upload gagal: URL kosong")
    }

    suspend fun createListing(body: Map<String, Any?>): Result<Property> = runCatching {
        requireAuth()
        val res = api.createListing(body)
        if (!res.success) throw ApiException(res.error ?: "Gagal menyimpan iklan")
        res.data?.toDomain() ?: throw ApiException("Respon tidak valid")
    }

    suspend fun updateListing(id: String, body: Map<String, Any?>): Result<Property> = runCatching {
        requireAuth()
        val res = api.updateListing(id, body)
        if (!res.success) throw ApiException(res.error ?: "Gagal memperbarui iklan")
        res.data?.toDomain() ?: throw ApiException("Respon tidak valid")
    }

    suspend fun purchaseLeadPackage(packageId: String): Result<com.lapakhunian.app.core.network.dto.LeadPurchaseDto> = runCatching {
        requireAuth()
        val res = api.purchaseLeadPackage(packageId)
        if (!res.success) throw ApiException(res.error ?: "Gagal membeli paket lead")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun payLeadPurchase(purchaseId: String): Result<SubscribeResponseDto> = runCatching {
        requireAuth()
        val res = api.payLeadPurchase(purchaseId)
        if (!res.success) throw ApiException(res.error ?: "Gagal memproses pembayaran")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun subscription(): Result<SubscriptionDto> = runCatching {
        requireAuth()
        val res = api.subscription()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat langganan")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun agentProfile(): Result<AgentDto> = runCatching {
        requireAuth()
        val res = api.agentProfile()
        if (!res.success) throw ApiException(res.error ?: "Bukan akun agen")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    /** Edit profil agen (bio, phone, company, licenseNumber, image, banner). */
    suspend fun updateAgentProfile(body: Map<String, Any?>): Result<AgentDto> = runCatching {
        requireAuth()
        val res = api.updateAgentProfile(body)
        if (!res.success) throw ApiException(res.error ?: "Gagal menyimpan profil agen")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun agentLeads(): Result<AgentLeadsDto> = runCatching {
        requireAuth()
        val res = api.agentLeads()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat leads")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun agentPlans(): Result<List<PlanDto>> = runCatching {
        val res = api.agentPlans()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat paket")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    /** Hasil subscribe + pesan server (aktivasi gratis via voucher kirim `message`). */
    data class SubscribeOutcome(
        val sub: SubscribeResponseDto,
        val message: String? = null,
    )

    suspend fun subscribePlan(planId: String, voucherCode: String? = null): Result<SubscribeOutcome> = runCatching {
        requireAuth()
        val res = api.subscribePlan(planId, voucherCode)
        if (!res.success) throw ApiException(res.error ?: "Gagal berlangganan")
        val data = res.data ?: throw ApiException("Respon tidak valid")
        SubscribeOutcome(data, res.message)
    }

    suspend fun agentSubscription(): Result<com.lapakhunian.app.core.network.dto.AgentSubscriptionDto?> = runCatching {
        requireAuth()
        val res = api.agentSubscription()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat status pembayaran")
        res.data
    }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }

    /** Batalkan tagihan langganan pending (PATCH /api/agent/subscribe). */
    suspend fun cancelPendingSubscription(): Result<Unit> = runCatching {
        requireAuth()
        val res = api.cancelPendingSubscription()
        if (!res.success) throw ApiException(res.error ?: "Gagal membatalkan tagihan")
        Unit
    }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }

    suspend fun transactions(): Result<List<TransactionDto>> = runCatching {
        requireAuth()
        val res = api.transactions()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat transaksi")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    // ===== Lead WhatsApp =====

    suspend fun leadWaList(): Result<List<LeadWaDto>> = runCatching {
        requireAuth()
        val res = api.leadWaList()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat lead WhatsApp")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun unlockLeadWa(id: String): Result<LeadWaUnlockDto> = runCatching {
        requireAuth()
        val res = api.leadWaUnlock(id)
        if (!res.success) throw ApiException(res.error ?: "Gagal membuka lead")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    // ===== Voucher =====

    suspend fun activeVouchers(): Result<List<VoucherDto>> = runCatching {
        requireAuth()
        val res = api.activeVouchers()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat voucher")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    suspend fun claimVoucher(code: String): Result<VoucherClaimDto> = runCatching {
        requireAuth()
        val res = api.claimVoucher(code)
        if (!res.success) throw ApiException(res.error ?: "Gagal klaim voucher")
        res.data ?: throw ApiException("Respon tidak valid")
    }

    // ===== Riwayat user (kompensasi, pembelian, voucher) =====

    suspend fun userHistory(): Result<List<UserHistoryItemDto>> = runCatching {
        requireAuth()
        val res = api.userHistory()
        if (!res.success) throw ApiException(res.error ?: "Gagal memuat riwayat")
        res.data ?: throw ApiException("Respon tidak valid")
    }
}
