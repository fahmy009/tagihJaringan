package com.fahms.tagihjaringan.data.remote

import com.fahms.tagihjaringan.data.model.DeleteCustomersRequest
import com.fahms.tagihjaringan.data.model.DeleteUserAccountRequest
import com.fahms.tagihjaringan.data.model.GoogleScriptBaseResponse
import com.fahms.tagihjaringan.data.model.GoogleScriptInitialResponse
import com.fahms.tagihjaringan.data.model.LoginRequest
import com.fahms.tagihjaringan.data.model.LoginResponse
import com.fahms.tagihjaringan.data.model.SaveSettingsRequest
import com.fahms.tagihjaringan.data.model.SaveUserAccountRequest
import com.fahms.tagihjaringan.data.model.SubmitSetoranRequest
import com.fahms.tagihjaringan.data.model.SyncNewCustomersRequest
import com.fahms.tagihjaringan.data.model.TutupBukuRequest
import com.fahms.tagihjaringan.data.model.UpdateNominalRequest
import com.fahms.tagihjaringan.data.model.UpdatePaymentRequest
import com.fahms.tagihjaringan.data.model.UpdateProfileRequest
import com.fahms.tagihjaringan.data.model.VerifySetoranRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface GoogleScriptApi {

    @GET
    suspend fun getData(
        @Url url: String
    ): Response<GoogleScriptInitialResponse>

    @POST
    suspend fun login(
        @Url url: String,
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST
    suspend fun updateUserProfile(
        @Url url: String,
        @Body request: UpdateProfileRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun saveUserAccount(
        @Url url: String,
        @Body request: SaveUserAccountRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun deleteUserAccount(
        @Url url: String,
        @Body request: DeleteUserAccountRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun submitSetoran(
        @Url url: String,
        @Body request: SubmitSetoranRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun verifySetoran(
        @Url url: String,
        @Body request: VerifySetoranRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun updatePayment(
        @Url url: String,
        @Body request: UpdatePaymentRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun updateNominal(
        @Url url: String,
        @Body request: UpdateNominalRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun saveSettings(
        @Url url: String,
        @Body request: SaveSettingsRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun tutupBuku(
        @Url url: String,
        @Body request: TutupBukuRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun syncNewCustomers(
        @Url url: String,
        @Body request: SyncNewCustomersRequest
    ): Response<GoogleScriptBaseResponse>

    @POST
    suspend fun deleteCustomers(
        @Url url: String,
        @Body request: DeleteCustomersRequest
    ): Response<GoogleScriptBaseResponse>
}
