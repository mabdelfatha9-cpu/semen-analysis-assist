package com.labtools.semenanalysis.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class CalibrationResponse(
    val micronsPerPixel: Double,
    val warnings: List<String>
)

data class AnalysisResponse(
    val sampleId: String,
    val framesAnalyzed: Int,
    val averageObjectsPerFrame: Double,
    val estimatedConcentrationMillionPerMl: Double,
    val confidenceScore: Double,
    val belowReferenceLimit: Boolean,
    val whoReferenceLimitMillionPerMl: Double,
    val warnings: List<String>
)

data class MotilityResponse(
    val sampleId: String,
    val tracksAnalyzed: Int,
    val progressiveMotilityPercent: Double,
    val nonProgressiveMotilityPercent: Double,
    val immotilePercent: Double,
    val totalMotilityPercent: Double,
    val confidenceScore: Double,
    val belowReferenceLimit: Boolean,
    val whoTotalMotilityLimitPercent: Double,
    val whoProgressiveMotilityLimitPercent: Double,
    val warnings: List<String>
)

data class MorphologyResponse(
    val sampleId: String,
    val normalFormsPercent: Double,
    val confidenceScore: Double,
    val warnings: List<String>
)

interface ApiService {

    @Multipart
    @POST("calibrate")
    suspend fun calibrate(
        @Part image: MultipartBody.Part,
        @Part("known_distance_microns") knownDistanceMicrons: RequestBody
    ): Response<CalibrationResponse>

    @Multipart
    @POST("analyze/concentration")
    suspend fun analyzeConcentration(
        @Part video: MultipartBody.Part,
        @Part("microns_per_pixel") micronsPerPixel: RequestBody,
        @Part("chamber_depth_microns") chamberDepthMicrons: RequestBody,
        @Part("dilution_factor") dilutionFactor: RequestBody
    ): Response<AnalysisResponse>

    /** Single still image concentration (no motility). */
    @Multipart
    @POST("analyze/concentration-image")
    suspend fun analyzeConcentrationImage(
        @Part image: MultipartBody.Part,
        @Part("microns_per_pixel") micronsPerPixel: RequestBody,
        @Part("chamber_depth_microns") chamberDepthMicrons: RequestBody,
        @Part("dilution_factor") dilutionFactor: RequestBody
    ): Response<AnalysisResponse>

    @Multipart
    @POST("analyze/motility")
    suspend fun analyzeMotility(
        @Part video: MultipartBody.Part,
        @Part("microns_per_pixel") micronsPerPixel: RequestBody
    ): Response<MotilityResponse>

    @Multipart
    @POST("analyze/morphology")
    suspend fun analyzeMorphology(
        @Part image: MultipartBody.Part
    ): Response<MorphologyResponse>
}
