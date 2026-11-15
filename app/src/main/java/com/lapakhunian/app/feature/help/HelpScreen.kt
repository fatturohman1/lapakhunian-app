package com.lapakhunian.app.feature.help

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import android.widget.Toast
import com.lapakhunian.app.core.data.repository.ApiException
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import com.lapakhunian.app.core.network.dto.SupportTicketDto
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Faq(val question: String, val answer: String)

private val FaqList = listOf(
    Faq(
        "Bagaimana cara memasang iklan properti?",
        "Buka menu Profil → Pasang Iklan, lalu lengkapi formulir (judul, harga, spesifikasi, dan foto). Iklanmu akan tampil setelah publikasi.",
    ),
    Faq(
        "Apa itu sistem lead?",
        "Lead adalah potensi pembeli yang tertarik pada iklanmu. Untuk melihat nomor telepon pembeli atau membuka chat, agen memakai saldo lead yang bisa dibeli lewat paket lead.",
    ),
    Faq(
        "Kenapa percakapan saya terkunci?",
        "Percakapan dengan calon pembeli terkunci sampai dibuka dengan 1 lead. Setelah dibuka, kamu bisa melihat seluruh pesan dan membalasnya.",
    ),
    Faq(
        "Bagaimana cara mendapat lead WhatsApp?",
        "Calon pembeli yang menghubungi kamu melalui WhatsApp akan muncul di halaman Lead WA (Dashboard Agen) dan juga di bagian Chat dengan penanda WhatsApp.",
    ),
    Faq(
        "Bisakah saya mengedit atau menghapus iklan?",
        "Bisa. Buka Dashboard Saya (owner) atau Dashboard Agen, pilih iklan, lalu gunakan tombol edit atau perpanjang iklan.",
    ),
    Faq(
        "Apakah membuat akun gratis?",
        "Ya, membuat akun dan memasang iklan dasar gratis. Saldo lead untuk membuka nomor/chat dibeli terpisah melalui paket yang tersedia.",
    ),
)

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onGoLogin: () -> Unit = {},
    viewModel: HelpViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tickets by viewModel.state.collectAsState()
    var expanded by remember { mutableIntStateOf(-1) }
    var showTicketForm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackIconButton(onClick = onBack)
            Text("Bantuan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W800, modifier = Modifier.padding(start = 6.dp))
        }

        // Kontak cepat
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
        ) {
            Text("Butuh bantuan langsung?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W800)
            Text(
                "Tim kami siap membantu seputar iklan, lead, dan transaksi.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 14.dp)) {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@lapakhunian.com")))
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    PhIcon(AppIcons.Chat, tint = Color.White, size = 15.dp)
                    Text("Email Kami", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lapakhunian.com")))
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    PhIcon(AppIcons.Share, tint = PrimaryDark, size = 15.dp)
                    Text("Website", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
                }
            }
            // Tiket bantuan — keluhan/penjelasan tercatat & bisa dipantau statusnya
            Button(
                onClick = {
                    if (viewModel.isLoggedIn()) showTicketForm = true else onGoLogin()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TintGreen,
                    contentColor = PrimaryDark,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                PhIcon(AppIcons.Ticket, tint = PrimaryDark, size = 15.dp)
                Text("Buat Tiket Bantuan", fontWeight = FontWeight.W700, modifier = Modifier.padding(start = 6.dp))
            }
            tickets.items.forEach { ticket ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                ) {
                    Text(
                        ticket.subject ?: "(tanpa subjek)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W700,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            when (ticket.status) {
                                "in_progress" -> "Sedang diproses"
                                "resolved", "closed" -> "Selesai"
                                else -> "Menunggu review"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                        )
                        Text(
                            formatTicketDate(ticket.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                        )
                    }
                }
            }
        }

        // FAQ accordion
        Text(
            "Pertanyaan Umum",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W800,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        FaqList.forEachIndexed { index, faq ->
            val isOpen = expanded == index
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { expanded = if (isOpen) -1 else index }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        faq.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.W700,
                        modifier = Modifier.weight(1f),
                    )
                    PhIcon(
                        AppIcons.Chevron,
                        tint = MutedGray,
                        size = 16.dp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                AnimatedVisibility(visible = isOpen) {
                    Text(
                        faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Box(Modifier.size(1.dp))
    }

    // Form tiket bantuan
    if (showTicketForm) {
        AlertDialog(
            onDismissRequest = { showTicketForm = false },
            title = { Text("Buat Tiket Bantuan", fontWeight = FontWeight.W800) },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.ticketSubject,
                        onValueChange = { viewModel.ticketSubject = it },
                        label = { Text("Subjek") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = viewModel.ticketMessage,
                        onValueChange = { viewModel.ticketMessage = it },
                        label = { Text("Jelaskan masalah kamu") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = viewModel.ticketSubject.isNotBlank() &&
                        viewModel.ticketMessage.isNotBlank() &&
                        !tickets.creating,
                    onClick = {
                        viewModel.createTicket {
                            showTicketForm = false
                            Toast.makeText(context, "Tiket terkirim — kami akan segera menanggapi", Toast.LENGTH_LONG).show()
                        }
                    },
                ) { Text("Kirim") }
            },
            dismissButton = { TextButton(onClick = { showTicketForm = false }) { Text("Batal") } },
        )
    }
}

/** "d MMM yyyy" locale Indonesia — dipakai daftar tiket. */
private fun formatTicketDate(iso: String?): String = runCatching {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
}.getOrDefault("")

data class TicketUiState(
    val items: List<SupportTicketDto> = emptyList(),
    val creating: Boolean = false,
)

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val repo: PropertyRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TicketUiState())
    val state: StateFlow<TicketUiState> = _state.asStateFlow()

    var ticketSubject: String by mutableStateOf("")
    var ticketMessage: String by mutableStateOf("")

    fun isLoggedIn(): Boolean = session.tokenFlow.value != null

    fun load() {
        if (!isLoggedIn()) return
        viewModelScope.launch {
            repo.supportTickets().fold(
                onSuccess = { items ->
                    _state.update { it.copy(items = items.take(5)) }
                },
                onFailure = { /* non-kritis: daftar tiket kosong saja */ },
            )
        }
    }

    fun createTicket(onSuccess: () -> Unit) {
        if (_state.value.creating) return
        val subject = ticketSubject.trim()
        val message = ticketMessage.trim()
        if (subject.isBlank() || message.isBlank()) return
        _state.update { it.copy(creating = true) }
        viewModelScope.launch {
            repo.createSupportTicket(subject, message).fold(
                onSuccess = {
                    ticketSubject = ""
                    ticketMessage = ""
                    _state.update { it.copy(creating = false) }
                    load()
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update { it.copy(creating = false) }
                    Log.w("HelpScreen", "createTicket gagal", e)
                },
            )
        }
    }
}
