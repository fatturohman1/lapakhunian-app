package com.lapakhunian.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.GreenGradient
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.designsystem.components.EmptyState
import com.lapakhunian.app.core.designsystem.components.InitialAvatar
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorWeight
import com.lapakhunian.app.core.network.dto.ConversationDto
import com.lapakhunian.app.core.network.dto.LeadWaDto
import com.lapakhunian.app.core.util.RoleHolder
import com.lapakhunian.app.core.util.toWaNumber
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatListScreen(
    onOpenThread: (String) -> Unit,
    onGoLogin: () -> Unit,
    onOpenLeadWa: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Tab Chat terlihat → polling badge 5 dtk; tab ditutup → VM tetap hidup untuk badge (30 dtk)
    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.setScreenVisible(true)
        onDispose { viewModel.setScreenVisible(false) }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Chat",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
        when {
            state.needsLogin -> EmptyState(
                title = "Masuk dulu ya",
                message = "Login untuk melihat percakapan dengan agen.",
                icon = AppIcons.Person,
                actionLabel = "Masuk",
                onAction = onGoLogin,
            )
            state.loading -> Column(Modifier.fillMaxSize()) {
                repeat(3) { ThreadSkeleton() }
            }
            state.error != null && state.items.isEmpty() && state.waLeads.isEmpty() -> EmptyState(
                title = "Gagal memuat",
                message = state.error ?: "Periksa koneksi internet kamu lalu coba lagi.",
                icon = AppIcons.Chat,
            )
            state.items.isEmpty() && state.waLeads.isEmpty() -> EmptyState(
                title = "Belum ada percakapan",
                message = "Hubungi agen dari halaman properti untuk mulai chat.",
                icon = AppIcons.Chat,
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(state.items, key = { idx, item -> item.id ?: "conv-$idx" }) { _, conversation ->
                    ConversationItem(conversation) { conversation.id?.let(onOpenThread) }
                }
                if (state.waLeads.isNotEmpty()) {
                    item(key = "lead-wa-header") {
                        Text(
                            "Lead WhatsApp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W800,
                            color = MutedGray,
                            modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(state.waLeads, key = { "lead-wa-${it.id ?: it.hashCode()}" }) { lead ->
                        WaLeadChatItem(
                            lead = lead,
                            onClick = {
                                if (lead.unlocked == true) {
                                    val number = toWaNumber(lead.phone.orEmpty())
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")),
                                        )
                                    }
                                } else {
                                    onOpenLeadWa()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Box(Modifier.widthIn(max = 140.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 220.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun ConversationItem(conversation: ConversationDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialAvatar(initials = (conversation.contactName ?: "A").take(2), size = 46)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.contactName ?: "Agen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (conversation.online == true) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF15803D)))
                    Text("Online", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Text(
                text = conversation.lastMessageText?.takeIf { it.isNotBlank() } ?: "Belum ada pesan",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = conversation.subject ?: "",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.W700,
                color = PrimaryDark,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(TintGreen, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            Text(
                formatChatTime(conversation.lastTime),
                fontSize = 11.sp,
                color = MutedGray,
                fontWeight = FontWeight.W600,
            )
            val unread = conversation.unread ?: 0
            if (unread > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text("$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.W800)
                }
            }
        }
    }
}

/** Lead WhatsApp di daftar chat — avatar glyph WA sebagai penanda sumber + status buka/kunci. */
@Composable
private fun WaLeadChatItem(lead: LeadWaDto, onClick: () -> Unit) {
    val unlocked = lead.unlocked == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // avatar hijau WhatsApp — penanda sumber chat
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF25D366)),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(AppIcons.WhatsApp, tint = Color.White, size = 24.dp, contentDescription = "WhatsApp")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lead.name ?: "Calon pembeli",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (unlocked) "Terbuka" else "Terkunci",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W800,
                    color = if (unlocked) Color(0xFF15803D) else Color(0xFFB45309),
                )
            }
            Text(
                text = lead.message?.takeIf { it.isNotBlank() } ?: "Pesan baru dari WhatsApp",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = lead.propertyTitle ?: "Lead WhatsApp",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.W700,
                color = PrimaryDark,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(TintGreen, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatChatTime(lead.createdAt),
            fontSize = 11.sp,
            color = MutedGray,
            fontWeight = FontWeight.W600,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private val chatDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID"))

/** Hari ini → HH:mm, kemarin → "Kemarin", selainnya → d MMM (locale Indonesia). */
private fun formatChatTime(iso: String?): String = runCatching {
    val dt = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
    val today = LocalDate.now()
    when (dt.toLocalDate()) {
        today -> dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        today.minusDays(1) -> "Kemarin"
        else -> dt.format(chatDayFormat)
    }
}.getOrDefault("")

@Composable
fun ThreadScreen(
    onBack: () -> Unit,
    viewModel: ThreadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDispute by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Seluruh layar naik bersama keyboard (pola resmi Compose): header tetap
            // terlihat, pesan selalu menempel di atas kotak ketik. Navbar sudah
            // "dikonsumsi" parent (padding Scaffold) — konsumsi manual agar imePadding
            // tepat di atas keyboard tanpa dobel gap (bug di sebagian perangkat).
            .consumeWindowInsets(WindowInsets.navigationBars)
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            InitialAvatar(initials = (state.conversation?.contactName ?: "A").take(2), size = 38, modifier = Modifier.padding(start = 2.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    state.conversation?.contactName ?: "Percakapan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W800,
                )
                if (state.conversation?.online == true) {
                    Text("Online", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), fontWeight = FontWeight.W700)
                }
            }
            // Lead dispute — hanya agen & chat sudah terbuka (lead terpakai)
            if (RoleHolder.role == "agent" && !state.locked) {
                androidx.compose.material3.IconButton(onClick = { showDispute = true }) {
                    PhIcon(
                        AppIcons.Flag,
                        contentDescription = "Laporkan lead invalid",
                        tint = MutedGray,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Messages — reverseLayout (index 0 = pesan terbaru) + alignment Bottom:
        // konten selalu menempel di atas kotak ketik — keyboard membuka/menutup
        // tidak pernah menggeser pesan ke atas.
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            reverseLayout = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.Bottom),
        ) {
            // Urutan DSL = dari bawah ke atas layar (karena reverseLayout)
            if (state.locked) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Percakapan terkunci", fontWeight = FontWeight.W800)
                        Text(
                            "Buka chat ini dengan 1 lead untuk melihat & membalas pesan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Button(
                            onClick = { viewModel.unlock() },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(top = 12.dp),
                        ) { Text("Buka Chat — 1 Lead", fontWeight = FontWeight.W700) }
                    }
                }
            }
            itemsIndexed(state.failedMessages.asReversed(), key = { idx, msg -> "failed-${msg.id ?: idx}" }) { _, message ->
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    MessageBubble(message, status = MessageTick.Failed)
                    Text("Gagal dikirim", color = ErrorRed, style = MaterialTheme.typography.labelMedium)
                    TextButton(
                        enabled = !state.sending && !state.locked,
                        onClick = { message.id?.let(viewModel::retryFailed) },
                    ) { Text("Coba kirim lagi") }
                }
            }
            itemsIndexed(state.messages.asReversed(), key = { idx, msg -> msg.id ?: "msg-$idx" }) { _, message ->
                MessageBubble(
                    message,
                    status = when {
                        message.id?.startsWith("temp-") == true -> MessageTick.Sending
                        message.read == true -> MessageTick.Read
                        else -> MessageTick.Sent
                    },
                )
            }
            state.error?.let { message ->
                item { Text(message, color = ErrorRed, style = MaterialTheme.typography.labelMedium) }
            }
            if (state.loading && state.messages.isEmpty()) {
                item { ThreadSkeleton() }
            }
        }

        // Input (disable saat locked) — imePadding sudah di root column
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = state.draft,
                onValueChange = { viewModel.updateDraft(it) },
                placeholder = { Text("Tulis pesan…", color = MutedGray) },
                enabled = !state.locked,
                shape = CircleShape,
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(46.dp)
                    .background(
                        // abu saat pesan kosong/terkunci, gradasi hijau saat siap kirim
                        if (state.locked || state.sending || state.draft.isBlank()) {
                            androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            GreenGradient
                        },
                        CircleShape,
                    )
                    .clickable(enabled = !state.locked && !state.sending && state.draft.isNotBlank()) {
                        viewModel.send(state.draft)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.sending) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    PhIcon(AppIcons.Send,
                        contentDescription = "Kirim",
                        tint = if (state.locked || state.draft.isBlank()) MutedGray else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    // Dialog lead dispute
    if (showDispute) {
        AlertDialog(
            onDismissRequest = { showDispute = false },
            title = { Text("Laporkan lead invalid?", fontWeight = FontWeight.W800) },
            text = {
                Column {
                    Text(
                        "Laporan direview admin dalam 1×24 jam. Bila disetujui, lead dikembalikan (refund).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    OutlinedTextField(
                        value = disputeReason,
                        onValueChange = { disputeReason = it },
                        label = { Text("Alasan (mis. nomor salah, tidak serius)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = disputeReason.isNotBlank() && !state.disputing,
                    onClick = {
                        viewModel.disputeLead(disputeReason) { res ->
                            showDispute = false
                            disputeReason = ""
                            Toast.makeText(
                                context,
                                res.fold(
                                    { it },
                                    { e -> e.message ?: "Gagal mengirim laporan" },
                                ),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                ) { Text("Kirim") }
            },
            dismissButton = {
                TextButton(onClick = { showDispute = false }) { Text("Batal") }
            },
        )
    }
}

/** Status pesanku ala WhatsApp: jam = belum terkirim, ceklis = terkirim, ceklis 2 = dibaca. */
enum class MessageTick { Sending, Failed, Sent, Read }

@Composable
private fun MessageBubble(
    message: com.lapakhunian.app.core.network.dto.MessageDto,
    status: MessageTick? = null,
) {
    val fromMe = message.fromMe == true
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    if (fromMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                    else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    message.content.orEmpty(),
                    color = if (fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatChatTime(message.createdAt),
                        fontSize = 10.sp,
                        color = if (fromMe) Color.White.copy(alpha = 0.75f) else MutedGray,
                        fontWeight = FontWeight.W600,
                    )
                    if (fromMe && status != null) {
                        PhIcon(
                            when (status) {
                                MessageTick.Sending, MessageTick.Failed -> AppIcons.Clock
                                MessageTick.Sent -> AppIcons.Check
                                MessageTick.Read -> AppIcons.DoneAll
                            },
                            contentDescription = when (status) {
                                MessageTick.Sending -> "Sedang mengirim"
                                MessageTick.Failed -> "Gagal terkirim"
                                MessageTick.Sent -> "Terkirim"
                                MessageTick.Read -> "Dibaca"
                            },
                            tint = when (status) {
                                MessageTick.Failed -> ErrorRed
                                // biru muda ala read receipt WhatsApp agar beda dari "terkirim"
                                MessageTick.Read -> Color(0xFF8FD8FF)
                                else -> Color.White.copy(alpha = 0.75f)
                            },
                            weight = PhosphorWeight.REGULAR,
                            size = 12.dp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
