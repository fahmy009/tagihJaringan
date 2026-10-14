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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppRepository(
    private val preferencesRepository: UserPreferencesRepository
) {

    val firebaseRepository = FirebaseRepository()
    private val unsafeOkHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient()
    val gitHubUpdateManager by lazy { com.fahms.tagihjaringan.data.remote.GitHubUpdateManager(unsafeOkHttpClient) }
    val mikrotikv6SocketApi by lazy { com.fahms.tagihjaringan.data.remote.Mikrotikv6SocketApi() }

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
            val settings = getUserSettings()
            if (settings.customProjectId.isNotBlank() && settings.customApiKey.isNotBlank() && settings.customAppId.isNotBlank()) {
                firebaseRepository.rebindFirestore(settings.customProjectId, settings.customApiKey, settings.customAppId)
            }
            val user = firebaseRepository.login(username, pass)
            if (user != null) {
                val session = UserSession(
                    username = user.username,
                    role = user.role,
                    assignedProfile = user.assignedProfile,
                    name = user.name,
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
        mitraName: String,
        amount: Int,
        sisaPegangan: Int
    ): Result<String> {
        return try {
            val session = getUserSession()
            firebaseRepository.submitSetoran(mitraName, amount, sisaPegangan, session)
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

    suspend fun saveMikrotikRouter(router: MikrotikRouter): Result<String> {
        return try {
            firebaseRepository.saveMikrotikRouter(router)
            Result.success("Router '${router.name}' berhasil disimpan!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMikrotikRouter(routerId: String, targetProfile: String = "", currentCustomers: List<CustomerItem> = emptyList()): Result<String> {
        return try {
            firebaseRepository.deleteMikrotikRouter(routerId)
            if (targetProfile.isNotBlank() && currentCustomers.isNotEmpty()) {
                val profiles = targetProfile.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                val customersInProfile = currentCustomers.filter { cust ->
                    profiles.any { p -> cust.profile.trim().equals(p, ignoreCase = true) }
                }
                if (customersInProfile.isNotEmpty()) {
                    firebaseRepository.deleteCustomers(customersInProfile.map { it.name })
                }
            }
            Result.success("Router berhasil dihapus.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildMikrotikBaseUrl(ip: String, port: Int = 0): String {
        var cleanIp = ip.trim()
        val scheme = if (cleanIp.startsWith("https://")) "https://" else if (cleanIp.startsWith("http://")) "http://" else "http://"
        var hostAndPort = cleanIp.replace("https://", "").replace("http://", "").split("/")[0].trim()

        if (!hostAndPort.contains(":") && port > 0) {
            hostAndPort = "$hostAndPort:$port"
        }

        var fullUrl = "$scheme$hostAndPort"
        if (!fullUrl.endsWith("/")) {
            fullUrl = "$fullUrl/"
        }
        if (!fullUrl.contains("/rest/ppp/")) {
            fullUrl = "${fullUrl}rest/ppp/"
        }
        return fullUrl
    }

    private fun buildBasicAuth(user: String, pass: String): String {
        val credentials = "$user:$pass"
        val base64 = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $base64"
    }

    suspend fun getConfiguredRouters(): List<MikrotikRouter> {
        return firebaseRepository.getRoutersOnce()
    }

    suspend fun toggleCustomerPayment(customer: CustomerItem, isChecked: Boolean): Result<String> {
        return try {
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

            // 2. Multi-Router Smart Dispatching
            val routers = getConfiguredRouters()
            val matchingRouter = routers.find { r ->
                r.targetProfile.isNotBlank() && r.targetProfile.split(",").any { p -> customer.profile.contains(p.trim(), ignoreCase = true) }
            } ?: routers.firstOrNull()

            if (matchingRouter != null && matchingRouter.ip.isNotBlank()) {
                if (matchingRouter.version == "v6") {
                    val secrets = mikrotikv6SocketApi.getSecrets(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, customer.profile)
                    val matchSecret = secrets.find { it.name.equals(customer.name, ignoreCase = true) }
                    val secretId = matchSecret?.id ?: customer.name
                    mikrotikv6SocketApi.patchSecret(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, secretId, !isChecked)

                    if (!isChecked) {
                        val activeSessions = mikrotikv6SocketApi.getActiveSessions(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, customer.name)
                        val matchSession = activeSessions.find { it.name.equals(customer.name, ignoreCase = true) }
                        if (matchSession != null && matchSession.id.isNotBlank()) {
                            mikrotikv6SocketApi.deleteActiveSession(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, matchSession.id)
                        }
                    }
                } else {
                    val baseUrl = buildMikrotikBaseUrl(matchingRouter.ip, matchingRouter.apiPort)
                    val authHeader = buildBasicAuth(matchingRouter.user, matchingRouter.pass)

                    val secretUrl = "${baseUrl}secret"
                    val queryProfile = customer.profile.ifBlank { matchingRouter.targetProfile }.ifBlank { null }
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

    suspend fun syncWithMikrotik(currentCustomers: List<CustomerItem>, targetRouterId: String = "ALL"): Result<Int> {
        return try {
            val settings = getUserSettings()
            val session = getUserSession()
            val isAdmin = session.role.uppercase() == "ADMIN"
            val profileToFilter = if (isAdmin) "" else session.assignedProfile.trim().ifBlank { settings.filterProfile.trim() }

            val configuredRouters = getConfiguredRouters()
            val routers = if (targetRouterId != "ALL" && targetRouterId.isNotBlank()) {
                configuredRouters.filter { it.id == targetRouterId }
            } else {
                configuredRouters
            }

            if (routers.isEmpty()) {
                return Result.failure(Exception("Belum ada router MikroTik terkonfigurasi!\nSilakan tambahkan router terlebih dahulu di menu 🌐 Kelola Router."))
            }

            val allFetchedSecrets = mutableListOf<MikrotikSecret>()
            val responsiveProfiles = mutableSetOf<String>()

            coroutineScope {
                val deferredList = routers.map { router ->
                    async {
                        withTimeoutOrNull(8000) { // Increased to 8 seconds
                            try {
                                if (router.version == "v6") {
                                    val secrets = mikrotikv6SocketApi.getSecrets(router.ip, router.apiPort, router.user, router.pass, if (isAdmin) null else profileToFilter)
                                    if (secrets.isNotEmpty() && router.targetProfile.isNotBlank()) {
                                        responsiveProfiles.addAll(router.targetProfile.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() })
                                    }
                                    secrets
                                } else {
                                    val baseUrl = buildMikrotikBaseUrl(router.ip, router.apiPort)
                                    val authHeader = buildBasicAuth(router.user, router.pass)
                                    val secretUrl = "${baseUrl}secret"

                                    val queryProfile = if (isAdmin) null else (if (profileToFilter.isBlank()) null else profileToFilter.trim())
                                    var secretsResp = mikrotikApi.getSecrets(secretUrl, authHeader, queryProfile)

                                    if (secretsResp.isSuccessful && secretsResp.body().orEmpty().isEmpty() && queryProfile != null) {
                                        val fallbackResp = mikrotikApi.getSecrets(secretUrl, authHeader, null)
                                        if (fallbackResp.isSuccessful && !fallbackResp.body().isNullOrEmpty()) {
                                            secretsResp = fallbackResp
                                        }
                                    }

                                    if (secretsResp.isSuccessful && secretsResp.body() != null) {
                                        if (router.targetProfile.isNotBlank()) {
                                            responsiveProfiles.addAll(router.targetProfile.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() })
                                        }
                                        secretsResp.body()!!
                                    } else emptyList()
                                }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } ?: emptyList()
                    }
                }
                val results = deferredList.awaitAll()
                results.forEach { allFetchedSecrets.addAll(it) }
            }

            var mikrotikSecrets = allFetchedSecrets.distinctBy { it.name.trim().lowercase() }

            // Filter secrets in Kotlin for non-admin multi-profile matching
            if (!isAdmin && profileToFilter.isNotBlank()) {
                val allowedProfiles = profileToFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                mikrotikSecrets = mikrotikSecrets.filter { secret ->
                    allowedProfiles.any { p -> (secret.profile ?: "").trim().lowercase() == p }
                }
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

            // Safe Per-Router Mirror Cleanup for responsive router profiles
            val mikrotikNameSet = mikrotikSecrets.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val targetProfilesToClean = if (responsiveProfiles.isNotEmpty()) responsiveProfiles else (if (isAdmin) emptySet() else profileToFilter.split(",").map { it.trim().lowercase() }.toSet())

            if (targetProfilesToClean.isNotEmpty()) {
                val customersInResponsiveProfiles = currentCustomers.filter { cust ->
                    targetProfilesToClean.any { p -> cust.profile.trim().equals(p, ignoreCase = true) }
                }
                val customersToDelete = customersInResponsiveProfiles.filter { cust ->
                    cust.name.isNotBlank() && !mikrotikNameSet.contains(cust.name.trim().lowercase())
                }
                if (customersToDelete.isNotEmpty()) {
                    val namesToDelete = customersToDelete.map { it.name }
                    firebaseRepository.deleteCustomers(namesToDelete)
                }
            }

            Result.success(mikrotikSecrets.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkMikrotikSecretDiff(currentCustomers: List<CustomerItem>): Boolean {
        return try {
            val routers = getConfiguredRouters()
            if (routers.isEmpty()) return false

            val session = getUserSession()
            val isAdmin = session.role.uppercase() == "ADMIN"
            val profileToFilter = if (isAdmin) "" else session.assignedProfile.trim()

            val allFetchedSecrets = mutableListOf<MikrotikSecret>()

            coroutineScope {
                val deferredList = routers.map { router ->
                    async {
                        withTimeoutOrNull(3000) {
                            try {
                                val baseUrl = buildMikrotikBaseUrl(router.ip)
                                val authHeader = buildBasicAuth(router.user, router.pass)
                                val secretUrl = "${baseUrl}secret"

                                val queryProfile = if (isAdmin) null else (if (profileToFilter.isBlank()) null else profileToFilter.trim())
                                var secretsResp = mikrotikApi.getSecrets(secretUrl, authHeader, queryProfile)

                                if (secretsResp.isSuccessful && secretsResp.body().orEmpty().isEmpty() && queryProfile != null) {
                                    val fallbackResp = mikrotikApi.getSecrets(secretUrl, authHeader, null)
                                    if (fallbackResp.isSuccessful && !fallbackResp.body().isNullOrEmpty()) {
                                        secretsResp = fallbackResp
                                    }
                                }

                                if (secretsResp.isSuccessful && secretsResp.body() != null) {
                                    secretsResp.body()!!
                                } else emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        } ?: emptyList()
                    }
                }
                val results = deferredList.awaitAll()
                results.forEach { allFetchedSecrets.addAll(it) }
            }

            var mikrotikSecrets = allFetchedSecrets.distinctBy { it.name.trim().lowercase() }

            if (!isAdmin && profileToFilter.isNotBlank()) {
                val allowedProfiles = profileToFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                mikrotikSecrets = mikrotikSecrets.filter { secret ->
                    allowedProfiles.any { p -> (secret.profile ?: "").trim().lowercase() == p }
                }
            }

            if (mikrotikSecrets.isEmpty()) return false

            val mikrotikNameSet = mikrotikSecrets.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val existingNameSet = currentCustomers.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()

            val hasNewSecrets = mikrotikNameSet.any { !existingNameSet.contains(it) }
            val hasDeletedSecrets = existingNameSet.any { !mikrotikNameSet.contains(it) }

            hasNewSecrets || hasDeletedSecrets
        } catch (e: Exception) {
            false
        }
    }

    suspend fun executeIsolirMassal(unpaidCustomers: List<CustomerItem>): Result<Int> {
        return try {
            val routers = getConfiguredRouters()
            if (routers.isEmpty()) {
                return Result.failure(Exception("Belum ada router MikroTik terkonfigurasi!"))
            }

            var countSuccess = 0

            for (customer in unpaidCustomers) {
                val matchingRouter = routers.find { r ->
                    r.targetProfile.isNotBlank() && r.targetProfile.split(",").any { p -> customer.profile.contains(p.trim(), ignoreCase = true) }
                } ?: routers.firstOrNull()

                if (matchingRouter != null && matchingRouter.ip.isNotBlank()) {
                    if (matchingRouter.version == "v6") {
                        val secrets = mikrotikv6SocketApi.getSecrets(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, customer.profile)
                        val matchSecret = secrets.find { it.name.equals(customer.name, ignoreCase = true) }
                        val secretId = matchSecret?.id ?: customer.name

                        mikrotikv6SocketApi.patchSecret(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, secretId, true)

                        val activeSessions = mikrotikv6SocketApi.getActiveSessions(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, customer.name)
                        val matchSession = activeSessions.find { it.name.equals(customer.name, ignoreCase = true) }
                        if (matchSession != null && matchSession.id.isNotBlank()) {
                            mikrotikv6SocketApi.deleteActiveSession(matchingRouter.ip, matchingRouter.apiPort, matchingRouter.user, matchingRouter.pass, matchSession.id)
                        }
                        countSuccess++
                    } else {
                        val baseUrl = buildMikrotikBaseUrl(matchingRouter.ip, matchingRouter.apiPort)
                        val authHeader = buildBasicAuth(matchingRouter.user, matchingRouter.pass)

                        val secretUrl = "${baseUrl}secret"
                        val queryProfile = customer.profile.ifBlank { matchingRouter.targetProfile }.ifBlank { null }
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
                }
            }

            Result.success(countSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
