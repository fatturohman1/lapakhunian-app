package com.lapakhunian.app.feature.chat

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.components.BackIconButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactUiState(
    val sending: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val conversationId: String? = null,
)

@HiltViewModel
class ContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PropertyRepository,
) : ViewModel() {

    val propertyId: String = savedStateHandle.get<String>("propertyId").orEmpty()

    private val _state = MutableStateFlow(ContactUiState())
    val state: StateFlow<ContactUiState> = _state.asStateFlow()

    /** timeline values sesuai kontrak: soon / month1_3 / month3_6 / unsure */
    fun send(budgetText: String, timeline: String, message: String, onOpenThread: (String) -> Unit) {
        val budget = budgetText.replace(Regex("[^0-9]"), "").toLongOrNull()
            ?.takeIf { it > 0 }
        // Validasi ringan sebelum kirim: pesan maksimal 1000 karakter, budget > Rp100 miliar dianggap tidak wajar
        val trimmedMessage = message.trim()
        val budgetUnreasonable = budget != null && budget > 100_000_000_000L
        if (budgetUnreasonable || trimmedMessage.length > 1000) {
            _state.update {
                it.copy(
                    sending = false,
                    error = if (budgetUnreasonable) "Nominal budget tidak wajar" else "Pesan maksimal 1000 karakter",
                )
            }
            return
        }
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            repo.contactAgent(propertyId, budget, timeline, trimmedMessage.take(1000)).fold(
                onSuccess = { data ->
                    val conversationId = data.conversationId
                    if (conversationId != null) {
                        _state.update { it.copy(sending = false, success = true, conversationId = conversationId) }
                        onOpenThread(conversationId)
                    } else {
                        _state.update { it.copy(sending = false, success = true) }
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(sending = false, error = e.message ?: "Gagal mengirim") }
                },
            )
        }
    }
}

private val TIMELINES = listOf(
    "Segera" to "soon",
    "1–3 bulan" to "month1_3",
    "3–6 bulan" to "month3_6",
    "Belum pasti" to "unsure",
)

@Composable
fun ContactScreen(
    onBack: () -> Unit,
    onOpenThread: (String) -> Unit,
    viewModel: ContactViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var budget by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Halo, saya tertarik dengan properti ini. Apakah masih tersedia?") }
    var timeline by remember { mutableStateOf("soon") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.size(height = 24.dp, width = 1.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackIconButton(onClick = onBack)
            Text(
                "Hubungi Agen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W800,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        Text(
            "Formulir ini membuat percakapan dengan agen. Pesanmu langsung masuk ke chat — nomor kamu tetap terlindungi.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.padding(top = 12.dp),
        )

        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it },
            label = { Text("Kisaran budget (opsional)") },
            singleLine = true,
            placeholder = { Text("cth: 1200000000") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )

        Text(
            "Rencana beli",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.W700,
            modifier = Modifier.padding(top = 20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            TIMELINES.forEach { (label, value) ->
                val selected = timeline == value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) TintGreen else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { timeline = value }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        label,
                        fontWeight = FontWeight.W700,
                        color = if (selected) PrimaryDark else MutedGray,
                    )
                }
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Pesan") },
            minLines = 4,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        )

        state.error?.let { error ->
            Text(error, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
        }

        Button(
            onClick = { viewModel.send(budget, timeline, message) { onOpenThread(it) } },
            enabled = !state.sending && message.isNotBlank(),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 32.dp),
        ) {
            if (state.sending) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text("Kirim & Buka Chat", fontWeight = FontWeight.W700)
            }
        }
    }
}
