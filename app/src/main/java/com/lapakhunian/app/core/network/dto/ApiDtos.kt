package com.lapakhunian.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response envelope standar web: { success, data, error, meta } */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null,
    val meta: ApiMeta? = null,
)

@Serializable
data class ApiMeta(
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val totalPages: Int? = null,
)

/** Banner promo aktif dari admin (GET /api/banners) — imageUrl bisa relative (/uploads/...). */
@Serializable
data class BannerDto(
    val id: String? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val position: String? = null,
)

/** Tiket bantuan milik user (GET /api/support/tickets). */
@Serializable
data class SupportTicketDto(
    val id: String? = null,
    val subject: String? = null,
    val message: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

/** Body POST /api/titip-jual — typed (jangan Map<String, Any?>: List<String> dalam map heterogen gagal diserialisasi kotlinx). */
@Serializable
data class TitipJualRequest(
    val name: String,
    val phone: String,
    val propertyTitle: String,
    val address: String,
    val estimatedPrice: String,
    val photos: List<String> = emptyList(),
)

@Serializable
data class PropertyDto(
    val id: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val price: Long? = null,
    val address: String? = null,
    val city: String? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val squareFeet: Int? = null,
    val buildingArea: Int? = null,
    val floors: Int? = null,
    val carport: Int? = null,
    val certificate: String? = null,
    val electricity: String? = null,
    val furnishing: String? = null,
    val yearBuilt: Int? = null,
    val category: String? = null,
    val purpose: String? = null,
    val images: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val listedBy: String? = null,
    val contactName: String? = null,
    @SerialName("contactPhone") val contactPhone: String? = null,
    val status: String? = null,
    val isFeatured: Boolean? = null,
    val isActive: Boolean? = null,
    val isSold: Boolean? = null,
    val viewCount: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val agentName: String? = null,
    val agentSlug: String? = null,
    val agentRating: Double? = null,
    val createdAt: String? = null,
)

/** Item daftar favorit — respons web berbentuk nested: { id, userId, property: {...} }. */
@Serializable
data class FavoriteItemDto(
    val id: String? = null,
    val property: PropertyDto? = null,
)

@Serializable
data class UserDto(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val image: String? = null,
    val subscriptionTier: String? = null,
)

@Serializable
data class LoginResponseDto(
    val user: UserDto? = null,
    val sessionToken: String? = null,
    /** Akun Google baru yang belum memilih role (owner/agent) → tampilkan role picker. */
    val needsOnboarding: Boolean? = null,
)

@Serializable
data class RegisterResponseDto(
    val requiresVerification: Boolean? = null,
)

@Serializable
data class ContactResponseDto(
    val conversationId: String? = null,
    val leadCost: Int? = null,
    val deducted: Int? = null,
    val hasUnlimitedChat: Boolean? = null,
    val message: String? = null,
)

/** Respon guest-contact (tanpa login) — hanya success yang dipakai UI; field lain opsional. */
@Serializable
data class GuestContactResponseDto(
    val leadId: String? = null,
    val agentName: String? = null,
)

@Serializable
data class ConversationDto(
    val id: String? = null,
    val subject: String? = null,
    val contactName: String? = null,
    val contactImage: String? = null,
    val lastMessage: LastMessageDto? = null,
    val lastMessageText: String? = null,
    val lastTime: String? = null,
    val unread: Int? = null,
    val online: Boolean? = null,
    val locked: Boolean? = null,
    val unlockedAt: String? = null,
)

@Serializable
data class LastMessageDto(
    val id: String? = null,
    val content: String? = null,
    val senderId: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class MessageDto(
    val id: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
    val fromMe: Boolean? = null,
    val locked: Boolean? = null,
    val sender: MessageSenderDto? = null,
)

@Serializable
data class MessageSenderDto(
    val id: String? = null,
    val name: String? = null,
    val image: String? = null,
)

@Serializable
data class MessagesEnvelope(
    val success: Boolean = false,
    val data: List<MessageDto> = emptyList(),
    val error: String? = null,
    val locked: Boolean? = null,
)

@Serializable
data class NotificationDto(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val message: String? = null,
    val link: String? = null,
    val isRead: Boolean? = null,
    val createdAt: String? = null,
)

// ===== Dashboard (owner/agent) =====

@Serializable
data class UploadResponse(
    val success: Boolean = false,
    val url: String? = null,
    val error: String? = null,
)

@Serializable
data class SubscriptionDto(
    val tier: String? = null,
    val label: String? = null,
    val isActive: Boolean? = null,
    val trialActive: Boolean? = null,
    val daysLeft: Int? = null,
    val maxProperties: Int? = null,
    val currentProperties: Int? = null,
    val price: Long? = null,
)

@Serializable
data class AgentDto(
    val id: String? = null,
    val slug: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val image: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val licenseNumber: String? = null,
    val propertiesCount: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val profileTier: String? = null,
    val leadBalance: Int? = null,
    val totalLeadsPurchased: Int? = null,
    val totalLeadsUsed: Int? = null,
    val bonusPropertySlots: Int? = null,
    val isFrozen: Boolean? = null,
)

@Serializable
data class AgentLeadsDto(
    val currentBalance: Int? = null,
    val totalPurchased: Int? = null,
    val totalUsed: Int? = null,
    val packages: List<LeadPackageDto> = emptyList(),
    val purchases: List<LeadPurchaseDto> = emptyList(),
    val pendingCount: Int? = null,
)

@Serializable
data class LeadPackageDto(
    val id: String? = null,
    val name: String? = null,
    val amount: Int? = null,
    val price: Long? = null,
)

@Serializable
data class LeadPurchaseDto(
    val id: String? = null,
    val amount: Int? = null,
    val status: String? = null,
    val createdAt: String? = null,
    @SerialName("package") val packageInfo: LeadPackageDto? = null,
)

@Serializable
data class PlanDto(
    val id: String? = null,
    val name: String? = null,
    val price: Long? = null,
    val propertySlots: Int? = null,
    val monthlyLeads: Int? = null,
    val durationDays: Int? = null,
    val unlimitedChat: Boolean? = null,
    val isFeatured: Boolean? = null,
)

@Serializable
data class SubscribeResponseDto(
    val paymentToken: String? = null,
    val paymentRedirectUrl: String? = null,
    val orderId: String? = null,
    val status: String? = null,
    val subscriptionId: String? = null,
)

@Serializable
data class TransactionDto(
    val id: String? = null,
    val propertyId: String? = null,
    val status: String? = null,
    val commission: Double? = null,
    val price: Long? = null,
    val createdAt: String? = null,
    val property: TxPropertyDto? = null,
)

@Serializable
data class TxPropertyDto(
    val id: String? = null,
    val title: String? = null,
    val price: Long? = null,
)

@Serializable
data class LeadWaDto(
    val id: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val message: String? = null,
    val budget: Long? = null,
    val timeline: String? = null,
    val propertyId: String? = null,
    val propertyTitle: String? = null,
    val status: String? = null,
    val charged: Boolean? = null,
    val unlocked: Boolean? = null,
    val leadCost: Int? = null,
    val createdAt: String? = null,
)

@Serializable
data class LeadWaUnlockDto(
    val id: String? = null,
    val charged: Boolean? = null,
    val unlocked: Boolean? = null,
    val phone: String? = null,
)

@Serializable
data class VoucherDto(
    val id: String? = null,
    val title: String? = null,
    val type: String? = null,
)

@Serializable
data class VoucherClaimDto(
    val bonusLeads: Int? = null,
    val bonusProperties: Int? = null,
    val message: String? = null,
)

/** Item riwayat user (GET /api/user/history) — kompensasi leads/slot dari admin, dll. */
@Serializable
data class UserHistoryItemDto(
    val category: String? = null,
    val type: String? = null,
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val amount: String? = null,
    val date: String? = null,
    val status: String? = null,
)
