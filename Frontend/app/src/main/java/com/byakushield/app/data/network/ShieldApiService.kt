package com.byakushield.app.data.network

import com.byakushield.app.model.AuditHistoryResponse
import com.byakushield.app.model.LoginRequest
import com.byakushield.app.model.LoginResponse
import com.byakushield.app.model.OrchestratorResponse
import com.byakushield.app.model.PageResponse
import com.byakushield.app.model.QuishGuardRequest
import com.byakushield.app.model.RegisterRequest
import com.byakushield.app.model.TextArmorRequest
import com.byakushield.app.model.ThreatResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import okhttp3.RequestBody

interface ShieldApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Any>

    @POST("api/shield/text-armor")
    suspend fun scanText(
        @Body request: TextArmorRequest
    ): Response<ThreatResponse>

    @POST("api/shield/quish-guard")
    suspend fun scanUrl(
        @Body request: QuishGuardRequest
    ): Response<ThreatResponse>

    @Multipart
    @POST("api/shield/quish-guard/qr")
    suspend fun scanQr(
        @Part file: MultipartBody.Part
    ): Response<ThreatResponse>

    @Multipart
    @POST("api/shield/voice-shield")
    suspend fun scanAudio(
        @Part file: MultipartBody.Part
    ): Response<ThreatResponse>

    @Multipart
    @POST("api/shield/data-scrub")
    suspend fun scanDataScrub(
        @Part file: MultipartBody.Part
    ): Response<ThreatResponse>

    @GET("api/shield/audit-history")
    suspend fun getAuditHistory(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PageResponse<AuditHistoryResponse>>

    @Multipart
    @POST("api/shield/orchestrate")
    suspend fun orchestrate(
        @Part("text") text: RequestBody?,
        @Part("url") url: RequestBody?,
        @Part audio: MultipartBody.Part?,
        @Part media: MultipartBody.Part?,
        @Part qr: MultipartBody.Part?
    ): Response<OrchestratorResponse>
}