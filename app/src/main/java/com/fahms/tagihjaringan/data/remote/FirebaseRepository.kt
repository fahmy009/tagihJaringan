package com.fahms.tagihjaringan.data.remote

import com.fahms.tagihjaringan.data.local.UserSession
import com.fahms.tagihjaringan.data.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private var firestore: FirebaseFirestore? = null

    private var customersListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var setoranListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var laporanListener: ListenerRegistration? = null
    private var routersListener: ListenerRegistration? = null

    init {
        try {
            val app = FirebaseApp.getInstance()
            firestore = FirebaseFirestore.getInstance(app)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rebindFirestore(projectId: String, apiKey: String, appId: String): Boolean {
        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .build()

            val existingApp = try { FirebaseApp.getInstance("DynamicApp") } catch (e: Exception) { null }
            val app = if (existingApp != null) {
                existingApp.delete()
                FirebaseApp.initializeApp(FirebaseApp.getInstance().applicationContext, options, "DynamicApp")
            } else {
                FirebaseApp.initializeApp(FirebaseApp.getInstance().applicationContext, options, "DynamicApp")
            }

            firestore = FirebaseFirestore.getInstance(app)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private val customersRef get() = firestore?.collection("customers")
    private val usersRef get() = firestore?.collection("users")
    private val setoranRef get() = firestore?.collection("setoran")
    private val settingsRef get() = firestore?.collection("settings")
    private val laporanRef get() = firestore?.collection("laporan")
    private val routersRef get() = firestore?.collection("routers")

    suspend fun ensureDefaultAccounts() {
        val ref = usersRef ?: return
        try {
            val adminDoc = ref.document("admin").get().await()
            if (!adminDoc.exists()) {
                val batch = firestore?.batch() ?: return

                val adminRef = ref.document("admin")
                batch.set(adminRef, mapOf(
                    "username" to "admin",
                    "password" to "admin",
                    "role" to "ADMIN",
                    "assignedProfile" to "",
                    "name" to "Administrator",
                    "remoteJalur" to "remote_client_1"
                ))

                val mitraDoc = ref.document("mitra1").get().await()
                if (!mitraDoc.exists()) {
                    val mitraRef = ref.document("mitra1")
                    batch.set(mitraRef, mapOf(
                        "username" to "mitra1",
                        "password" to "1234",
                        "role" to "MITRA",
                        "assignedProfile" to "SUMBERGADUNG",
                        "name" to "Mitra Sumbergadung",
                        "remoteJalur" to "remote_client_1"
                    ))
                }

                val tagihDoc = ref.document("tagih1").get().await()
                if (!tagihDoc.exists()) {
                    val tagihRef = ref.document("tagih1")
                    batch.set(tagihRef, mapOf(
                        "username" to "tagih1",
                        "password" to "1234",
                        "role" to "COLLECTOR",
                        "assignedProfile" to "SUMBERGADUNG",
                        "name" to "Tukang Tagih A",
                        "remoteJalur" to "remote_client_1"
                    ))
                }

                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observeCustomers(onData: (List<CustomerItem>) -> Unit) {
        val ref = customersRef ?: return
        customersListener?.remove()
        customersListener = ref.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: doc.id
                    val comment = doc.getString("comment") ?: ""
                    val modem = doc.getString("modem") ?: ""
                    val profile = doc.getString("profile") ?: ""
                    val nominal = doc.getLong("nominal")?.toInt() ?: 0
                    val bayar = doc.getBoolean("bayar") ?: false
                    val collectedBy = doc.getString("collectedBy") ?: ""
                    val rowIndex = doc.getLong("rowIndex")?.toInt() ?: (doc.id.hashCode() and 0x7FFFFFFF)
                    val lat = doc.getDouble("lat") ?: 0.0
                    val lng = doc.getDouble("lng") ?: 0.0

                    CustomerItem(
                        rowIndex = rowIndex,
                        name = name,
                        comment = comment,
                        modem = modem,
                        profile = profile,
                        nominal = nominal,
                        bayar = bayar,
                        collectedBy = collectedBy,
                        lat = lat,
                        lng = lng
                    )
                }.distinctBy { it.name.trim() }
                onData(list)
            }
        }
    }

    fun observeUsers(onData: (List<UserAccount>) -> Unit) {
        val ref = usersRef ?: return
        usersListener?.remove()
        usersListener = ref.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val username = doc.getString("username") ?: doc.id
                    val role = doc.getString("role") ?: "ADMIN"
                    val profile = doc.getString("assignedProfile") ?: ""
                    val name = doc.getString("name") ?: username
                    UserAccount(
                        username = username,
                        role = role,
                        assignedProfile = profile,
                        name = name
                    )
                }
                onData(list)
            }
        }
    }

    fun observeSetoran(onData: (List<SetoranItem>) -> Unit) {
        val ref = setoranRef ?: return
        setoranListener?.remove()
        setoranListener = ref.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val date = doc.getString("date") ?: ""
                    val cName = doc.getString("collectorName") ?: ""
                    val mName = doc.getString("mitraName") ?: ""
                    val profile = doc.getString("profile") ?: ""
                    val amount = doc.getLong("amount")?.toInt() ?: 0
                    val sisa = doc.getLong("sisaPegangan")?.toInt() ?: 0
                    val status = doc.getString("status") ?: "PENDING"
                    val rec = doc.getLong("amountReceived")?.toInt() ?: amount
                    val hutang = doc.getLong("selisihHutang")?.toInt() ?: 0

                    SetoranItem(
                        id = id,
                        date = date,
                        collectorName = cName,
                        mitraName = mName,
                        profile = profile,
                        amount = amount,
                        sisaPegangan = sisa,
                        status = status,
                        amountReceived = rec,
                        selisihHutang = hutang
                    )
                }
                onData(list)
            }
        }
    }

    fun observeSettings(onData: (GoogleScriptSettings) -> Unit) {
        val ref = settingsRef ?: return
        settingsListener?.remove()
        settingsListener = ref.document("potongan").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val rawPotongan = snapshot.get("potongan") as? List<Map<String, Any>>
                val potonganList = rawPotongan?.map { map ->
                    PotonganItem(
                        id = map["id"]?.toString() ?: java.util.UUID.randomUUID().toString(),
                        name = map["name"]?.toString() ?: "",
                        amount = (map["amount"] as? Number)?.toInt() ?: 0,
                        profile = map["profile"]?.toString() ?: ""
                    )
                } ?: emptyList()
                onData(GoogleScriptSettings(potongan = potonganList))
            }
        }
    }

    fun observeLaporanBulanan(onData: (List<LaporanBulananItem>) -> Unit) {
        val ref = laporanRef ?: return
        laporanListener?.remove()
        laporanListener = ref.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    LaporanBulananItem(
                        id = doc.getString("id") ?: doc.id,
                        periode = doc.getString("periode") ?: "",
                        countLunas = doc.getLong("countLunas")?.toInt() ?: 0,
                        grossIncome = doc.getLong("grossIncome")?.toInt() ?: 0,
                        totalPotongan = doc.getLong("totalPotongan")?.toInt() ?: 0,
                        netIncome = doc.getLong("netIncome")?.toInt() ?: 0,
                        profile = doc.getString("profile") ?: "",
                        date = doc.getString("date") ?: ""
                    )
                }
                onData(list)
            }
        }
    }

    fun observeRouters(onData: (List<MikrotikRouter>) -> Unit) {
        val ref = routersRef ?: return
        routersListener?.remove()
        routersListener = ref.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    MikrotikRouter(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        ip = doc.getString("ip") ?: "",
                        user = doc.getString("user") ?: "",
                        pass = doc.getString("pass") ?: "",
                        targetProfile = doc.getString("targetProfile") ?: "",
                        version = doc.getString("version") ?: "v7",
                        apiPort = doc.getLong("apiPort")?.toInt() ?: 8728
                    )
                }
                onData(list)
            }
        }
    }

    suspend fun getRoutersOnce(): List<MikrotikRouter> {
        val ref = routersRef ?: return emptyList()
        return try {
            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { doc ->
                MikrotikRouter(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    ip = doc.getString("ip") ?: "",
                    user = doc.getString("user") ?: "",
                    pass = doc.getString("pass") ?: "",
                    targetProfile = doc.getString("targetProfile") ?: "",
                    version = doc.getString("version") ?: "v7",
                    apiPort = doc.getLong("apiPort")?.toInt() ?: 8728
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveMikrotikRouter(router: MikrotikRouter) {
        val ref = routersRef ?: return
        val data = mapOf(
            "id" to router.id,
            "name" to router.name,
            "ip" to router.ip,
            "user" to router.user,
            "pass" to router.pass,
            "targetProfile" to router.targetProfile,
            "version" to router.version,
            "apiPort" to router.apiPort
        )
        ref.document(router.id).set(data, SetOptions.merge()).await()
    }

    suspend fun deleteMikrotikRouter(routerId: String) {
        val ref = routersRef ?: return
        ref.document(routerId).delete().await()
    }

    suspend fun updatePaymentStatus(rowIndex: Int, customerName: String, customerProfile: String, isChecked: Boolean, nominal: Int, collectedBy: String) {
        val ref = customersRef ?: return
        val docId = customerName.trim().ifBlank { rowIndex.toString() }
        val data = mapOf(
            "name" to customerName,
            "profile" to customerProfile,
            "bayar" to isChecked,
            "nominal" to nominal,
            "collectedBy" to if (isChecked) collectedBy else "",
            "rowIndex" to rowIndex
        )
        ref.document(docId).set(data, SetOptions.merge()).await()
    }

    suspend fun updateCustomerLocation(customerName: String, lat: Double, lng: Double) {
        val ref = customersRef ?: return
        val docId = customerName.trim().ifBlank { System.currentTimeMillis().toString() }
        val data = mapOf(
            "lat" to lat,
            "lng" to lng
        )
        ref.document(docId).set(data, SetOptions.merge()).await()
    }

    suspend fun updateCustomerNominal(rowIndex: Int, customerName: String, newNominal: Int) {
        val ref = customersRef ?: return
        val docId = customerName.trim().ifBlank { rowIndex.toString() }
        ref.document(docId).set(mapOf("nominal" to newNominal, "rowIndex" to rowIndex), SetOptions.merge()).await()
    }

    suspend fun updateCustomerRemotePort(rowIndex: Int, customerName: String, remotePort: Int) {
        val ref = customersRef ?: return
        val docId = customerName.trim().ifBlank { rowIndex.toString() }
        ref.document(docId).set(mapOf("remotePort" to remotePort, "rowIndex" to rowIndex), SetOptions.merge()).await()
    }

    suspend fun saveFinancialSettings(potonganList: List<PotonganItem>) {
        val ref = settingsRef ?: return
        val dataList = potonganList.map { p ->
            mapOf("id" to p.id, "name" to p.name, "amount" to p.amount, "profile" to p.profile)
        }
        ref.document("potongan").set(mapOf("potongan" to dataList)).await()
    }

    suspend fun tutupBuku(periode: String, countLunas: Int, grossIncome: Int, totalPotongan: Int, netIncome: Int, profile: String) {
        val db = firestore ?: return
        val ref = customersRef ?: return
        val snapshot = ref.get().await()
        if (!snapshot.isEmpty) {
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                val nominal = doc.getLong("nominal")?.toInt() ?: 0
                if (nominal != 0) {
                    batch.update(doc.reference, mapOf("bayar" to false, "collectedBy" to ""))
                }
            }
            batch.commit().await()
        }

        val id = "lap_${System.currentTimeMillis()}"
        val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date())
        val lapData = mapOf(
            "id" to id,
            "periode" to periode,
            "countLunas" to countLunas,
            "grossIncome" to grossIncome,
            "totalPotongan" to totalPotongan,
            "netIncome" to netIncome,
            "profile" to profile,
            "date" to dateStr
        )
        laporanRef?.document(id)?.set(lapData)?.await()
    }

    suspend fun syncNewCustomers(newCustomers: List<SyncNewCustomerPayload>) {
        val db = firestore ?: return
        val ref = customersRef ?: return
        val batch = db.batch()
        var hasUpdates = false

        val existingDocsMap = try {
            val docs = ref.get().await().documents
            val map = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
            for (doc in docs) {
                map[doc.id.trim()] = doc
                map[doc.id.trim().lowercase()] = doc
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }

        for ((idx, c) in newCustomers.withIndex()) {
            val exactName = c.name.trim()
            val docId = exactName.ifBlank { "cust_${System.currentTimeMillis()}_$idx" }
            val docRef = ref.document(docId)
            val docSnap = existingDocsMap[exactName] ?: existingDocsMap[exactName.lowercase()]

            if (docSnap != null && docSnap.exists()) {
                val updateData = mutableMapOf<String, Any>(
                    "name" to c.name,
                    "profile" to c.profile,
                    "comment" to c.comment,
                    "modem" to c.modem
                )
                val currentNominal = docSnap.getLong("nominal")?.toInt() ?: 0
                if (currentNominal == 0) {
                    updateData["nominal"] = c.nominal
                }
                batch.set(docRef, updateData, SetOptions.merge())
            } else {
                val newCustomerData = mapOf(
                    "name" to c.name,
                    "profile" to c.profile,
                    "comment" to c.comment,
                    "modem" to c.modem,
                    "nominal" to c.nominal,
                    "bayar" to c.bayar,
                    "collectedBy" to "",
                    "rowIndex" to (System.currentTimeMillis() and 0x7FFFFFFF).toInt() + idx
                )
                batch.set(docRef, newCustomerData, SetOptions.merge())
            }
            hasUpdates = true
        }

        if (hasUpdates) {
            batch.commit().await()
        }
    }

    suspend fun deleteCustomers(names: List<String>) {
        val db = firestore ?: return
        val ref = customersRef ?: return
        if (names.isEmpty()) return

        val batch = db.batch()
        names.forEach { name ->
            if (name.isNotBlank()) {
                ref.document(name.trim()).delete()
                ref.document(name.trim().lowercase()).delete()
            }
        }
        batch.commit().await()
    }

    suspend fun submitSetoran(mitraName: String, amount: Int, sisaPegangan: Int, collectorUser: UserSession) {
        val ref = setoranRef ?: return
        val id = "setor_${System.currentTimeMillis()}"
        val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date())

        val setoranData = mapOf(
            "id" to id,
            "date" to dateStr,
            "collectorName" to (collectorUser.name.ifBlank { collectorUser.username }),
            "mitraName" to mitraName,
            "profile" to collectorUser.assignedProfile,
            "amount" to amount,
            "sisaPegangan" to sisaPegangan,
            "amountReceived" to amount,
            "selisihHutang" to 0,
            "status" to "PENDING"
        )
        ref.document(id).set(setoranData).await()
    }

    suspend fun verifySetoran(setoranId: String, amountReceived: Int, selisihHutang: Int) {
        val ref = setoranRef ?: return
        val data = mapOf(
            "status" to "VERIFIED",
            "amountReceived" to amountReceived,
            "selisihHutang" to selisihHutang
        )
        ref.document(setoranId).set(data, SetOptions.merge()).await()
    }

    suspend fun deleteSetoran(setoranId: String) {
        val ref = setoranRef ?: return
        ref.document(setoranId).delete().await()
    }

    suspend fun login(username: String, pass: String): UserAccount? {
        val ref = usersRef ?: return null
        val doc = ref.document(username.trim().lowercase()).get().await()
        if (doc.exists()) {
            val storedPass = doc.getString("password") ?: ""
            if (storedPass == pass.trim()) {
                return UserAccount(
                    username = doc.getString("username") ?: username,
                    role = doc.getString("role") ?: "ADMIN",
                    assignedProfile = doc.getString("assignedProfile") ?: "",
                    name = doc.getString("name") ?: username
                )
            }
        }
        return null
    }

    suspend fun updateUserProfile(username: String, newPass: String, newName: String) {
        val ref = usersRef ?: return
        val docRef = ref.document(username.trim().lowercase())
        val data = mutableMapOf<String, Any>()
        if (newPass.isNotBlank()) data["password"] = newPass.trim()
        if (newName.isNotBlank()) data["name"] = newName.trim()
        if (data.isNotEmpty()) {
            docRef.set(data, SetOptions.merge()).await()
        }
    }

    suspend fun saveUserAccount(username: String, pass: String, role: String, profile: String, name: String, oldUsername: String = "") {
        val ref = usersRef ?: return
        val newDocId = username.trim().lowercase()
        val oldDocId = oldUsername.trim().lowercase()

        if (oldDocId.isNotBlank() && oldDocId != newDocId) {
            val oldDoc = ref.document(oldDocId).get().await()
            val existingPass = oldDoc.getString("password") ?: ""
            val data = mutableMapOf<String, Any>(
                "username" to username.trim(),
                "password" to (pass.trim().ifBlank { existingPass }),
                "role" to role.trim().uppercase(),
                "assignedProfile" to profile.trim(),
                "name" to name.trim()
            )
            ref.document(newDocId).set(data).await()
            ref.document(oldDocId).delete().await()
            return
        }

        val docRef = ref.document(newDocId)
        val data = mutableMapOf<String, Any>(
            "username" to username.trim(),
            "role" to role.trim().uppercase(),
            "assignedProfile" to profile.trim(),
            "name" to name.trim()
        )
        if (pass.isNotBlank()) {
            data["password"] = pass.trim()
        }
        docRef.set(data, SetOptions.merge()).await()
    }

    suspend fun deleteUserAccount(username: String) {
        val ref = usersRef ?: return
        ref.document(username.trim().lowercase()).delete().await()
    }
}
