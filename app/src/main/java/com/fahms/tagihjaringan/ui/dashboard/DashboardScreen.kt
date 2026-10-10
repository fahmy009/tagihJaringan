package com.fahms.tagihjaringan.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahms.tagihjaringan.data.model.CustomerItem
import com.fahms.tagihjaringan.ui.auth.LoginScreen
import com.fahms.tagihjaringan.ui.components.CustomRoundedCheckbox
import com.fahms.tagihjaringan.ui.components.SweetAlertDialog
import com.fahms.tagihjaringan.ui.dashboard.sheets.EditHargaBottomSheet
import com.fahms.tagihjaringan.ui.dashboard.sheets.KelolaUserBottomSheet
import com.fahms.tagihjaringan.ui.dashboard.sheets.KeuanganBottomSheet
import com.fahms.tagihjaringan.ui.dashboard.sheets.ProfileSayaBottomSheet
import com.fahms.tagihjaringan.ui.dashboard.sheets.SettingsBottomSheet
import com.fahms.tagihjaringan.ui.dashboard.sheets.SetoranMasukBottomSheet
import com.fahms.tagihjaringan.ui.dashboard.sheets.SubmitSetoranDialog
import com.fahms.tagihjaringan.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.userSession.isLoggedIn) {
        LoginScreen(
            onLoginClick = { user, pass -> viewModel.login(user, pass) },
            isLoading = uiState.isAuthenticating,
            errorMessage = uiState.loginError
        )
        return
    }

    fun formatRupiah(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "")
    }

    val role = uiState.userSession.role.uppercase()
    val assignedProfile = uiState.userSession.assignedProfile.trim()
    val assignedProfileList = remember(assignedProfile) {
        assignedProfile.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    // Base customer list strictly isolated by profile for non-admin accounts (supports multi-profiles)
    val profileCustomers = remember(uiState.customers, role, assignedProfileList) {
        if (role != "ADMIN" && assignedProfileList.isNotEmpty()) {
            uiState.customers.filter { cust -> assignedProfileList.any { p -> cust.profile.equals(p, ignoreCase = true) } }
        } else {
            uiState.customers
        }
    }

    var selectedAdminProfileFilter by remember { mutableStateOf("Semua") }
    val availableAdminProfiles = remember(uiState.customers) {
        val profs = uiState.customers.map { it.profile.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("Semua") + profs
    }

    val adminProfileFilteredCustomers = remember(profileCustomers, selectedAdminProfileFilter, role) {
        if (role == "ADMIN" && selectedAdminProfileFilter != "Semua") {
            profileCustomers.filter { cust -> cust.profile.equals(selectedAdminProfileFilter, ignoreCase = true) }
        } else {
            profileCustomers
        }
    }

    // Role-based customer filtering for list view
    val filteredCustomers = remember(adminProfileFilteredCustomers, uiState.searchQuery, uiState.statusFilter, role) {
        adminProfileFilteredCustomers.filter { customer ->
            val matchesCollector = if (role == "COLLECTOR") !customer.bayar else true

            val matchesQuery = if (uiState.searchQuery.isBlank()) true else {
                customer.name.contains(uiState.searchQuery, ignoreCase = true) ||
                customer.comment.contains(uiState.searchQuery, ignoreCase = true) ||
                customer.modem.contains(uiState.searchQuery, ignoreCase = true) ||
                customer.profile.contains(uiState.searchQuery, ignoreCase = true)
            }

            val matchesStatus = when (uiState.statusFilter) {
                StatusFilter.SEMUA -> true
                StatusFilter.LUNAS -> customer.bayar
                StatusFilter.BELUM_BAYAR -> !customer.bayar
            }

            matchesCollector && matchesQuery && matchesStatus
        }.sortedBy { it.name.lowercase() }
    }

    // Collector live calculations (pure per-account collected & total debt)
    val collectorName = uiState.userSession.name.ifBlank { uiState.userSession.username }
    val collectorUsername = uiState.userSession.username
    val totalCollectedByCollector = remember(profileCustomers, collectorName, collectorUsername) {
        profileCustomers
            .filter { customer ->
                customer.bayar && (
                    customer.collectedBy.isBlank() ||
                    customer.collectedBy.equals(collectorName, ignoreCase = true) ||
                    customer.collectedBy.equals(collectorUsername, ignoreCase = true)
                )
            }
            .sumOf { it.nominal }
    }
    val totalSetorByCollector = remember(uiState.setoranList, collectorName) {
        uiState.setoranList
            .filter { it.collectorName.equals(collectorName, ignoreCase = true) }
            .sumOf { it.amount }
    }
    val sisaHasilTagihHariIni = (totalCollectedByCollector - totalSetorByCollector).coerceAtLeast(0)

    val collectorTotalHutang = remember(uiState.setoranList, collectorName, collectorUsername) {
        uiState.setoranList
            .filter {
                it.collectorName.equals(collectorName, ignoreCase = true) ||
                it.collectorName.equals(collectorUsername, ignoreCase = true)
            }
            .sumOf { it.selisihHutang }
    }

    // Income calculations (Mitra Gross Income based on verified physical cash received)
    val totalPemasukanLunas = remember(profileCustomers, uiState.setoranList, role, assignedProfileList) {
        if (role == "MITRA" && assignedProfileList.isNotEmpty()) {
            val verifiedCashFromSetoran = uiState.setoranList
                .filter { it.status == "VERIFIED" && assignedProfileList.any { p -> it.profile.equals(p, ignoreCase = true) } }
                .sumOf { it.amountReceived }
            if (verifiedCashFromSetoran > 0) verifiedCashFromSetoran else profileCustomers.filter { it.bayar }.sumOf { it.nominal }
        } else {
            profileCustomers.filter { it.bayar }.sumOf { it.nominal }
        }
    }
    val totalPotongan = remember(uiState.potonganList, assignedProfileList, role) {
        if (role != "ADMIN" && assignedProfileList.isNotEmpty()) {
            uiState.potonganList.filter { it.profile.isBlank() || assignedProfileList.any { p -> it.profile.equals(p, ignoreCase = true) } }.sumOf { it.amount }
        } else {
            uiState.potonganList.sumOf { it.amount }
        }
    }
    val hasilBersih = totalPemasukanLunas - totalPotongan

    var showSubmitSetoranDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(colors = listOf(PinkPrimary, SkySecondary)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = uiState.userSession.name.ifBlank { uiState.userSession.username },
                                style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.25f)) {
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (assignedProfile.isNotBlank()) "Profil: $assignedProfile" else "Semua Profil",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.promptSync() }) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync", tint = Color.White)
                        }

                        if (role == "ADMIN" || role == "MITRA") {
                            IconButton(onClick = { viewModel.promptIsolirMassal() }) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Isolir Massal", tint = Color.White)
                            }
                        }

                        IconButton(onClick = { viewModel.openSheet(ActiveSheet.PROFILE_SAYA) }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profil Saya", tint = Color.White)
                        }

                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (role == "COLLECTOR") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 12.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = Brush.horizontalGradient(colors = listOf(SkySecondary, Color(0xFF0284C7))))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("HASIL TAGIH HARI INI", style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold))
                                Text("Pegangan: ${formatRupiah(sisaHasilTagihHariIni)}", style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f)))
                            }

                            Button(
                                onClick = { showSubmitSetoranDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🤝 Setor Uang", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                StickyBottomHasilBersihBar(hasilBersihFormatted = formatRupiah(hasilBersih))
            }
        },
        containerColor = AppBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search TextField
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari nama, profil, atau catatan...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PinkPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Quick Action Cards Grid for Admin & Mitra (Clear Text Labels)
                if (role == "ADMIN" || role == "MITRA") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (role == "MITRA") {
                            ActionMenuCard(
                                icon = Icons.Default.MoveToInbox,
                                title = "Setoran Masuk",
                                containerColor = WarningYellow.copy(alpha = 0.15f),
                                contentColor = WarningYellow,
                                onClick = { viewModel.openSheet(ActiveSheet.SETORAN_MASUK) }
                            )
                        }

                        ActionMenuCard(
                            icon = Icons.Default.Group,
                            title = "Kelola User",
                            containerColor = SkySecondary.copy(alpha = 0.15f),
                            contentColor = SkySecondary,
                            onClick = { viewModel.openSheet(ActiveSheet.KELOLA_USER) }
                        )

                        ActionMenuCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Keuangan",
                            containerColor = PriceGreen.copy(alpha = 0.15f),
                            contentColor = PriceGreen,
                            onClick = { viewModel.openSheet(ActiveSheet.KEUANGAN) }
                        )

                        ActionMenuCard(
                            icon = Icons.Default.Edit,
                            title = "Edit Harga",
                            containerColor = PinkPrimary.copy(alpha = 0.15f),
                            contentColor = PinkPrimary,
                            onClick = { viewModel.openSheet(ActiveSheet.EDIT_HARGA) }
                        )

                        if (role == "ADMIN") {
                            ActionMenuCard(
                                icon = Icons.Default.Settings,
                                title = "Pengaturan",
                                containerColor = Color(0xFF64748B).copy(alpha = 0.15f),
                                contentColor = Color(0xFF475569),
                                onClick = { viewModel.openSheet(ActiveSheet.SETTINGS) }
                            )
                        }
                    }
                }

                // Collector Debt Warning Banner Card
                if (role == "COLLECTOR" && collectorTotalHutang > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RedDanger.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 22.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Catatan Tanggungan / Hutang Setoran",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = RedDanger,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Terdapat selisih setoran yang belum diserahkan sebesar ${formatRupiah(collectorTotalHutang)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = RedDanger,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Admin Profile Filter Chips Bar
                if (role == "ADMIN" && availableAdminProfiles.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableAdminProfiles.forEach { prof ->
                            FilterChip(
                                selected = selectedAdminProfileFilter.equals(prof, ignoreCase = true),
                                onClick = { selectedAdminProfileFilter = prof },
                                label = { Text(if (prof == "Semua") "Semua Profil" else prof, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SkySecondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Filter Status Chips (Semua, Sudah Lunas, Belum Bayar) strictly calculated from profileCustomers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        text = "Semua (${adminProfileFilteredCustomers.size})",
                        selected = uiState.statusFilter == StatusFilter.SEMUA,
                        onClick = { viewModel.setStatusFilter(StatusFilter.SEMUA) }
                    )
                    FilterChipItem(
                        text = "Lunas (${adminProfileFilteredCustomers.count { it.bayar }})",
                        selected = uiState.statusFilter == StatusFilter.LUNAS,
                        onClick = { viewModel.setStatusFilter(StatusFilter.LUNAS) }
                    )
                    FilterChipItem(
                        text = "Belum (${adminProfileFilteredCustomers.count { !it.bayar }})",
                        selected = uiState.statusFilter == StatusFilter.BELUM_BAYAR,
                        onClick = { viewModel.setStatusFilter(StatusFilter.BELUM_BAYAR) }
                    )
                }

                // Customer List
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PinkPrimary)
                    }
                } else if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (role == "COLLECTOR") "Semua tagihan di wilayah ini sudah LUNAS! 👍" else "Pelanggan tidak ditemukan.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(filteredCustomers, key = { index, customer -> "${customer.rowIndex}_${customer.name}_$index" }) { _, customer ->
                            CustomerCardItem(
                                customer = customer,
                                formatRupiah = ::formatRupiah,
                                onCheckedChange = { isChecked ->
                                    viewModel.toggleCustomerPayment(customer, isChecked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Render Setor Dialog for Collector
    if (showSubmitSetoranDialog) {
        SubmitSetoranDialog(
            totalHasilTagihHariIni = sisaHasilTagihHariIni,
            mitraTargetName = assignedProfile.ifBlank { "Pemilik Profile" },
            onSubmitSetoran = { mitraName, amount, sisa ->
                showSubmitSetoranDialog = false
                viewModel.submitSetoran(mitraName, amount, sisa)
            },
            onDismiss = { showSubmitSetoranDialog = false }
        )
    }

    // Render Bottom Sheets
    when (uiState.activeSheet) {
        ActiveSheet.PROFILE_SAYA -> {
            val context = LocalContext.current
            ProfileSayaBottomSheet(
                userSession = uiState.userSession,
                onUpdateProfile = { newPass, newName ->
                    viewModel.updateUserProfile(newPass, newName)
                },
                onCheckUpdateClick = {
                    viewModel.checkForAppUpdate(context, manualClick = true)
                },
                onLogout = { viewModel.logout() },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.KELOLA_USER -> {
            KelolaUserBottomSheet(
                users = uiState.usersList,
                userSession = uiState.userSession,
                existingProfiles = availableAdminProfiles.filter { it != "Semua" },
                onSaveUser = { u, p, r, prof, n, oldUser ->
                    viewModel.saveUserAccount(u, p, r, prof, n, oldUser)
                },
                onDeleteUser = { username ->
                    viewModel.deleteUserAccount(username)
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.SETTINGS -> {
            SettingsBottomSheet(
                currentSettings = uiState.userSettings,
                onSaveSettings = { viewModel.saveSettings(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.KEUANGAN -> {
            KeuanganBottomSheet(
                grossIncome = totalPemasukanLunas,
                initialPotongan = uiState.potonganList,
                laporanBulananList = uiState.laporanBulananList,
                userRole = role,
                assignedProfile = assignedProfile,
                onSavePotongan = { viewModel.savePotongan(it) },
                onTutupBukuClick = { viewModel.promptTutupBuku() },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.EDIT_HARGA -> {
            EditHargaBottomSheet(
                customers = profileCustomers,
                userRole = role,
                assignedProfile = assignedProfile,
                onSaveNominal = { customer, newNominal ->
                    viewModel.updateCustomerNominal(customer, newNominal)
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.SETORAN_MASUK -> {
            SetoranMasukBottomSheet(
                setoranList = uiState.setoranList,
                userRole = role,
                userAssignedProfile = assignedProfile,
                userAccountName = collectorName,
                onVerifySetoran = { setoranId, rec, hutang ->
                    viewModel.verifySetoran(setoranId, rec, hutang)
                },
                onDeleteSetoran = { setoranId ->
                    viewModel.deleteSetoran(setoranId)
                },
                onDismiss = { viewModel.closeSheet() }
            )
        }
        ActiveSheet.NONE -> {}
    }

    // Render SweetAlert Custom Dialog
    if (uiState.dialogState.isVisible) {
        SweetAlertDialog(
            type = uiState.dialogState.type,
            title = uiState.dialogState.title,
            message = uiState.dialogState.message,
            confirmText = uiState.dialogState.confirmText,
            dismissText = uiState.dialogState.dismissText,
            onConfirm = uiState.dialogState.onConfirm,
            onDismiss = uiState.dialogState.onDismiss
        )
    }
}

@Composable
fun ActionMenuCard(
    icon: ImageVector,
    title: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
fun FilterChipItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PinkPrimary,
            selectedLabelColor = Color.White,
            containerColor = Color.White,
            labelColor = TextDark
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) PinkPrimary else Color(0xFFE2E8F0)
        )
    )
}

@Composable
fun CustomerCardItem(
    customer: CustomerItem,
    formatRupiah: (Int) -> String,
    onCheckedChange: (Boolean) -> Unit
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (customer.bayar) PinkLight else CardSurface,
        animationSpec = tween(durationMillis = 250),
        label = "CardBgColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!customer.bayar) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val detailText = listOf(customer.profile, customer.modem, customer.comment)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")

                if (detailText.isNotBlank()) {
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatRupiah(customer.nominal),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = PriceGreen,
                        fontWeight = FontWeight.Bold
                    )
                )

                CustomRoundedCheckbox(
                    checked = customer.bayar,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}

/**
 * Sticky Bottom Bar showing live calculated "Hasil Bersih"
 */
@Composable
fun StickyBottomHasilBersihBar(
    hasilBersihFormatted: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(SkySecondary, Color(0xFF0284C7))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HASIL BERSIH",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Pemasukan Lunas - Potongan",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    )
                }

                Text(
                    text = hasilBersihFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }
}
