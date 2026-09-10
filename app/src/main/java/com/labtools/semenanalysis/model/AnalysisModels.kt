package com.labtools.semenanalysis.model

/**
 * WHO Laboratory Manual for the Examination and Processing of Human Semen,
 * 6th Edition (2021) — lower reference limits (5th percentile).
 * Source verified against the published manual and secondary literature
 * citing it as of 2026. Re-verify against the official manual before any
 * clinical use; WHO occasionally issues corrigenda.
 */
object WhoReferenceLimits6thEdition {
    const val VOLUME_ML = 1.4
    const val CONCENTRATION_MILLION_PER_ML = 16.0
    const val TOTAL_COUNT_MILLION = 39.0
    const val PROGRESSIVE_MOTILITY_PERCENT = 30.0
    const val TOTAL_MOTILITY_PERCENT = 42.0
    const val NORMAL_MORPHOLOGY_PERCENT = 4.0
    const val PH_MIN = 7.2
}

/**
 * Result of a one-time calibration against a stage micrometer.
 * Must be redone whenever magnification, phone, or eyepiece coupling changes.
 */
data class CalibrationData(
    val micronsPerPixel: Double,
    val chamberDepthMicrons: Double,
    val calibratedAtEpochMillis: Long,
    val deviceLabel: String
)

/**
 * A single detected object (candidate sperm cell) in one analyzed frame,
 * as returned by the backend's OpenCV blob-detection pipeline.
 */
data class DetectedObject(
    val xPixel: Float,
    val yPixel: Float,
    val areaPixels: Float,
    val circularity: Float
)

/**
 * Concentration-focused report returned by the backend for Phase 1.
 * Motility/morphology fields are placeholders for later phases.
 */
data class AnalysisReport(
    val sampleId: String,
    val timestampEpochMillis: Long,
    val framesAnalyzed: Int,
    val averageObjectsPerFrame: Double,
    val estimatedConcentrationMillionPerMl: Double,
    val confidenceScore: Double, // 0.0 - 1.0, heuristic from detection stability across frames
    val belowReferenceLimit: Boolean,
    val warnings: List<String>,
    val reviewedByHuman: Boolean = false,
    val humanCorrectedConcentration: Double? = null
)
