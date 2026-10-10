package com.fahms.tagihjaringan.data.remote

import com.fahms.tagihjaringan.data.model.MikrotikActiveSession
import com.fahms.tagihjaringan.data.model.MikrotikSecret
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Query
import retrofit2.http.Url

interface MikrotikApi {

    @GET
    suspend fun getSecrets(
        @Url fullUrl: String,
        @Header("Authorization") authHeader: String,
        @Query("profile") profile: String? = null
    ): Response<List<MikrotikSecret>>

    @PATCH
    suspend fun patchSecret(
        @Url fullUrl: String,
        @Header("Authorization") authHeader: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET
    suspend fun getActiveSessions(
        @Url fullUrl: String,
        @Header("Authorization") authHeader: String,
        @Query("name") name: String? = null
    ): Response<List<MikrotikActiveSession>>

    @DELETE
    suspend fun deleteActiveSession(
        @Url fullUrl: String,
        @Header("Authorization") authHeader: String
    ): Response<Unit>
}
