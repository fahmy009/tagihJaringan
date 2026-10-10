package com.fahms.tagihjaringan.data.repository

import android.util.Base64
import com.fahms.tagihjaringan.data.local.UserPreferencesRepository
import com.fahms.tagihjaringan.data.local.UserSession
import com.fahms.tagihjaringan.data.local.UserSettings
import com.fahms.tagihjaringan.data.model.*
import com.fahms.tagihjaringan.data.remote.FirebaseRepository
import com.fahms.tagihjaringan.data.remote.FlexibleBooleanAdapter
import com.fahms.tagihjaringan.data.remote.FlexibleIntAdapter
import com.fahms.tagihjaringan.data.remote.MikrotikApi
import com.fahms.tagihjaringan.data.remote.UnsafeOkHttpClient
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppRepository(
    private val preferencesRepository: UserPreferencesRepository
) {

    val firebaseRepository = FirebaseRepository()
    private val unsafeOkHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient()
    val gitHubUpdateManager by lazy { com.fahms.tagihjaringan.data.remote.GitHubUpdateManager(unsafeOkHttpClient) }

    private val gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.java, FlexibleBooleanAdapter())
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, FlexibleBooleanAdapter())
        .registerTypeAdapter(Int::class.java, FlexibleIntAdapter())
        .registerTypeAdapter(Int::class.javaPrimitiveType, FlexibleIntAdapter())
        .create()

    private val mikrotikApi: MikrotikApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://localhost/") // Base URL placeholder; dynamic fullUrl passed in methods
            .client(unsafeOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MikrotikApi::class.java)
    }

    suspend fun getUserSettings(): UserSettings {
        return preferencesRepository.userSettingsFlow.first()
    }

    suspend fun saveUserSettings(settings: UserSettings) {
        preferencesRepository.saveSettings(settings)
    }

    suspend fun getUserSession(): UserSession {
        return preferencesRepository.userSessionFlow.first()
    }

    suspend fun saveUserSession(session: UserSession) {
        preferencesRepository.saveUserSession(session)
    }

    suspend fun clearUserSession() {
        preferencesRepository.clearUserSession()
    }

    suspend fun checkForUpdates(currentVersion: String): com.fahms.tagihjaringan.data.remote.AppUpdateInfo? {
        return gitHubUpdateManager.checkForUpdates(currentVersion)
    }

    suspend fun loginUser(username: String, pass: String): Result<UserSession> {
        return try {
            val firebaseUser = firebaseRepository.authenticateUser(username, pass)
            if (firebaseUser != null) {
                val session = UserSession(
                    username = firebaseUser.username,
                    role = firebaseUser.role,
                    assignedProfile = firebaseUser.assignedProfile,
                    name = firebaseUser.name,
                    isLoggedIn = true
                )
                saveUserSession(session)
                Result.success(session)
            } else {
                Result.failure(Exception("Username atau Password salah."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitSetoran(
        collectorName: String,
        mitraName: String,
        profile: String,
        amount: Int,
        sisaPegangan: Int
    ): Result<String> {
        return try {
            firebaseRepository.submitSetoran(collectorName, mitraName, profile, amount, sisaPegangan)
            Result.success("Setoran sebesar Rp${amount} berhasil diajukan!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifySetoran(
        setoranId: String,
        amountReceived: Int = 0,
        selisihHutang: Int = 0
    ): Result<String> {
        return try {
            firebaseRepository.verifySetoran(setoranId, amountReceived, selisihHutang)
            Result.success("Setoran berhasil diverifikasi!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSetoran(setoranId: String): Result<String> {
        return try {
            firebaseRepository.deleteSetoran(setoranId)
            Result.success("Setoran berhasil dihapus.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(username: String, newPassword: String, newName: String): Result<String> {
        return try {
            val session = getUserSession()
            firebaseRepository.saveUserAccount(
                username = username,
                pass = newPassword,
                role = session.role,
                profile = session.assignedProfile,
                name = newName.ifBlank { session.name }
            )

            val currentSession = getUserSession()
            val updatedName = newName.ifBlank { currentSession.name }
            saveUserSession(currentSession.copy(name = updatedName))

            Result.success("Profil dan password berhasil diperbarui!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserAccount(
        username: String,
        password: String,
        role: String,
        assignedProfile: String,
        name: String,
        oldUsername: String = ""
    ): Result<String> {
        return try {
            firebaseRepository.saveUserAccount(username, password, role, assignedProfile, name, oldUsername)
            Result.success("Akun user '$username' berhasil disimpan!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(username: String): Result<String> {
        return try {
            firebaseRepository.deleteUserAccount(username)
            Result.success("Akun user '$username' berhasil dihapus.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildMikrotikBaseUrl(ip: String): String {
        var cleanIp = ip.trim()
        if (!cleanIp.startsWith("http://") && !cleanIp.startsWith("https://")) {
            cleanIp = "https://$cleanIp"
        }
        if (!cleanIp.endsWith("/")) {
            cleanIp = "$cleanIp/"
        }
        if (!cleanIp.contains("/rest/ppp/")) {
            cleanIp = "${cleanIp}rest/ppp/"
        }
        return cleanIp
    }

    private fun buildBasicAuth(user: String, pass: String): String {
        val credentials = "$user:$pass"
        val base64 = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $base64"
    }

    suspend fun toggleCustomerPayment(customer: CustomerItem, isChecked: Boolean): Result<String> {
        return try {
            val settings = getUserSettings()
            val session = getUserSession()
            val currentCollectorName = if (isChecked) (session.name.ifBlank { session.username }) else ""

            // 1. Update Firebase Firestore (instant real-time live sync across devices)
            firebaseRepository.updatePaymentStatus(
                rowIndex = customer.rowIndex,
                customerName = customer.name,
                customerProfile = customer.profile,
                isChecked = isChecked,
                nominal = customer.nominal,
                collectedBy = currentCollectorName
            )

            // 2. Update MikroTik status if credentials and IP are configured
            if (settings.mikrotikIp.isNotBlank() && settings.mikrotikUser.isNotBlank()) {
                val baseUrl = buildMikrotikBaseUrl(settings.mikrotikIp)
                val authHeader = buildBasicAuth(settings.mikrotikUser, settings.mikrotikPass)

                val secretUrl = "${baseUrl}secret"
                val queryProfile = customer.profile.ifBlank { settings.filterProfile }.ifBlank { null }
                var secretsResp = mikrotikApi.getSecrets(secretUrl, authHeader, queryProfile)
                var secrets = if (secretsResp.isSuccessful) secretsResp.body() ?: emptyList() else emptyList()

                var matchSecret = secrets.find { it.name.equals(customer.name, ignoreCase = true) }
                if (matchSecret == null) {
                    val fallbackResp = mikrotikApi.getSecrets(secretUrl, authHeader, null)
                    if (fallbackResp.isSuccessful) {
                        val fallbackSecrets = fallbackResp.body() ?: emptyList()
                        matchSecret = fallbackSecrets.find { it.name.equals(customer.name, ignoreCase = true) }
                    }
                }

                val secretId = matchSecret?.id
                if (secretId != null) {
                    val patchUrl = "${baseUrl}secret/$secretId"
                    val disabledValue = if (isChecked) "false" else "true"
                    mikrotikApi.patchSecret(patchUrl, authHeader, mapOf("disabled" to disabledValue))
                }

                // If unchecked (Belum Lunas), execute Triple Combo Isolir: GET active & DELETE active
                if (!isChecked) {
                    val activeUrl = "${baseUrl}active"
                    val activeResp = mikrotikApi.getActiveSessions(activeUrl, authHeader, customer.name)
                    if (activeResp.isSuccessful) {
                        val activeSessions = activeResp.body() ?: emptyList()
                        val matchSession = activeSessions.find { it.name.equals(customer.name, ignoreCase = true) }
                        if (matchSession != null && matchSession.id.isNotBlank()) {
                            val deleteUrl = "${baseUrl}active/${matchSession.id}"
                            mikrotikApi.deleteActiveSession(deleteUrl, authHeader)
                        }
                    }
                }
            }

            Result.success(if (isChecked) "Status pelanggan ${customer.name} berhasil diubah menjadi Lunas." else "Pelanggan ${customer.name} berhasil di-isolir.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerNominal(rowIndex: Int, customerName: String, newNominal: Int): Result<String> {
        return try {
            firebaseRepository.updateCustomerNominal(rowIndex, customerName, newNominal)
            Result.success("Berhasil memperbarui harga tagihan.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFinancialSettings(potonganList: List<PotonganItem>): Result<String> {
        return try {
            firebaseRepository.saveFinancialSettings(potonganList)
            Result.success("Daftar potongan berhasil disimpan.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerLocation(customerName: String, lat: Double, lng: Double): Result<String> {
        return try {
            firebaseRepository.updateCustomerLocation(customerName, lat, lng)
            Result.success("Lokasi GPS berhasil disimpan!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tutupBuku(
        periode: String = "",
        countLunas: Int = 0,
        grossIncome: Int = 0,
        totalPotongan: Int = 0,
        netIncome: Int = 0,
        profile: String = ""
    ): Result<String> {
        return try {
            firebaseRepository.tutupBuku(periode, countLunas, grossIncome, totalPotongan, netIncome, profile)
            Result.success("Tutup buku berhasil! Status pembayaran bulan baru telah di-reset dan laporan bulanan dicatat.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithMikrotik(currentCustomers: List<CustomerItem>): Result<Int> {
        return try {
            val settings = getUserSettings()
            val session = getUserSession()
            val isAdmin = session.role.uppercase() == "ADMIN"
            val profileToFilter = if (isAdmin) "" else session.assignedProfile.trim().ifBlank { settings.filterProfile.trim() }

            if (!isAdmin && profileToFilter.isBlank()) {
                return Result.failure(Exception("Profil Wilayah Tugas belum ditentukan untuk akun ini!"))
            }
            if (settings.mikrotikIp.isBlank() || settings.mikrotikUser.isBlank()) {
                return Result.failure(Exception("IP dan Kredensial MikroTik belum diisi di Pengaturan!"))
            }

            val baseUrl = buildMikrotikBaseUrl(settings.mikrotikIp)
            val authHeader = buildBasicAuth(settings.mikrotikUser, settings.mikrotikPass)
            val secretUrl = "${baseUrl}secret"

            val queryProfile = if (isAdmin) null else (if (profileToFilter.isBlank()) null else profileToFilter.trim())
            var secretsResp = mikrotikApi.getSecrets(secretUrl, authHeader, queryProfile)

            // Fallback if query with profile parameter returned empty list
            if (secretsResp.isSuccessful && secretsResp.body().orEmpty().isEmpty() && queryProfile != null) {
                val fallbackResp = mikrotikApi.getSecrets(secretUrl, authHeader, null)
                if (fallbackResp.isSuccessful && !fallbackResp.body().isNullOrEmpty()) {
                    secretsResp = fallbackResp
                }
            }

            if (!secretsResp.isSuccessful || secretsResp.body() == null) {
                return Result.failure(Exception("Gagal mengambil data secret MikroTik. HTTP ${secretsResp.code()}"))
            }

            var mikrotikSecrets = secretsResp.body()!!

            // Filter secrets in Kotlin for non-admin multi-profile matching
            if (!isAdmin && profileToFilter.isNotBlank()) {
                val allowedProfiles = profileToFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                mikrotikSecrets = mikrotikSecrets.filter { secret ->
                    allowedProfiles.any { p -> (secret.profile ?: "").trim().lowercase() == p }
                }
            }

            val mikrotikNameSet = mikrotikSecrets.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val existingNames = currentCustomers.map { it.name.trim().lowercase() }.toSet()

            // 1. Identify new secrets to add
            val newSecretsToSync = mikrotikSecrets.filter { secret ->
                secret.name.isNotBlank() && !existingNames.contains(secret.name.trim().lowercase())
            }

            // 2. Identify deleted secrets to remove (Profile-Isolated Mirror Sync)
            val targetCustomersInSheets = if (isAdmin) {
                currentCustomers
            } else {
                val allowedProfiles = profileToFilter.split(",").map { it.trim() }.filter { it.isNotBlank() }
                currentCustomers.filter { cust -> allowedProfiles.any { p -> cust.profile.equals(p, ignoreCase = true) } }
            }
            val customersToDelete = targetCustomersInSheets.filter { customer ->
                customer.name.isNotBlank() && !mikrotikNameSet.contains(customer.name.trim().lowercase())
            }

            if (mikrotikSecrets.isNotEmpty()) {
                val payloads = mikrotikSecrets.map { secret ->
                    val commentText = (secret.comment ?: "").trim()
                    val isServer = commentText.contains("server", ignoreCase = true)
                    val userDefaultNominal = if (settings.defaultNominal > 0) settings.defaultNominal else 95000
                    val calculatedNominal = if (isServer) 0 else userDefaultNominal
                    val extractedModem = if (commentText.isNotBlank()) {
                        commentText.split(Regex("[\\s-]+")).filter { it.isNotBlank() }.lastOrNull() ?: ""
                    } else ""

                    SyncNewCustomerPayload(
                        name = secret.name,
                        profile = secret.profile ?: "",
                        comment = secret.comment ?: "",
                        modem = extractedModem,
                        nominal = calculatedNominal,
                        bayar = false
                    )
                }

                // Sync into Firebase Firestore (updates comments/modem/profile for existing, creates new)
                firebaseRepository.syncNewCustomers(payloads)
            }

            if (customersToDelete.isNotEmpty()) {
                val namesToDelete = customersToDelete.map { it.name }
                firebaseRepository.deleteCustomers(namesToDelete)
            }

            Result.success(newSecretsToSync.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeIsolirMassal(unpaidCustomers: List<CustomerItem>): Result<Int> {
        return try {
            val settings = getUserSettings()
            if (settings.mikrotikIp.isBlank() || settings.mikrotikUser.isBlank()) {
                return Result.failure(Exception("IP dan Kredensial MikroTik belum diisi di Pengaturan!"))
            }

            val baseUrl = buildMikrotikBaseUrl(settings.mikrotikIp)
            val authHeader = buildBasicAuth(settings.mikrotikUser, settings.mikrotikPass)
            var countSuccess = 0

            val secretUrl = "${baseUrl}secret"

            for (customer in unpaidCustomers) {
                var secretId: String? = null

                val queryProfile = customer.profile.ifBlank { settings.filterProfile }.ifBlank { null }
                val profileResp = mikrotikApi.getSecrets(secretUrl, authHeader, queryProfile)
                if (profileResp.isSuccessful) {
                    val profileSecrets = profileResp.body() ?: emptyList()
                    secretId = profileSecrets.find { it.name.equals(customer.name, ignoreCase = true) }?.id
                }

                if (secretId == null) {
                    val fallbackResp = mikrotikApi.getSecrets(secretUrl, authHeader, null)
                    if (fallbackResp.isSuccessful) {
                        val fallbackSecrets = fallbackResp.body() ?: emptyList()
                        secretId = fallbackSecrets.find { it.name.equals(customer.name, ignoreCase = true) }?.id
                    }
                }

                if (secretId != null) {
                    val patchUrl = "${baseUrl}secret/$secretId"
                    mikrotikApi.patchSecret(patchUrl, authHeader, mapOf("disabled" to "true"))
                }

                val activeUrl = "${baseUrl}active"
                val activeResp = mikrotikApi.getActiveSessions(activeUrl, authHeader, customer.name)
                if (activeResp.isSuccessful) {
                    val activeSessions = activeResp.body() ?: emptyList()
                    val matchSession = activeSessions.find { it.name.equals(customer.name, ignoreCase = true) }
                    if (matchSession != null && matchSession.id.isNotBlank()) {
                        val deleteUrl = "${baseUrl}active/${matchSession.id}"
                        mikrotikApi.deleteActiveSession(deleteUrl, authHeader)
                    }
                }

                countSuccess++
            }

            Result.success(countSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
