package com.fahms.tagihjaringan

import com.fahms.tagihjaringan.data.model.CustomerItem
import com.fahms.tagihjaringan.data.model.GoogleScriptSettings
import com.fahms.tagihjaringan.data.model.MikrotikSecret
import com.fahms.tagihjaringan.data.model.PotonganItem
import com.fahms.tagihjaringan.data.remote.FlexibleBooleanAdapter
import com.fahms.tagihjaringan.data.remote.FlexibleIntAdapter
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLogicTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Boolean::class.java, FlexibleBooleanAdapter())
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, FlexibleBooleanAdapter())
        .registerTypeAdapter(Int::class.java, FlexibleIntAdapter())
        .registerTypeAdapter(Int::class.javaPrimitiveType, FlexibleIntAdapter())
        .create()

    @Test
    fun testSemverVersionComparison() {
        val updateManager = com.fahms.tagihjaringan.data.remote.GitHubUpdateManager(OkHttpClient())

        assertTrue(updateManager.isVersionNewer("1.0.1", "1.0.0"))
        assertTrue(updateManager.isVersionNewer("1.1.0", "1.0.5"))
        assertTrue(updateManager.isVersionNewer("2.0.0", "1.9.9"))
        assertFalse(updateManager.isVersionNewer("1.0.0", "1.0.0"))
        assertFalse(updateManager.isVersionNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun testHasilBersihCalculation() {
        val customers = listOf(
            CustomerItem(rowIndex = 1, name = "Ahmad", nominal = 100000, bayar = true),
            CustomerItem(rowIndex = 2, name = "Budi", nominal = 150000, bayar = true),
            CustomerItem(rowIndex = 3, name = "Cici", nominal = 200000, bayar = false)
        )

        val potonganList = listOf(
            PotonganItem(name = "Internet", amount = 50000),
            PotonganItem(name = "Listrik", amount = 20000)
        )

        val totalPemasukanLunas = customers.filter { it.bayar }.sumOf { it.nominal }
        val totalPotongan = potonganList.sumOf { it.amount }
        val hasilBersih = totalPemasukanLunas - totalPotongan

        assertEquals(250000, totalPemasukanLunas)
        assertEquals(70000, totalPotongan)
        assertEquals(180000, hasilBersih)
    }

    @Test
    fun testStatusFilterLogic() {
        val customers = listOf(
            CustomerItem(rowIndex = 1, name = "Ahmad", bayar = true),
            CustomerItem(rowIndex = 2, name = "Budi", bayar = false),
            CustomerItem(rowIndex = 3, name = "Cici", bayar = true)
        )

        val lunasList = customers.filter { it.bayar }
        val belumBayarList = customers.filter { !it.bayar }

        assertEquals(2, lunasList.size)
        assertEquals(1, belumBayarList.size)
        assertEquals("Budi", belumBayarList.first().name)
    }

    @Test
    fun testSearchQueryFilter() {
        val customers = listOf(
            CustomerItem(rowIndex = 1, name = "Ahmad Susanto", profile = "5mbps", modem = "TOTOLINK", comment = "ODP HAR"),
            CustomerItem(rowIndex = 2, name = "Budi Santoso", profile = "10mbps", modem = "TENDA", comment = "ODP AMELIA"),
            CustomerItem(rowIndex = 3, name = "Charlie", profile = "5mbps", modem = "F477v2", comment = "Fiberhome")
        )

        val query = "tenda"
        val filtered = customers.filter { customer ->
            customer.name.contains(query, ignoreCase = true) ||
            customer.comment.contains(query, ignoreCase = true) ||
            customer.modem.contains(query, ignoreCase = true) ||
            customer.profile.contains(query, ignoreCase = true)
        }

        assertEquals(1, filtered.size)
        assertEquals("Budi Santoso", filtered.first().name)
    }

    @Test
    fun testAlphabeticalSorting() {
        val unsortedCustomers = listOf(
            CustomerItem(name = "desi"),
            CustomerItem(name = "adel"),
            CustomerItem(name = "Budi"),
            CustomerItem(name = "ameliaSGP")
        )

        val sorted = unsortedCustomers.sortedBy { it.name.lowercase() }

        assertEquals("adel", sorted[0].name)
        assertEquals("ameliaSGP", sorted[1].name)
        assertEquals("Budi", sorted[2].name)
        assertEquals("desi", sorted[3].name)
    }

    @Test
    fun testModemExtractionAndServerNominalLogic() {
        val defaultNominalSetting = 95000

        val comment1 = "ODP AMELIA - PETTUNG - TENDA"
        val isServer1 = comment1.contains("server", ignoreCase = true)
        val nominal1 = if (isServer1) 0 else defaultNominalSetting
        val modem1 = comment1.split(Regex("[\\s-]+")).filter { it.isNotBlank() }.lastOrNull() ?: ""

        assertFalse(isServer1)
        assertEquals(95000, nominal1)
        assertEquals("TENDA", modem1)

        val comment2 = "SERVER - TOTOLINK"
        val isServer2 = comment2.contains("server", ignoreCase = true)
        val nominal2 = if (isServer2) 0 else defaultNominalSetting
        val modem2 = comment2.split(Regex("[\\s-]+")).filter { it.isNotBlank() }.lastOrNull() ?: ""

        assertTrue(isServer2)
        assertEquals(0, nominal2)
        assertEquals("TOTOLINK", modem2)

        val comment3 = "ODP PARTELON - SG - F477v2"
        val modem3 = comment3.split(Regex("[\\s-]+")).filter { it.isNotBlank() }.lastOrNull() ?: ""
        assertEquals("F477v2", modem3)
    }

    @Test
    fun testPerAccountCollectedByIsolation() {
        val customers = listOf(
            CustomerItem(name = "Ahmad", nominal = 100000, bayar = true, collectedBy = "tagih1"),
            CustomerItem(name = "Budi", nominal = 95000, bayar = true, collectedBy = "tagih1"),
            CustomerItem(name = "Cici", nominal = 150000, bayar = true, collectedBy = "mitra1"),
            CustomerItem(name = "Dani", nominal = 95000, bayar = false, collectedBy = "")
        )

        val tagih1Total = customers.filter { it.bayar && it.collectedBy.equals("tagih1", ignoreCase = true) }.sumOf { it.nominal }
        val mitra1Total = customers.filter { it.bayar && it.collectedBy.equals("mitra1", ignoreCase = true) }.sumOf { it.nominal }

        assertEquals(195000, tagih1Total)
        assertEquals(150000, mitra1Total)
    }

    @Test
    fun testFullMirrorSyncDeltaIdentification() {
        val currentSheetsCustomers = listOf(
            CustomerItem(name = "adel"),
            CustomerItem(name = "ameliaSGP"),
            CustomerItem(name = "oldCustomerRemovedInMikrotik")
        )

        val activeMikrotikSecrets = listOf(
            MikrotikSecret(name = "adel"),
            MikrotikSecret(name = "ameliaSGP"),
            MikrotikSecret(name = "newCustomerAddedInMikrotik")
        )

        val mikrotikNameSet = activeMikrotikSecrets.map { it.name.trim().lowercase() }.toSet()
        val existingSheetsNames = currentSheetsCustomers.map { it.name.trim().lowercase() }.toSet()

        val newToSync = activeMikrotikSecrets.filter { !existingSheetsNames.contains(it.name.trim().lowercase()) }
        val toDelete = currentSheetsCustomers.filter { !mikrotikNameSet.contains(it.name.trim().lowercase()) }

        assertEquals(1, newToSync.size)
        assertEquals("newCustomerAddedInMikrotik", newToSync.first().name)

        assertEquals(1, toDelete.size)
        assertEquals("oldCustomerRemovedInMikrotik", toDelete.first().name)
    }

    @Test
    fun testIsolirMassalUnpaidFilter() {
        val customers = listOf(
            CustomerItem(name = "Ahmad", bayar = true),
            CustomerItem(name = "Budi", bayar = false),
            CustomerItem(name = "Cici", bayar = false)
        )

        val unpaidList = customers.filter { !it.bayar }
        assertEquals(2, unpaidList.size)
        assertEquals("Budi", unpaidList[0].name)
        assertEquals("Cici", unpaidList[1].name)
    }

    @Test
    fun testRowIndexMappingFromId() {
        val jsonWithId = """
            {
                "id": 5,
                "name": "dani",
                "comment": "ODP PARTELON",
                "modem": "TOTOLINK",
                "profile": "SUMBERGADUNG",
                "nominal": 95000,
                "bayar": "",
                "collectedBy": "tagih1"
            }
        """.trimIndent()

        val customer = gson.fromJson(jsonWithId, CustomerItem::class.java)
        assertEquals(5, customer.rowIndex)
        assertEquals("dani", customer.name)
        assertEquals("tagih1", customer.collectedBy)
    }

    @Test
    fun testPotonganSettingsDeserialization() {
        val jsonSettings = """
            {
                "potongan": [
                    { "id": "potA", "name": "Internet", "amount": 50000 },
                    { "id": "potB", "name": "Gaji", "amount": 100000 }
                ]
            }
        """.trimIndent()

        val settings = gson.fromJson(jsonSettings, GoogleScriptSettings::class.java)
        assertEquals(2, settings.potongan.size)
        assertEquals("Internet", settings.potongan[0].name)
        assertEquals(50000, settings.potongan[0].amount)
        assertEquals("Gaji", settings.potongan[1].name)
        assertEquals(100000, settings.potongan[1].amount)
    }

    @Test
    fun testFlexibleBooleanAndIntDeserialization() {
        val jsonPaidNumeric = """
            {
                "rowIndex": 1,
                "name": "ameliaSGP",
                "comment": "ODP AMELIA",
                "modem": "TENDA",
                "profile": "SUMBERGADUNG",
                "nominal": 95000,
                "bayar": 95000
            }
        """.trimIndent()

        val customer1 = gson.fromJson(jsonPaidNumeric, CustomerItem::class.java)
        assertEquals("ameliaSGP", customer1.name)
        assertEquals(95000, customer1.nominal)
        assertTrue(customer1.bayar)
        assertEquals("TENDA", customer1.modem)

        val jsonFreeCustomer = """
            {
                "rowIndex": 2,
                "name": "adel",
                "comment": "SERVER - TOTOLINK",
                "modem": "TOTOLINK",
                "profile": "SUMBERGADUNG",
                "nominal": 0,
                "bayar": 0
            }
        """.trimIndent()

        val customer2 = gson.fromJson(jsonFreeCustomer, CustomerItem::class.java)
        assertEquals("adel", customer2.name)
        assertEquals(0, customer2.nominal)
        assertTrue(customer2.bayar)

        val jsonUnpaidCustomer = """
            {
                "rowIndex": 3,
                "name": "ases",
                "comment": "ODP HAR",
                "modem": "TENDA",
                "profile": "SUMBERGADUNG",
                "nominal": "95.000",
                "bayar": null
            }
        """.trimIndent()

        val customer3 = gson.fromJson(jsonUnpaidCustomer, CustomerItem::class.java)
        assertEquals("ases", customer3.name)
        assertEquals(95000, customer3.nominal)
        assertFalse(customer3.bayar)
    }
}
