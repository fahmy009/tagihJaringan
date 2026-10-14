package com.fahms.tagihjaringan.ui.dashboard.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fahms.tagihjaringan.data.local.UserSettings
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentSettings: UserSettings,
    onSaveSettings: (UserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    fun formatDots(num: Int): String {
        if (num == 0) return ""
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(num)
    }

    var defaultNominalText by remember(currentSettings.defaultNominal) {
        mutableStateOf(formatDots(currentSettings.defaultNominal))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚙️ Pengaturan Tagihan & Router",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "🌐 Pengelolaan Router MikroTik",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Seluruh IP, Username, dan Password router MikroTik kini dikelola terpusat pada tombol 🌐 Kelola Router di Top Bar.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            }

            OutlinedTextField(
                value = defaultNominalText,
                onValueChange = { input ->
                    val cleanStr = input.replace(".", "").replace(",", "").replace(" ", "").trim()
                    val parsed = cleanStr.toIntOrNull()
                    defaultNominalText = if (parsed != null && parsed > 0) NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed) else ""
                },
                label = { Text("Nominal Default Tagihan Pelanggan Baru (Rp)") },
                placeholder = { Text("95.000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val cleanText = defaultNominalText.replace(".", "").replace(",", "").replace(" ", "").trim()
                    val parsedNominal = cleanText.toIntOrNull() ?: 95000

                    onSaveSettings(
                        currentSettings.copy(defaultNominal = parsedNominal)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text(
                    text = "Simpan Pengaturan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
