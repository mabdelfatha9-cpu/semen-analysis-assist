package com.labtools.semenanalysis.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * One row per analyzed sample. This table IS the "self-learning library":
 * technician reviews + morphology corrections become labeled data points
 * for future model training.
 */
@Entity(tableName = "sample_reports")
@TypeConverters(StringListConverter::class)
data class SampleReportEntity(
    @PrimaryKey val sampleId: String,
    val timestampEpochMillis: Long,

    val framesAnalyzed: Int,
    val averageObjectsPerFrame: Double,
    val estimatedConcentrationMillionPerMl: Double,
    val concentrationConfidenceScore: Double,
    val concentrationBelowReferenceLimit: Boolean,
    val whoConcentrationLimitMillionPerMl: Double,

    val progressiveMotilityPercent: Double? = null,
    val nonProgressiveMotilityPercent: Double? = null,
    val immotilePercent: Double? = null,
    val totalMotilityPercent: Double? = null,
    val motilityConfidenceScore: Double? = null,
    val motilityBelowReferenceLimit: Boolean? = null,
    val tracksAnalyzed: Int? = null,

    // Morphology (forms) — AI estimate and/or human correction
    val estimatedNormalFormsPercent: Double? = null,
    val morphologyConfidenceScore: Double? = null,

    val warnings: List<String> = emptyList(),

    val reviewedByHuman: Boolean = false,
    val humanCorrectedConcentration: Double? = null,
    val humanCorrectedProgressiveMotilityPercent: Double? = null,
    val humanCorrectedNormalFormsPercent: Double? = null,
    val reviewerNote: String? = null
)

class StringListConverter {
    private val delimiter = "\u0001"

    @TypeConverter
    fun fromList(list: List<String>?): String = list?.joinToString(delimiter) ?: ""

    @TypeConverter
    fun toList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(delimiter)
}
