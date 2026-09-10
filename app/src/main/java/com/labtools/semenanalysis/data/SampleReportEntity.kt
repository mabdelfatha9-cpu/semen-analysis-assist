package com.labtools.semenanalysis.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * One row per analyzed sample. This table IS the "self-learning library":
 * every row a technician reviews and (optionally) corrects becomes a
 * labeled data point. On its own, Room does not retrain anything — export
 * this table (see SampleReportDao.getAllOnce) and use it as the labeled
 * dataset when you train a real detection/tracking model later, per
 * backend/README.md.
 */
@Entity(tableName = "sample_reports")
@TypeConverters(StringListConverter::class)
data class SampleReportEntity(
    @PrimaryKey val sampleId: String,
    val timestampEpochMillis: Long,

    // Concentration (Phase 1)
    val framesAnalyzed: Int,
    val averageObjectsPerFrame: Double,
    val estimatedConcentrationMillionPerMl: Double,
    val concentrationConfidenceScore: Double,
    val concentrationBelowReferenceLimit: Boolean,
    val whoConcentrationLimitMillionPerMl: Double,

    // Motility (Phase 2) — nullable: older rows or failed motility calls
    // simply won't have these filled in.
    val progressiveMotilityPercent: Double? = null,
    val nonProgressiveMotilityPercent: Double? = null,
    val immotilePercent: Double? = null,
    val totalMotilityPercent: Double? = null,
    val motilityConfidenceScore: Double? = null,
    val motilityBelowReferenceLimit: Boolean? = null,
    val tracksAnalyzed: Int? = null,

    val warnings: List<String> = emptyList(),

    // Human-in-the-loop review — this is the actual "learning" mechanism.
    val reviewedByHuman: Boolean = false,
    val humanCorrectedConcentration: Double? = null,
    val humanCorrectedProgressiveMotilityPercent: Double? = null,
    val reviewerNote: String? = null
)

/** Stores List<String> warnings as a single delimited column. */
class StringListConverter {
    private val delimiter = "\u0001"

    @TypeConverter
    fun fromList(list: List<String>?): String = list?.joinToString(delimiter) ?: ""

    @TypeConverter
    fun toList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(delimiter)
}
