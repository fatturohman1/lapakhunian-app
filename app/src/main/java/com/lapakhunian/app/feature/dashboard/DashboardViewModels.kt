package com.lapakhunian.app.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.domain.model.ListingStatus
import com.lapakhunian.app.core.domain.model.Property
import com.lapakhunian.app.core.network.dto.AgentDto
import com.lapakhunian.app.core.network.dto.AgentLeadsDto
import com.lapakhunian.app.core.network.dto.PlanDto
import com.lapakhunian.app.core.network.dto.TransactionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ================= Owner =================

data class OwnerDashUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val items: List<Property> = emptyList(),
    val error: String? = null,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class OwnerDashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OwnerDashUiState())
    val state: StateFlow<OwnerDashUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.myListings().fold(
                onSuccess = { items -> _state.update { it.copy(loading = false, items = items) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat") } },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }

    /** Tandai terjual: item tetap di daftar, status jadi SOLD. */
    fun markSold(id: String) {
        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            repo.markSold(id).fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(
                            actionInProgress = false,
                            items = s.items.map { p ->
                                if (p.id == id) p.copy(status = ListingStatus.SOLD, isSold = true) else p
                            },
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(actionInProgress = false, error = e.message) } },
            )
        }
    }

    fun delete(id: String) = mutate(id) { repo.deleteListing(id) }

    private fun mutate(id: String, action: suspend () -> Result<Unit>) {
        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            action().fold(
                onSuccess = { _state.update { s -> s.copy(actionInProgress = false, items = s.items.filterNot { p -> p.id == id }) } },
                onFailure = { e -> _state.update { it.copy(actionInProgress = false, error = e.message) } },
            )
        }
    }
}

data class WizardUiState(
    val submitting: Boolean = false,
    val uploading: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    /** Validasi form di UI (judul/harga/alamat) — diisi saat klik Lanjut/Submit. */
    val validationError: String? = null,
    val success: Boolean = false,
    /** URL path /uploads/... dari server */
    val uploadedPhotos: List<String> = emptyList(),
    // form fields — disimpan di VM supaya survive rotasi (remember hilang saat config change)
    val step: Int = 1,
    val purpose: String = "dijual",
    val title: String = "",
    val description: String = "",
    val category: String = "house",
    val bedrooms: String = "",
    val bathrooms: String = "",
    val landArea: String = "",
    val buildingArea: String = "",
    val floors: String = "1",
    val certificate: String = "SHM",
    val electricity: String = "",
    val furnishing: String = "Unfurnished",
    val amenities: Set<String> = emptySet(),
    val address: String = "",
    val city: String = "Kota Bekasi",
    val price: String = "",
)

