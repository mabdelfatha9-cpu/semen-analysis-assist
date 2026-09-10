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

interface ApiService {

    /**
     * Upload a photo of a stage micrometer to compute microns-per-pixel.
     */
    @Multipart
    @POST("calibrate")
    suspend fun calibrate(
        @Part image: MultipartBody.Part,
        @Part("known_distance_microns") knownDistanceMicrons: RequestBody
    ): Response<CalibrationResponse>

    /**
     * Upload a short video of the sample (recorded through the eyepiece)
     * plus calibration + chamber depth, and receive back a concentration
     * report per WHO 6th-edition reference limits.
     */
    @Multipart
    @POST("analyze/concentration")
    suspend fun analyzeConcentration(
        @Part video: MultipartBody.Part,
        @Part("microns_per_pixel") micronsPerPixel: RequestBody,
        @Part("chamber_depth_microns") chamberDepthMicrons: RequestBody,
        @Part("dilution_factor") dilutionFactor: RequestBody
    ): Response<AnalysisResponse>

    /**
     * Uses the SAME video already uploaded for concentration to classify
     * motility (progressive / non-progressive / immotile) via frame-to-frame
     * tracking on the backend.
     */
    @Multipart
    @POST("analyze/motility")
    suspend fun analyzeMotility(
        @Part video: MultipartBody.Part,
        @Part("microns_per_pixel") micronsPerPixel: RequestBody
    ): Response<MotilityResponse>
}
