package com.lapakhunian.app.core.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lapakhunian.app.BuildConfig
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.util.UpdateChecker
import androidx.lifecycle.repeatOnLifecycle

internal class UpdateCheckPolicy(
    private val prefs: android.content.SharedPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
    private val check: suspend () -> com.lapakhunian.app.core.util.UpdateInfo? = UpdateChecker::check,
) {
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun checkIfDue(): com.lapakhunian.app.core.util.UpdateInfo? {
        if (!mutex.tryLock()) return null
        try {
            val now = clock()
            val success = prefs.getLong("last_successful_check", 0L)
            val attempt = prefs.getLong("last_attempt_check", 0L)
            if (success > 0 && now >= success && now - success < 86_400_000L) return null
            if (attempt > 0 && now >= attempt && now - attempt < 60_000L) return null
            prefs.edit().putLong("last_attempt_check", now).apply()
            return try {
                val latest = check()
                prefs.edit().putLong("last_successful_check", clock()).apply()
                latest
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        } finally {
            mutex.unlock()
        }
    }
}

@Composable
fun AutoUpdatePrompt() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val policy = remember(context) { UpdateCheckPolicy(context.getSharedPreferences("lapak_update", Context.MODE_PRIVATE)) }
    var updateInfo by remember { mutableStateOf<com.lapakhunian.app.core.util.UpdateInfo?>(null) }

    LaunchedEffect(lifecycleOwner, policy) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            policy.checkIfDue()?.let { updateInfo = it }
        }
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(version = info.version, notes = info.notes, onDismiss = { updateInfo = null })
    }
}

@Composable
fun UpdateAvailableDialog(version: String, notes: String = "", onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update tersedia", fontWeight = FontWeight.W800) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            ) {
                Text("Versi $version sudah tersedia. Kamu memakai ${BuildConfig.VERSION_NAME}.")
                if (notes.isNotBlank()) {
                    Text(
                        "Yang baru:",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.W800,
                        modifier = androidx.compose.ui.Modifier.padding(top = 12.dp),
                    )
                    Text(
                        notes,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        modifier = androidx.compose.ui.Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    "Cara pasang:",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.W800,
                    modifier = androidx.compose.ui.Modifier.padding(top = 12.dp),
                )
                Text(
                    "Unduh APK lewat browser, buka file unduhannya, izinkan pemasangan dari sumber ini bila diminta, lalu ketuk Pasang. Data dan sesi kamu tetap aman.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = androidx.compose.ui.Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.DOWNLOAD_URL)))
                }.onFailure {
                    Toast.makeText(context, "Tidak bisa membuka browser", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Unduh Update", color = PrimaryDark, fontWeight = FontWeight.W700)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti")
            }
        },
    )
}