package com.fahms.tagihjaringan.data.remote

import com.fahms.tagihjaringan.data.model.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Collection References
    private val customersRef: CollectionReference? get() = firestore?.collection("customers")
    private val usersRef: CollectionReference? get() = firestore?.collection("users")
    private val setoranRef: CollectionReference? get() = firestore?.collection("setoran")
    private val settingsRef: CollectionReference? get() = firestore?.collection("settings")
    private val laporanRef: CollectionReference? get() = firestore?.collection("laporan_bulanan")

    // Listeners
    private var customersListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var setoranListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var laporanListener: ListenerRegistration? = null

    suspend fun ensureDefaultAccounts() {
        val ref = usersRef ?: return
        try {
            val snapshot = ref.get().await()
            if (snapshot.isEmpty) {
                val batch = firestore?.batch() ?: return
                
                val adminRef = ref.document("admin")
                batch.set(adminRef, mapOf(
                    "username" to "admin",
                    "password" to "admin",
                    "role" to "ADMIN",
                    "assignedProfile" to "",
                    "name" to "Administrator"
                ))

                val mitraRef = ref.document("mitra1")
                batch.set(mitraRef, mapOf(
                    "username" to "mitra1",
                    "password" to "1234",
                    "role" to "MITRA",
                    "assignedProfile" to "SUMBERGADUNG",
                    "name" to "Mitra Sumbergadung"
                ))

                val tagihRef = ref.document("tagih1")
                batch.set(tagihRef, mapOf(
                    "username" to "tagih1",
                    "password" to "1234",
                    "role" to "COLLECTOR",
                    "assignedProfile" to "SUMBERGADUNG",
                    "name" to "Tukang Tagih A"
                ))

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
                }
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

    suspend fun updatePaymentStatus(rowIndex: Int, customerName: String, customerProfile: String, isChecked: Boolean, nominal: Int, collectedBy: String) {
        val ref = customersRef ?: return
        val docId = customerName.ifBlank { rowIndex.toString() }
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
        val docId = customerName.ifBlank { System.currentTimeMillis().toString() }
        val data = mapOf(
            "lat" to lat,
            "lng" to lng
        )
        ref.document(docId).set(data, SetOptions.merge()).await()
    }

    suspend fun updateCustomerNominal(rowIndex: Int, customerName: String, newNominal: Int) {
        val ref = customersRef ?: return
        val docId = customerName.ifBlank { rowIndex.toString() }
        ref.document(docId).set(mapOf("nominal" to newNominal, "rowIndex" to rowIndex), SetOptions.merge()).await()
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

        for ((idx, c) in newCustomers.withIndex()) {
            val docId = c.name.ifBlank { "cust_${System.currentTimeMillis()}_$idx" }
            val docRef = ref.document(docId)
            val docSnap = docRef.get().await()

            if (docSnap.exists()) {
                // Update comment, modem, and profile while preserving bayar, collectedBy, custom nominal, and GPS!
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
                // Brand new customer: initialize default fields
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
        val batch = db.batch()
        names.forEach { name ->
            if (name.isNotBlank()) {
                batch.delete(ref.document(name))
            }
        }
        batch.commit().await()
    }

    suspend fun authenticateUser(username: String, pass: String): UserAccount? {
        ensureDefaultAccounts()
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

    suspend fun saveUserAccount(username: String, pass: String, role: String, profile: String, name: String, oldUsername: String = "") {
        val ref = usersRef ?: return
        val newDocId = username.trim().lowercase()
        val oldDocId = oldUsername.trim().lowercase()

        // If renaming username, migration: copy data to new document and delete old document
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

    suspend fun submitSetoran(collectorName: String, mitraName: String, profile: String, amount: Int, sisaPegangan: Int) {
        val ref = setoranRef ?: return
        val id = "setor_${System.currentTimeMillis()}"
        val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date())
        val data = mapOf(
            "id" to id,
            "date" to dateStr,
            "collectorName" to collectorName,
            "mitraName" to mitraName,
            "profile" to profile,
            "amount" to amount,
            "sisaPegangan" to sisaPegangan,
            "status" to "PENDING",
            "amountReceived" to amount,
            "selisihHutang" to 0
        )
        ref.document(id).set(data).await()
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

    fun clearListeners() {
        customersListener?.remove()
        usersListener?.remove()
        setoranListener?.remove()
        settingsListener?.remove()
        laporanListener?.remove()
    }
}
