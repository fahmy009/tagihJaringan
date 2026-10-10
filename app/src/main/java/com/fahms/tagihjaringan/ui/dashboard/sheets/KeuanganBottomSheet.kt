package com.fahms.tagihjaringan.ui.dashboard.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahms.tagihjaringan.data.model.LaporanBulananItem
import com.fahms.tagihjaringan.data.model.PotonganItem
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.PriceGreen
import com.fahms.tagihjaringan.ui.theme.RedDanger
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeuanganBottomSheet(
    grossIncome: Int,
    initialPotongan: List<PotonganItem>,
    laporanBulananList: List<LaporanBulananItem> = emptyList(),
    userRole: String = "ADMIN",
    assignedProfile: String = "",
    onSavePotongan: (List<PotonganItem>) -> Unit,
    onTutupBukuClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val isNonAdmin = userRole.uppercase() != "ADMIN" && assignedProfile.isNotBlank()

    val profileFilteredPotongan = remember(initialPotongan, userRole, assignedProfile) {
        if (isNonAdmin) {
            initialPotongan.filter { it.profile.isBlank() || it.profile.equals(assignedProfile, ignoreCase = true) }
        } else {
            initialPotongan
        }
    }

    var potonganList by remember(profileFilteredPotongan) { mutableStateOf(profileFilteredPotongan.toMutableList()) }

    fun formatRupiah(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    fun formatDots(amount: Int): String {
        if (amount == 0) return ""
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)
    }

    val totalPotongan = potonganList.sumOf { it.amount }
    val netIncome = grossIncome - totalPotongan

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
                text = if (isNonAdmin) "👝 Laporan Keuangan ($assignedProfile)" else "👝 Laporan Keuangan Keseluruhan",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            // Gross Income Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PriceGreen.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pemasukan Kotor (Lunas):",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = formatRupiah(grossIncome),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = PriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Net Income Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SkySecondary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hasil Bersih:",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formatRupiah(netIncome),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = SkySecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Potongan Operasional",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(
                    onClick = {
                        val newList = potonganList.toMutableList()
                        newList.add(PotonganItem(name = "", amount = 0, profile = if (isNonAdmin) assignedProfile else ""))
                        potonganList = newList
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Potongan")
                }
            }

            // Dynamic list of Potongan
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(potonganList) { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { newName ->
                                val updated = potonganList.toMutableList()
                                updated[index] = item.copy(name = newName)
                                potonganList = updated
                            },
                            placeholder = { Text("Nama Potongan (mis. Internet)") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = formatDots(item.amount),
                            onValueChange = { newAmount ->
                                val cleanStr = newAmount.replace(".", "").replace(",", "").replace(" ", "").trim()
                                val parsed = cleanStr.toIntOrNull() ?: 0
                                val updated = potonganList.toMutableList()
                                updated[index] = item.copy(amount = parsed)
                                potonganList = updated
                            },
                            placeholder = { Text("Rp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        IconButton(
                            onClick = {
                                val updated = potonganList.toMutableList()
                                updated.removeAt(index)
                                potonganList = updated
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = RedDanger
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val otherProfilesPotongan = if (isNonAdmin) {
                        initialPotongan.filter { !it.profile.isBlank() && !it.profile.equals(assignedProfile, ignoreCase = true) }
                    } else {
                        emptyList()
                    }
                    val fullListToSave = otherProfilesPotongan + potonganList.map { p ->
                        if (p.profile.isBlank() && isNonAdmin) {
                            p.copy(profile = assignedProfile)
                        } else p
                    }
                    onSavePotongan(fullListToSave)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text("Simpan Potongan", fontWeight = FontWeight.Bold)
            }

            // Monthly Report History Section
            if (laporanBulananList.isNotEmpty()) {
                Text(
                    text = "📊 Riwayat & Analytics Bulanan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(laporanBulananList) { lap ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = lap.periode.ifBlank { "Laporan Bulanan" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${lap.countLunas} Pelanggan • ${lap.date}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Nett: ${formatRupiah(lap.netIncome)}",
                                        style = MaterialTheme.typography.titleSmall.copy(color = PriceGreen, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Gross: ${formatRupiah(lap.grossIncome)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Big Red Button Tutup Buku
            Button(
                onClick = onTutupBukuClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
            ) {
                Text(
                    text = "TUTUP BUKU (RESET BULAN)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