@HiltViewModel
class ListingWizardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val propertyRepo: PropertyRepository,
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Route dashboard wizard tanpa argumen; route edit mengirim "listingId". */
    private val editListingId: String? = savedStateHandle.get<String>("listingId")

    private val _state = MutableStateFlow(WizardUiState())
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    init {
        val id = editListingId
        if (id == null) {
            _state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                propertyRepo.detail(id).fold(
                    onSuccess = { p -> _state.update { it.prefillFrom(p).copy(loading = false) } },
                    onFailure = { e -> _state.update { it.copy(loading = false, error = e.message ?: "Gagal memuat iklan") } },
                )
            }
        }
    }

    /** Prefill form dari listing yang diedit. */
    private fun WizardUiState.prefillFrom(p: Property): WizardUiState = copy(
        purpose = when (p.purpose?.lowercase()) {
            "jual", "dijual" -> "dijual"
            "sewa", "disewa" -> "disewa"
            else -> purpose
        },
        title = p.title,
        description = p.description ?: "",
        category = p.category?.apiValue ?: category,
        bedrooms = p.bedrooms?.toString().orEmpty(),
        bathrooms = p.bathrooms?.toString().orEmpty(),
        landArea = p.landArea?.toString().orEmpty(),
        buildingArea = p.buildingArea?.toString().orEmpty(),
        floors = p.floors?.toString() ?: "1",
        electricity = p.electricity ?: "",
        furnishing = p.furnishing ?: furnishing,
        certificate = p.certificate ?: certificate,
        amenities = p.amenities.toSet(),
        address = p.address ?: "",
        city = p.city ?: city,
        price = p.price.toString(),
        uploadedPhotos = p.images,
    )

    fun update(transform: (WizardUiState) -> WizardUiState) {
        _state.update(transform)
    }

    fun setStep(step: Int) {
        _state.update { it.copy(step = step.coerceIn(1, 4)) }
    }

    fun uploadPhoto(bytes: ByteArray, filename: String, mime: String) {
        _state.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch {
            repo.uploadImage(bytes, filename, mime).fold(
                onSuccess = { url -> _state.update { it.copy(uploading = false, uploadedPhotos = it.uploadedPhotos + url) } },
                onFailure = { e -> _state.update { it.copy(uploading = false, error = e.message) } },
            )
        }
    }

    fun removePhoto(url: String) {
        _state.update { it.copy(uploadedPhotos = it.uploadedPhotos - url) }
    }

    /** Jadikan foto terpilih sebagai foto utama — pindah ke urutan pertama (thumbnail server). */
    fun setCoverPhoto(url: String) {
        _state.update { s ->
            if (s.uploadedPhotos.firstOrNull() == url) return@update s
            s.copy(uploadedPhotos = listOf(url) + (s.uploadedPhotos - url))
        }
    }

    fun uploadFromUri(context: android.content.Context, uri: android.net.Uri) {
        uploadManyFromUri(context, listOf(uri))
    }

    /** Upload satu atau banyak foto sekaligus (galeri multi-pilih) — berurutan, hormati maks 10. */
    fun uploadManyFromUri(context: android.content.Context, uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val failed = mutableListOf<String>()
            for (uri in uris) {
                if (_state.value.uploadedPhotos.size >= 10) break
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw com.lapakhunian.app.core.data.repository.ApiException("Gagal membaca file")
                    if (bytes.size > 10_000_000) {
                        failed.add("1 foto dilewati (>10MB)")
                        continue
                    }
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val name = "photo-${System.currentTimeMillis()}.jpg"
                    repo.uploadImage(bytes, name, mime).fold(
                        onSuccess = { url ->
                            _state.update { it.copy(uploadedPhotos = it.uploadedPhotos + url) }
                        },
                        onFailure = { e -> failed.add(e.message ?: "Gagal mengunggah") },
                    )
                } catch (e: Exception) {
                    failed.add(e.message ?: "Gagal membaca file")
                }
            }
            _state.update {
                it.copy(
                    uploading = false,
                    error = failed.takeIf { f -> f.isNotEmpty() }?.distinct()?.joinToString("; "),
                )
            }
        }
    }

    fun submit(body: Map<String, Any?>, onDone: () -> Unit) {
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val full = body.toMutableMap()
            full["images"] = _state.value.uploadedPhotos
            full["contactName"] = session.userFlow.value?.name ?: "Pemilik"
            full["contactEmail"] = session.userFlow.value?.email
            val role = session.userFlow.value?.role ?: "owner"
            full["listedBy"] = if (role == "agent") "agent" else "owner"
            val call = editListingId?.let { id -> repo.updateListing(id, full) } ?: repo.createListing(full)
            call.fold(
                onSuccess = {
                    _state.update { it.copy(submitting = false, success = true) }
                    onDone()
                },
                onFailure = { e -> _state.update { it.copy(submitting = false, error = e.message) } },
            )
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

// ================= Agent =================

data class AgentDashUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val notAgent: Boolean = false,
    val agent: AgentDto? = null,
    val subscription: com.lapakhunian.app.core.network.dto.SubscriptionDto? = null,
    val listings: List<Property> = emptyList(),
    val leadCountByProperty: Map<String, Int> = emptyMap(),
    val unreadChats: Int = 0,
    val compensations: List<com.lapakhunian.app.core.network.dto.UserHistoryItemDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class AgentDashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val chatRepo: com.lapakhunian.app.core.data.repository.ChatRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentDashUiState())
    val state: StateFlow<AgentDashUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null, notAgent = false) }
        viewModelScope.launch {
            repo.agentProfile().fold(
                onSuccess = { agent ->
                    // data pendukung diambil paralel; gagal salah satu tidak menggagalkan dashboard
                    val subDeferred = async { repo.subscription().getOrNull() }
                    val listingsDeferred = async { repo.myListings().getOrNull().orEmpty() }
                    val leadWaDeferred = async { repo.leadWaList().getOrNull().orEmpty() }
                    val unreadDeferred = async {
                        chatRepo.conversations().first().fold(
                            onSuccess = { list -> list.sumOf { c -> c.unread ?: 0 } },
                            onFailure = { 0 },
                        )
                    }
                    val compsDeferred = async {
                        repo.userHistory().getOrNull().orEmpty().filter { it.type == "compensation" }
                    }

                    val listings = listingsDeferred.await()
                    val leadCount = leadWaDeferred.await()
                        .mapNotNull { it.propertyId }
                        .groupingBy { it }
                        .eachCount()

                    _state.update {
                        it.copy(
                            loading = false,
                            agent = agent,
                            subscription = subDeferred.await(),
                            listings = listings.take(3),
                            leadCountByProperty = leadCount,
                            unreadChats = unreadDeferred.await(),
                            compensations = compsDeferred.await(),
                        )
                    }
                },
                onFailure = { e ->
                    val isForbidden = e.message?.contains("agent", ignoreCase = true) == true
                    _state.update {
                        it.copy(
                            loading = false,
                            notAgent = isForbidden,
                            error = if (!isForbidden) e.message else null,
                        )
                    }
                },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }
}

