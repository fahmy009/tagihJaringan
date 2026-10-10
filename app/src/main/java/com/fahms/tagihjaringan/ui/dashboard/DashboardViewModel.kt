package com.fahms.tagihjaringan.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahms.tagihjaringan.data.local.UserSession
import com.fahms.tagihjaringan.data.local.UserSettings
import com.fahms.tagihjaringan.data.model.CustomerItem
import com.fahms.tagihjaringan.data.model.LaporanBulananItem
import com.fahms.tagihjaringan.data.model.PotonganItem
import com.fahms.tagihjaringan.data.model.SetoranItem
import com.fahms.tagihjaringan.data.model.UserAccount
import com.fahms.tagihjaringan.data.repository.AppRepository
import com.fahms.tagihjaringan.ui.components.DialogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StatusFilter {
    SEMUA,
    LUNAS,
    BELUM_BAYAR
}

enum class ActiveSheet {
    NONE,
    SETTINGS,
    KEUANGAN,
    EDIT_HARGA,
    SETORAN_MASUK,
    PROFILE_SAYA,
    KELOLA_USER
}

data class DialogState(
    val isVisible: Boolean = false,
    val type: DialogType = DialogType.SUCCESS,
    val title: String = "",
    val message: String = "",
    val confirmText: String = "OK",
    val dismissText: String = "Batal",
    val onConfirm: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

data class DashboardUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.SEMUA,
    val customers: List<CustomerItem> = emptyList(),
    val potonganList: List<PotonganItem> = emptyList(),
    val setoranList: List<SetoranItem> = emptyList(),
    val usersList: List<UserAccount> = emptyList(),
    val laporanBulananList: List<LaporanBulananItem> = emptyList(),
    val userSettings: UserSettings = UserSettings(),
    val userSession: UserSession = UserSession(),
    val isAuthenticating: Boolean = false,
    val loginError: String? = null,
    val activeSheet: ActiveSheet = ActiveSheet.NONE,
    val dialogState: DialogState = DialogState()
)

class DashboardViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndData()
    }

    fun loadSettingsAndData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val settings = repository.getUserSettings()
            val session = repository.getUserSession()
            _uiState.update { it.copy(userSettings = settings, userSession = session) }

            // Connect live real-time listeners to Firebase Firestore for instant zero-lag updates
            repository.firebaseRepository.observeCustomers { liveCustomers ->
                val sorted = liveCustomers.sortedBy { it.name.lowercase() }
                _uiState.update { it.copy(customers = sorted, isLoading = false) }
            }
            repository.firebaseRepository.observeUsers { liveUsers ->
                _uiState.update { it.copy(usersList = liveUsers) }
            }
            repository.firebaseRepository.observeSetoran { liveSetoran ->
                _uiState.update { it.copy(setoranList = liveSetoran) }
            }
            repository.firebaseRepository.observeSettings { liveSettings ->
                _uiState.update { it.copy(potonganList = liveSettings.potongan) }
            }
            repository.firebaseRepository.observeLaporanBulanan { liveLaporan ->
                _uiState.update { it.copy(laporanBulananList = liveLaporan) }
            }

            executeBackgroundSync()
        }
    }

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, loginError = null) }
            val result = repository.loginUser(username, pass)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        userSession = session,
                        loginError = null
                    )
                }
                loadSettingsAndData()
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        loginError = exception.message ?: "Login gagal. Periksa username dan password."
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearUserSession()
            _uiState.update {
                it.copy(userSession = UserSession(isLoggedIn = false))
            }
        }
    }

    fun updateUserProfile(newPassword: String, newName: String) {
        viewModelScope.launch {
            showLoadingDialog("Memperbarui Profil...")
            val username = _uiState.value.userSession.username
            val result = repository.updateUserProfile(username, newPassword, newName)
            result.onSuccess { msg ->
                _uiState.update { it.copy(activeSheet = ActiveSheet.NONE) }
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Profil Diperbarui",
                            message = msg,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Memperbarui Profil",
                            message = exception.message ?: "Tidak dapat memperbarui profil.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun saveUserAccount(username: String, pass: String, role: String, profile: String, name: String, oldUsername: String = "") {
        viewModelScope.launch {
            showLoadingDialog("Menyimpan Akun User...")
            val result = repository.saveUserAccount(
                username = username,
                password = pass,
                role = role,
                assignedProfile = profile,
                name = name,
                oldUsername = oldUsername
            )
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Berhasil",
                            message = msg,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Menyimpan User",
                            message = exception.message ?: "Tidak dapat menyimpan akun user.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun deleteUserAccount(username: String) {
        viewModelScope.launch {
            showLoadingDialog("Menghapus Akun User...")
            val result = repository.deleteUserAccount(username)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Berhasil",
                            message = msg,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Menghapus User",
                            message = exception.message ?: "Tidak dapat menghapus user.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setStatusFilter(filter: StatusFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    fun openSheet(sheet: ActiveSheet) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.NONE) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = DialogState(isVisible = false)) }
    }

    fun toggleCustomerPayment(customer: CustomerItem, isChecked: Boolean) {
        if (!isChecked && customer.bayar) {
            _uiState.update {
                it.copy(
                    dialogState = DialogState(
                        isVisible = true,
                        type = DialogType.CONFIRMATION,
                        title = "🔒 Konfirmasi Isolir Pelanggan",
                        message = "Apakah Anda yakin ingin membatalkan status LUNAS dan meng-ISOLIR pelanggan \"${customer.name}\"?",
                        confirmText = "Ya, Isolir",
                        dismissText = "Batal",
                        onConfirm = {
                            dismissDialog()
                            executeToggleCustomerPayment(customer, false)
                        },
                        onDismiss = { dismissDialog() }
                    )
                )
            }
        } else {
            executeToggleCustomerPayment(customer, isChecked)
        }
    }

    private fun executeToggleCustomerPayment(customer: CustomerItem, isChecked: Boolean) {
        val currentUser = _uiState.value.userSession.name.ifBlank { _uiState.value.userSession.username }
        // Optimistic UI update
        val updatedCustomers = _uiState.value.customers.map {
            if (it.rowIndex == customer.rowIndex) {
                it.copy(bayar = isChecked, collectedBy = if (isChecked) currentUser else "")
            } else it
        }
        _uiState.update { it.copy(customers = updatedCustomers) }

        viewModelScope.launch {
            val result = repository.toggleCustomerPayment(customer, isChecked)
            result.onFailure { exception ->
                // Rollback on failure
                val rollbackCustomers = _uiState.value.customers.map {
                    if (it.rowIndex == customer.rowIndex) it.copy(bayar = !isChecked, collectedBy = customer.collectedBy) else it
                }
                _uiState.update {
                    it.copy(
                        customers = rollbackCustomers,
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Mengubah Status",
                            message = exception.message ?: "Tidak dapat mengubah status pembayaran pelanggan.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun submitSetoran(mitraName: String, amount: Int, sisaPegangan: Int) {
        viewModelScope.launch {
            showLoadingDialog("Mengajukan Setoran...")
            val session = _uiState.value.userSession
            val result = repository.submitSetoran(
                collectorName = session.name.ifBlank { session.username },
                mitraName = mitraName,
                profile = session.assignedProfile,
                amount = amount,
                sisaPegangan = sisaPegangan
            )
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Setoran Berhasil",
                            message = msg,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Setor",
                            message = exception.message ?: "Tidak dapat mengajukan setoran.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun verifySetoran(setoranId: String, amountReceived: Int, selisihHutang: Int) {
        viewModelScope.launch {
            showLoadingDialog("Verifikasi Setoran...")
            val result = repository.verifySetoran(setoranId, amountReceived, selisihHutang)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Verifikasi Berhasil",
                            message = msg,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Verifikasi",
                            message = exception.message ?: "Tidak dapat memverifikasi setoran.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun deleteSetoran(setoranId: String) {
        viewModelScope.launch {
            showLoadingDialog("Hapus Setoran...")
            val result = repository.deleteSetoran(setoranId)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Berhasil",
                            message = msg,
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Hapus",
                            message = exception.message ?: "Tidak dapat menghapus setoran.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun updateCustomerNominal(customer: CustomerItem, newNominal: Int) {
        viewModelScope.launch {
            showLoadingDialog("Memperbarui Harga...")
            val result = repository.updateCustomerNominal(customer.rowIndex, customer.name, newNominal)
            result.onSuccess { successMsg ->
                val updatedCustomers = _uiState.value.customers.map {
                    if (it.rowIndex == customer.rowIndex) it.copy(nominal = newNominal) else it
                }
                _uiState.update {
                    it.copy(
                        customers = updatedCustomers,
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Berhasil",
                            message = successMsg,
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Memperbarui",
                            message = exception.message ?: "Gagal mengubah nominal pelanggan.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun saveSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.saveUserSettings(newSettings)
            _uiState.update { it.copy(userSettings = newSettings, activeSheet = ActiveSheet.NONE) }
            _uiState.update {
                it.copy(
                    dialogState = DialogState(
                        isVisible = true,
                        type = DialogType.SUCCESS,
                        title = "Pengaturan Disimpan",
                        message = "Data pengaturan berhasil disimpan.",
                        onConfirm = {
                            dismissDialog()
                            loadSettingsAndData()
                        }
                    )
                )
            }
        }
    }

    fun savePotongan(newPotonganList: List<PotonganItem>) {
        viewModelScope.launch {
            showLoadingDialog("Menyimpan Potongan...")
            val result = repository.saveFinancialSettings(newPotonganList)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        potonganList = newPotonganList,
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Berhasil",
                            message = msg,
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Menyimpan",
                            message = exception.message ?: "Tidak dapat menyimpan data potongan.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun promptTutupBuku() {
        _uiState.update {
            it.copy(
                dialogState = DialogState(
                    isVisible = true,
                    type = DialogType.CONFIRMATION,
                    title = "Konfirmasi Tutup Buku",
                    message = "Apakah Anda yakin ingin melakukan Tutup Buku? Status pembayaran seluruh pelanggan akan di-reset untuk bulan baru.",
                    confirmText = "Ya, Tutup Buku",
                    dismissText = "Batal",
                    onConfirm = {
                        dismissDialog()
                        executeTutupBuku()
                    },
                    onDismiss = { dismissDialog() }
                )
            )
        }
    }

    fun updateCustomerLocation(customer: CustomerItem, lat: Double, lng: Double) {
        viewModelScope.launch {
            val result = repository.updateCustomerLocation(customer.name, lat, lng)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Lokasi Disimpan",
                            message = msg,
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    private fun executeTutupBuku() {
        viewModelScope.launch {
            showLoadingDialog("Proses Tutup Buku...")
            val role = _uiState.value.userSession.role.uppercase()
            val assignedProfile = _uiState.value.userSession.assignedProfile.trim()
            val currentCustomers = _uiState.value.customers
            val profileCustomers = if (role != "ADMIN" && assignedProfile.isNotBlank()) {
                val assignedProfileList = assignedProfile.split(",").map { it.trim() }.filter { it.isNotBlank() }
                currentCustomers.filter { cust -> assignedProfileList.any { p -> cust.profile.equals(p, ignoreCase = true) } }
            } else currentCustomers

            val countLunas = profileCustomers.count { it.bayar }
            val gross = profileCustomers.filter { it.bayar }.sumOf { it.nominal }
            val totalPot = _uiState.value.potonganList.sumOf { it.amount }
            val net = gross - totalPot
            val periodeStr = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())

            val result = repository.tutupBuku(
                periode = periodeStr,
                countLunas = countLunas,
                grossIncome = gross,
                totalPotongan = totalPot,
                netIncome = net,
                profile = assignedProfile.ifBlank { "Semua" }
            )
            result.onSuccess { msg ->
                _uiState.update { it.copy(activeSheet = ActiveSheet.NONE) }
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Tutup Buku Berhasil",
                            message = msg,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Tutup Buku",
                            message = exception.message ?: "Tidak dapat memproses tutup buku.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun promptSync() {
        val session = _uiState.value.userSession
        val role = session.role.uppercase()
        val settings = _uiState.value.userSettings

        val promptMessage = if (role == "ADMIN") {
            "Yakin ingin menarik SELURUH data MikroTik (Semua Profil Wilayah)?"
        } else {
            val targetProfile = session.assignedProfile.trim().ifBlank { settings.filterProfile.trim() }
            if (targetProfile.isBlank()) {
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Filter Profile Belum Ditentukan",
                            message = "Profil wilayah tugas belum ditentukan untuk akun ini!",
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
                return
            }
            "Yakin ingin menarik data MikroTik dengan profil \"$targetProfile\"?"
        }

        _uiState.update {
            it.copy(
                dialogState = DialogState(
                    isVisible = true,
                    type = DialogType.CONFIRMATION,
                    title = "Konfirmasi Sync",
                    message = promptMessage,
                    confirmText = "Mulai Sync",
                    dismissText = "Batal",
                    onConfirm = {
                        dismissDialog()
                        executeSync()
                    },
                    onDismiss = { dismissDialog() }
                )
            )
        }
    }

    private fun executeSync() {
        viewModelScope.launch {
            showLoadingDialog("Menarik Data MikroTik...")
            val currentCustomers = _uiState.value.customers
            val result = repository.syncWithMikrotik(currentCustomers)
            result.onSuccess { addedCount ->
                val message = if (addedCount > 0) {
                    "Berhasil menyinkronkan $addedCount pelanggan baru dari MikroTik."
                } else {
                    "Daftar pelanggan sudah 100% tersinkron dengan MikroTik."
                }
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Sinkronisasi Selesai",
                            message = message,
                            onConfirm = {
                                dismissDialog()
                                loadSettingsAndData()
                            }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Sinkronisasi",
                            message = exception.message ?: "Terjadi kesalahan saat menyinkronkan data.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    fun promptIsolirMassal() {
        val role = _uiState.value.userSession.role.uppercase()
        val assignedProfile = _uiState.value.userSession.assignedProfile.trim()
        val assignedProfileList = assignedProfile.split(",").map { it.trim() }.filter { it.isNotBlank() }

        val profileCustomers = if (role != "ADMIN" && assignedProfileList.isNotEmpty()) {
            _uiState.value.customers.filter { cust -> assignedProfileList.any { p -> cust.profile.equals(p, ignoreCase = true) } }
        } else {
            _uiState.value.customers
        }

        val unpaidList = profileCustomers.filter { !it.bayar }
        if (unpaidList.isEmpty()) {
            _uiState.update {
                it.copy(
                    dialogState = DialogState(
                        isVisible = true,
                        type = DialogType.SUCCESS,
                        title = "Info Isolir Massal",
                        message = "Semua pelanggan di wilayah ini sudah LUNAS! Tidak ada pelanggan yang perlu diisolir.",
                        onConfirm = { dismissDialog() }
                    )
                )
            }
            return
        }

        val profileText = if (role != "ADMIN" && assignedProfile.isNotBlank()) " wilayah $assignedProfile" else ""
        _uiState.update {
            it.copy(
                dialogState = DialogState(
                    isVisible = true,
                    type = DialogType.CONFIRMATION,
                    title = "🔒 Konfirmasi Isolir Massal",
                    message = "Apakah Anda yakin ingin meng-ISOLIR & MEN-KICK seluruh pelanggan yang BELUM BAYAR$profileText (${unpaidList.size} pelanggan)?",
                    confirmText = "Ya, Isolir Semua",
                    dismissText = "Batal",
                    onConfirm = {
                        dismissDialog()
                        executeIsolirMassal(unpaidList)
                    },
                    onDismiss = { dismissDialog() }
                )
            )
        }
    }

    private fun executeIsolirMassal(unpaidList: List<CustomerItem>) {
        viewModelScope.launch {
            showLoadingDialog("Memproses Isolir Massal...")
            val result = repository.executeIsolirMassal(unpaidList)
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Isolir Massal Selesai",
                            message = "Berhasil meng-isolir & men-kick $count pelanggan yang belum bayar.",
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.ERROR,
                            title = "Gagal Isolir Massal",
                            message = exception.message ?: "Terjadi kesalahan saat memproses isolir massal.",
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    private fun executeBackgroundSync() {
        viewModelScope.launch {
            val currentCustomers = _uiState.value.customers
            repository.syncWithMikrotik(currentCustomers)
        }
    }

    fun checkForAppUpdate(context: android.content.Context, manualClick: Boolean = false) {
        viewModelScope.launch {
            if (manualClick) {
                showLoadingDialog("Memeriksa Pembaruan di GitHub...")
            }
            val updateInfo = repository.checkForUpdates("1.0.0")
            if (manualClick) {
                dismissDialog()
            }
            if (updateInfo != null) {
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.CONFIRMATION,
                            title = "🚀 Pembaruan Aplikasi Tersedia (${updateInfo.version})",
                            message = "Versi terbaru '${updateInfo.version}' tersedia di GitHub.\n\nCatatan Rilis:\n${updateInfo.changelog}",
                            confirmText = "📥 Download & Install",
                            dismissText = "Nanti Saja",
                            onConfirm = {
                                dismissDialog()
                                downloadAndInstallUpdate(context, updateInfo.downloadUrl)
                            },
                            onDismiss = { dismissDialog() }
                        )
                    )
                }
            } else if (manualClick) {
                _uiState.update {
                    it.copy(
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SUCCESS,
                            title = "Versi Terbaru",
                            message = "Aplikasi Anda sudah menggunakan versi paling baru di GitHub!",
                            onConfirm = { dismissDialog() }
                        )
                    )
                }
            }
        }
    }

    private fun downloadAndInstallUpdate(context: android.content.Context, downloadUrl: String) {
        viewModelScope.launch {
            showLoadingDialog("Mengunduh Pembaruan (0%)...")
            repository.gitHubUpdateManager.downloadAndInstallApk(
                context = context,
                downloadUrl = downloadUrl,
                onProgress = { progress ->
                    _uiState.update {
                        it.copy(
                            dialogState = DialogState(
                                isVisible = true,
                                type = DialogType.LOADING,
                                title = "Mengunduh Pembaruan ($progress%)...",
                                message = "Mohon tunggu hingga proses download selesai..."
                            )
                        )
                    }
                },
                onCompleted = {
                    dismissDialog()
                },
                onError = { errorMsg ->
                    _uiState.update {
                        it.copy(
                            dialogState = DialogState(
                                isVisible = true,
                                type = DialogType.ERROR,
                                title = "Gagal Mengunduh Pembaruan",
                                message = errorMsg,
                                onDismiss = { dismissDialog() }
                            )
                        )
                    }
                }
            )
        }
    }

    private fun showLoadingDialog(title: String) {
        _uiState.update {
            it.copy(
                dialogState = DialogState(
                    isVisible = true,
                    type = DialogType.LOADING,
                    title = title,
                    message = "Mohon tunggu sejenak..."
                )
            )
        }
    }

    class Factory(private val repository: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
