package com.fahms.tagihjaringan.data.model

import com.fahms.tagihjaringan.data.remote.FlexibleBooleanAdapter
import com.fahms.tagihjaringan.data.remote.FlexibleIntAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * Data model for Customer record from Google Apps Script Sheets
 */
data class CustomerItem(
    @SerializedName("rowIndex", alternate = ["id"])
    val rowIndex: Int = 0,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("comment")
    val comment: String = "",

    @SerializedName("modem")
    val modem: String = "",

    @SerializedName("profile")
    val profile: String = "",

    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("nominal")
    val nominal: Int = 0,

    @JsonAdapter(FlexibleBooleanAdapter::class)
    @SerializedName("bayar")
    val bayar: Boolean = false,

    @SerializedName("collectedBy")
    val collectedBy: String = "",

    @SerializedName("lat")
    val lat: Double = 0.0,

    @SerializedName("lng")
    val lng: Double = 0.0
)

/**
 * Monthly financial report snapshot item
 */
data class LaporanBulananItem(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("periode")
    val periode: String = "",

    @SerializedName("countLunas")
    val countLunas: Int = 0,

    @SerializedName("grossIncome")
    val grossIncome: Int = 0,

    @SerializedName("totalPotongan")
    val totalPotongan: Int = 0,

    @SerializedName("netIncome")
    val netIncome: Int = 0,

    @SerializedName("profile")
    val profile: String = "",

    @SerializedName("date")
    val date: String = ""
)

/**
 * Deduction item for financial management
 */
data class PotonganItem(
    @SerializedName("id")
    val id: String = java.util.UUID.randomUUID().toString(),

    @SerializedName("name")
    val name: String = "",

    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("amount")
    val amount: Int = 0,

    @SerializedName("profile")
    val profile: String = ""
)

/**
 * Setoran Item for deposit tracking between Collector & Mitra
 */
data class SetoranItem(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("date")
    val date: String = "",

    @SerializedName("collectorName")
    val collectorName: String = "",

    @SerializedName("mitraName")
    val mitraName: String = "",

    @SerializedName("profile")
    val profile: String = "",

    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("amount")
    val amount: Int = 0,

    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("sisaPegangan")
    val sisaPegangan: Int = 0,

    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("amountReceived")
    val amountReceived: Int = 0,

    @JsonAdapter(FlexibleIntAdapter::class)
    @SerializedName("selisihHutang")
    val selisihHutang: Int = 0,

    @SerializedName("status")
    val status: String = "PENDING" // PENDING, VERIFIED
)

/**
 * User Account model
 */
data class UserAccount(
    @SerializedName("username")
    val username: String = "",

    @SerializedName("role")
    val role: String = "ADMIN",

    @SerializedName("assignedProfile")
    val assignedProfile: String = "",

    @SerializedName("name")
    val name: String = ""
)

/**
 * Settings container returned by Google Apps Script
 */
data class GoogleScriptSettings(
    @SerializedName("potongan")
    val potongan: List<PotonganItem> = emptyList()
)

/**
 * Initial payload response from Google Apps Script (GET /exec?api=true)
 */
data class GoogleScriptInitialResponse(
    @SerializedName("settings")
    val settings: GoogleScriptSettings? = GoogleScriptSettings(),

    @SerializedName("customers")
    val customers: List<CustomerItem>? = emptyList(),

    @SerializedName("setoran")
    val setoran: List<SetoranItem>? = emptyList(),

    @SerializedName("users")
    val users: List<UserAccount>? = emptyList(),

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Standard API response from Google Apps Script
 */
data class GoogleScriptBaseResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("success")
    val success: Boolean = true
)

data class LoginResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("user")
    val user: UserAccount? = null
)

// Request payloads for Google Apps Script
data class LoginRequest(
    val action: String = "login",
    val username: String,
    val password: String
)

data class SubmitSetoranRequest(
    val action: String = "submitSetoran",
    val collectorName: String,
    val mitraName: String,
    val profile: String,
    val amount: Int,
    val sisaPegangan: Int
)

data class VerifySetoranRequest(
    val action: String = "verifySetoran",
    val setoranId: String,
    val amountReceived: Int = 0,
    val selisihHutang: Int = 0
)

data class UpdateProfileRequest(
    val action: String = "updateUserProfile",
    val username: String,
    val newPassword: String = "",
    val newName: String = ""
)

data class SaveUserAccountRequest(
    val action: String = "saveUserAccount",
    val username: String,
    val password: String = "",
    val role: String,
    val assignedProfile: String = "",
    val name: String = ""
)

data class DeleteUserAccountRequest(
    val action: String = "deleteUserAccount",
    val username: String
)

data class UpdatePaymentRequest(
    val action: String = "updatePayment",
    val rowIndex: Int,
    val isChecked: Boolean,
    val nominal: Int,
    val collectedBy: String = ""
)

data class UpdateNominalRequest(
    val action: String = "updateNominal",
    val rowIndex: Int,
    val nominal: Int
)

data class SaveSettingsRequest(
    val action: String = "saveSettings",
    val settings: GoogleScriptSettings
)

data class TutupBukuRequest(
    val action: String = "tutupBuku"
)

data class SyncNewCustomerPayload(
    val name: String,
    val profile: String,
    val comment: String = "",
    val modem: String = "",
    val nominal: Int = 0,
    val bayar: Boolean = false
)

data class SyncNewCustomersRequest(
    val action: String = "syncNewCustomers",
    val newCustomers: List<SyncNewCustomerPayload>
)

data class DeleteCustomersRequest(
    val action: String = "deleteCustomers",
    val names: List<String>
)

/**
 * MikroTik Secret representation from GET /rest/ppp/secret
 */
data class MikrotikSecret(
    @SerializedName(".id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("profile")
    val profile: String = "",

    @SerializedName("disabled")
    val disabled: String = "false",

    @SerializedName("comment")
    val comment: String? = ""
)

/**
 * MikroTik Active Session representation from GET /rest/ppp/active
 */
data class MikrotikActiveSession(
    @SerializedName(".id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("address")
    val address: String = "",

    @SerializedName("uptime")
    val uptime: String = ""
)
