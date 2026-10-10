package com.fahms.tagihjaringan.ui.dashboard.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.PriceGreen
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SubmitSetoranDialog(
    totalHasilTagihHariIni: Int,
    mitraTargetName: String,
    onSubmitSetoran: (mitraName: String, amount: Int, sisaPegangan: Int) -> Unit,
    onDismiss: () -> Unit
) {
    fun formatRupiah(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    fun formatDots(num: Int): String {
        if (num == 0) return ""
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(num)
    }

    var setoranText by remember { mutableStateOf(formatDots(totalHasilTagihHariIni)) }
    var mitraNameInput by remember { mutableStateOf(mitraTargetName.ifBlank { "Pemilik Profile" }) }

    val setoranAmount = remember(setoranText) {
        val cleanStr = setoranText.replace(".", "").replace(",", "").replace(" ", "").trim()
        cleanStr.toIntOrNull() ?: 0
    }

    val sisaPegangan = remember(totalHasilTagihHariIni, setoranAmount) {
        (totalHasilTagihHariIni - setoranAmount).coerceAtLeast(0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🤝 Setor Hasil Tagihan",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PriceGreen.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("Total Pegangan Hari Ini:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(
                            text = formatRupiah(totalHasilTagihHariIni),
                            style = MaterialTheme.typography.titleLarge.copy(color = PriceGreen, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                OutlinedTextField(
                    value = mitraNameInput,
                    onValueChange = { mitraNameInput = it },
                    label = { Text("Nama Pemilik Profile / Mitra") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = setoranText,
                    onValueChange = { input ->
                        val cleanStr = input.replace(".", "").replace(",", "").replace(" ", "").trim()
                        val parsed = cleanStr.toIntOrNull()
                        setoranText = if (parsed != null && parsed > 0) NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed) else ""
                    },
                    label = { Text("Nominal Uang Disetorkan (Rp)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SkySecondary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("Sisa Pegangan Setelah Setor:", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(
                            text = formatRupiah(sisaPegangan),
                            style = MaterialTheme.typography.titleLarge.copy(color = SkySecondary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (setoranAmount > 0) {
                                onSubmitSetoran(mitraNameInput.trim(), setoranAmount, sisaPegangan)
                            }
                        },
                        enabled = setoranAmount > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                    ) {
                        Text("Konfirmasi Setor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
