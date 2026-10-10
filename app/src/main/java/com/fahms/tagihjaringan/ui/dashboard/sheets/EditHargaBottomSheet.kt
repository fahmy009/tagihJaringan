package com.fahms.tagihjaringan.ui.dashboard.sheets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahms.tagihjaringan.data.model.CustomerItem
import com.fahms.tagihjaringan.ui.theme.PinkPrimary
import com.fahms.tagihjaringan.ui.theme.SkySecondary
import com.fahms.tagihjaringan.ui.theme.TextDark
import com.fahms.tagihjaringan.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHargaBottomSheet(
    customers: List<CustomerItem>,
    userRole: String = "ADMIN",
    assignedProfile: String = "",
    onSaveNominal: (CustomerItem, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProfileFilter by remember { mutableStateOf("Semua") }

    // Unique profiles extraction
    val availableProfiles = remember(customers) {
        val profs = customers.map { it.profile.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("Semua") + profs
    }

    val filteredCustomers = remember(customers, searchQuery, selectedProfileFilter) {
        customers.filter { cust ->
            val matchesSearch = searchQuery.isBlank() || cust.name.contains(searchQuery, ignoreCase = true)
            val matchesProfile = selectedProfileFilter == "Semua" || cust.profile.equals(selectedProfileFilter, ignoreCase = true)
            matchesSearch && matchesProfile
        }.sortedBy { it.name.lowercase() }
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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "📝 Edit Harga Tagihan Pelanggan",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama pelanggan...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Bersihkan")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Profile Filter Chips for Admin
            if (userRole.uppercase() == "ADMIN" && availableProfiles.size > 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableProfiles.forEach { prof ->
                        FilterChip(
                            selected = selectedProfileFilter.equals(prof, ignoreCase = true),
                            onClick = { selectedProfileFilter = prof },
                            label = { Text(prof, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PinkPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pelanggan tidak ditemukan.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredCustomers, key = { index, customer -> "${customer.rowIndex}_${customer.name}_$index" }) { _, customer ->
                        EditHargaItemRow(
                            customer = customer,
                            onSaveNominal = { newNominal ->
                                onSaveNominal(customer, newNominal)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditHargaItemRow(
    customer: CustomerItem,
    onSaveNominal: (Int) -> Unit
) {
    fun formatDots(num: Int): String {
        if (num == 0) return ""
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(num)
    }

    var nominalText by remember(customer.nominal) {
        mutableStateOf(formatDots(customer.nominal))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (customer.profile.isNotBlank()) {
                    Text(
                        text = "Profil: ${customer.profile}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nominalText,
                    onValueChange = { input ->
                        val cleanStr = input.replace(".", "").replace(",", "").replace(" ", "").trim()
                        val parsed = cleanStr.toIntOrNull()
                        nominalText = if (parsed != null && parsed > 0) NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed) else ""
                    },
                    placeholder = { Text("Rp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(115.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        val cleanStr = nominalText.replace(".", "").replace(",", "").replace(" ", "").trim()
                        val parsed = cleanStr.toIntOrNull() ?: 0
                        onSaveNominal(parsed)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
