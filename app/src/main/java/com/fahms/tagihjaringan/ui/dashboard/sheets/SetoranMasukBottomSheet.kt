package com.fahms.tagihjaringan.ui.dashboard.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fahms.tagihjaringan.data.model.SetoranItem
import com.fahms.tagihjaringan.ui.theme.PriceGreen
import com.fahms.tagihjaringan.ui.theme.RedDanger
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextMuted
import com.fahms.tagihjaringan.ui.theme.WarningYellow
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetoranMasukBottomSheet(
    setoranList: List<SetoranItem>,
    userRole: String = "ADMIN",
    userAssignedProfile: String = "",
    userAccountName: String = "",
    onVerifySetoran: (setoranId: String, amountReceived: Int, selisihHutang: Int) -> Unit,
    onDeleteSetoran: (setoranId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isNonAdmin = userRole.uppercase() != "ADMIN" && userAssignedProfile.isNotBlank()
    val allowedProfiles = remember(userAssignedProfile) {
        userAssignedProfile.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    val filteredSetoranList = remember(setoranList, isNonAdmin, allowedProfiles, userAccountName) {
        if (isNonAdmin) {
            setoranList.filter { item ->
                allowedProfiles.any { p -> item.profile.equals(p, ignoreCase = true) } ||
                (userAccountName.isNotBlank() && item.mitraName.equals(userAccountName, ignoreCase = true))
            }
        } else {
            setoranList
        }
    }

    fun formatRupiah(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    var selectedSetoranForVerify by remember { mutableStateOf<SetoranItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isNonAdmin) "📥 Setoran Masuk ($userAssignedProfile)" else "📥 Setoran Masuk Keseluruhan",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            if (filteredSetoranList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada riwayat setoran masuk untuk wilayah ini.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSetoranList, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.status == "VERIFIED") PriceGreen.copy(alpha = 0.1f) else WarningYellow.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.collectorName.ifBlank { "Tukang Tagih" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        IconButton(
                                            onClick = { onDeleteSetoran(item.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Setoran",
                                                tint = RedDanger
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Tanggal: ${item.date} • Profil: ${item.profile.ifBlank { "Semua" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                    Text(
                                        text = "Diajukan Setor: ${formatRupiah(item.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )

                                    if (item.status == "VERIFIED") {
                                        Text(
                                            text = "Diterima Fisik: ${formatRupiah(item.amountReceived)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = PriceGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        if (item.selisihHutang > 0) {
                                            Text(
                                                text = "⚠️ Hutang Penagih: ${formatRupiah(item.selisihHutang)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = RedDanger,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    if (item.status == "VERIFIED") {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PriceGreen
                                        ) {
                                            Text(
                                                text = "TERIMA",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        if (item.selisihHutang > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TextButton(
                                                onClick = { selectedSetoranForVerify = item },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Pelunasan",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = SkySecondary
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Pelunasan",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = SkySecondary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { selectedSetoranForVerify = item },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SkySecondary)
                                        ) {
                                            Text("✓ Terima", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedSetoranForVerify != null) {
        VerifySetoranAmountDialog(
            setoranItem = selectedSetoranForVerify!!,
            onConfirmVerify = { rec, hutang ->
                val id = selectedSetoranForVerify!!.id
                selectedSetoranForVerify = null
                onVerifySetoran(id, rec, hutang)
            },
            onDismiss = { selectedSetoranForVerify = null }
        )
    }
}

@Composable
fun VerifySetoranAmountDialog(
    setoranItem: SetoranItem,
    onConfirmVerify: (amountReceived: Int, selisihHutang: Int) -> Unit,
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

    var receivedInputText by remember { mutableStateOf(formatDots(setoranItem.amountReceived.takeIf { it > 0 } ?: setoranItem.amount)) }

    val amountReceived = remember(receivedInputText) {
        val cleanStr = receivedInputText.replace(".", "").replace(",", "").replace(" ", "").trim()
        cleanStr.toIntOrNull() ?: 0
    }

    val selisihHutang = remember(setoranItem.amount, amountReceived) {
        (setoranItem.amount - amountReceived).coerceAtLeast(0)
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
                    text = "📥 Terima & Pelunasan Setoran",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SkySecondary.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text("Tukang Tagih: ${setoranItem.collectorName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Diajukan Setor: ${formatRupiah(setoranItem.amount)}", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
                    }
                }

                OutlinedTextField(
                    value = receivedInputText,
                    onValueChange = { input ->
                        val cleanStr = input.replace(".", "").replace(",", "").replace(" ", "").trim()
                        val parsed = cleanStr.toIntOrNull()
                        receivedInputText = if (parsed != null && parsed > 0) NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed) else ""
                    },
                    label = { Text("Jumlah Uang Fisik Diterima (Rp)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (selisihHutang > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RedDanger.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text("⚠️ Belum Disetor / Selisih Uang:", style = MaterialTheme.typography.bodySmall.copy(color = RedDanger, fontWeight = FontWeight.Bold))
                            Text(
                                text = "${formatRupiah(selisihHutang)} (Tercatat sebagai Hutang Penagih)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PriceGreen.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = "✓ Uang Fisik Pas / Hutang Lunas (Rp0)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = PriceGreen, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(12.dp)
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
                            onConfirmVerify(amountReceived, selisihHutang)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PriceGreen)
                    ) {
                        Text("✓ Simpan", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