data class AgentLeadsUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val notAgent: Boolean = false,
    val data: AgentLeadsDto? = null,
    val leadWa: List<com.lapakhunian.app.core.network.dto.LeadWaDto> = emptyList(),
    val unlockingId: String? = null,
    val error: String? = null,
    val purchasing: Boolean = false,
)

@HiltViewModel
class AgentLeadsViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentLeadsUiState())
    val state: StateFlow<AgentLeadsUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null, notAgent = false) }
        viewModelScope.launch {
            repo.agentLeads().fold(
                onSuccess = { data ->
                    val leadWa = repo.leadWaList().getOrNull().orEmpty()
                    _state.update { it.copy(loading = false, data = data, leadWa = leadWa) }
                },
                onFailure = { e ->
                    val isForbidden = e.message?.contains("agent", ignoreCase = true) == true
                    _state.update { it.copy(loading = false, notAgent = isForbidden, error = if (!isForbidden) e.message else null) }
                },
            )
        }
    }

    /** Buka nomor lead WhatsApp — potong 1 saldo lead (sesuai referensi "Buka · 1 Lead"). */
    fun unlockLeadWa(id: String) {
        _state.update { it.copy(unlockingId = id, error = null) }
        viewModelScope.launch {
            repo.unlockLeadWa(id).fold(
                onSuccess = { load() },
                onFailure = { e -> _state.update { it.copy(unlockingId = null, error = e.message) } },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }

    /** Beli paket lead: buat purchase pending lalu minta URL bayar, buka PaymentScreen. */
    fun purchaseLeadPackage(packageId: String, onPay: (String) -> Unit) {
        _state.update { it.copy(purchasing = true, error = null) }
        viewModelScope.launch {
            repo.purchaseLeadPackage(packageId).fold(
                onSuccess = { purchase ->
                    val id = purchase.id
                    if (id == null) {
                        _state.update { it.copy(purchasing = false, error = "Respon tidak valid") }
                    } else {
                        repo.payLeadPurchase(id).fold(
                            onSuccess = {
                                _state.update { it.copy(purchasing = false) }
                                load()
                                onPay(id)
                            },
                            onFailure = { e -> _state.update { it.copy(purchasing = false, error = e.message) } },
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(purchasing = false, error = e.message) } },
            )
        }
    }

    /** Purchase pending: minta ulang URL pembayaran (idempotent di server). */
    fun payPendingPurchase(purchaseId: String, onPay: (String) -> Unit) {
        _state.update { it.copy(purchasing = true, error = null) }
        viewModelScope.launch {
            repo.payLeadPurchase(purchaseId).fold(
                onSuccess = {
                    _state.update { it.copy(purchasing = false) }
                    onPay(purchaseId)
                },
                onFailure = { e -> _state.update { it.copy(purchasing = false, error = e.message) } },
            )
        }
    }
}

@HiltViewModel
class AgentPlansViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    data class PlansUiState(
        val loading: Boolean = true,
        val needsLogin: Boolean = false,
        val plans: List<PlanDto> = emptyList(),
        val subscription: com.lapakhunian.app.core.network.dto.SubscriptionDto? = null,
        val error: String? = null,
        val subscribingPlanId: String? = null,
        val paymentUrl: String? = null,
        val voucherCode: String = "",
        val successMessage: String? = null,
    )

    private val _state = MutableStateFlow(PlansUiState())
    val state: StateFlow<PlansUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val subDeferred = async { repo.subscription().getOrNull() }
            repo.agentPlans().fold(
                onSuccess = { plans ->
                    _state.update { it.copy(loading = false, plans = plans, subscription = subDeferred.await()) }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.message, subscription = subDeferred.await()) }
                },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }

    fun updateVoucherCode(text: String) {
        _state.update { it.copy(voucherCode = text.uppercase().take(20)) }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    fun subscribe(planId: String) {
        _state.update { it.copy(subscribingPlanId = planId, error = null, successMessage = null) }
        viewModelScope.launch {
            repo.subscribePlan(planId, _state.value.voucherCode.trim().ifBlank { null }).fold(
                onSuccess = { outcome ->
                    if (outcome.sub.status == "active") {
                        // voucher menutup seluruh biaya / paket gratis → langsung aktif, tidak ada pembayaran
                        _state.update {
                            it.copy(
                                subscribingPlanId = null,
                                successMessage = outcome.message ?: "Berlangganan berhasil — paket sekarang aktif!",
                                voucherCode = "",
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(subscribingPlanId = null, paymentUrl = outcome.sub.paymentRedirectUrl)
                        }
                    }
                },
                onFailure = { e -> _state.update { it.copy(subscribingPlanId = null, error = e.message) } },
            )
        }
    }

    fun clearPaymentUrl() {
        _state.update { it.copy(paymentUrl = null) }
    }
}

/** Baris riwayat tergabung: komisi properti + pembelian paket lead + klaim voucher (sesuai referensi). */
data class HistoryRow(
    val key: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val status: String?,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val needsLogin: Boolean = false,
    val rows: List<HistoryRow> = emptyList(),
    val error: String? = null,
)

private fun normalizeHistoryStatus(raw: String?): String? = when (raw?.uppercase()) {
    "PAID", "COMPLETED", "SETTLED", "SUCCESS" -> "LUNAS"
    "PENDING", "PROCESSING", "EXPIRED" -> "TAGIHAN"
    null -> null
    else -> raw.uppercase()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { viewModelScope.launch { load() } }

    suspend fun load() {
        if (session.token() == null) {
            _state.update { it.copy(loading = false, needsLogin = true) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val historyDeferred = async { repo.userHistory().getOrNull().orEmpty() }
            repo.transactions().fold(
                onSuccess = { txs ->
                    val rows = buildList {
                        // Komisi properti (/api/transactions)
                        txs.forEach { tx ->
                            add(
                                HistoryRow(
                                    key = "tx-${tx.id ?: tx.hashCode()}",
                                    icon = com.lapakhunian.app.core.designsystem.AppIcons.Receipt,
                                    title = "Komisi penjualan",
                                    subtitle = listOfNotNull(tx.property?.title, tx.createdAt?.take(10)).joinToString(" · "),
                                    amount = com.lapakhunian.app.core.util.formatRupiah(tx.commission?.toLong() ?: tx.price ?: 0),
                                    status = normalizeHistoryStatus(tx.status),
                                ),
                            )
                        }
                        // Pembelian paket lead + klaim voucher (/api/user/history)
                        historyDeferred.await().forEach { h ->
                            when (h.type) {
                                "purchase" -> add(
                                    HistoryRow(
                                        key = "h-${h.id ?: h.hashCode()}",
                                        icon = com.lapakhunian.app.core.designsystem.AppIcons.Wallet,
                                        title = h.title ?: "Pembelian Paket Lead",
                                        subtitle = listOfNotNull(h.description, h.date?.take(10)).joinToString(" · "),
                                        amount = h.amount.orEmpty(),
                                        status = normalizeHistoryStatus(h.status),
                                    ),
                                )
                                "claim" -> add(
                                    HistoryRow(
                                        key = "h-${h.id ?: h.hashCode()}",
                                        icon = com.lapakhunian.app.core.designsystem.AppIcons.Ticket,
                                        title = h.title ?: "Voucher diklaim",
                                        subtitle = listOfNotNull(h.description, h.date?.take(10)).joinToString(" · "),
                                        amount = h.amount.orEmpty(),
                                        status = normalizeHistoryStatus(h.status),
                                    ),
                                )
                            }
                        }
                    }
                    _state.update { it.copy(loading = false, rows = rows) }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.message) }
                },
            )
        }
    }

    fun retry() { viewModelScope.launch { load() } }
}
