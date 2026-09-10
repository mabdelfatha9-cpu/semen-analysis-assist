package com.labtools.semenanalysis.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

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

    val estimatedNormalFormsPercent: Double? = null,
    val morphologyConfidenceScore: Double? = null,

    val warnings: List<String> = emptyList(),

    // —— Clinical / patient meta ——
    val patientName: String? = null,
    val patientAge: String? = null,
    val patientSex: String? = null,
    val referredBy: String? = null,
    val centre: String? = null,
    val patientId: String? = null,
    /** Abstinence period in days (مدة الامتناع). */
    val abstinenceDays: Double? = null,

    // —— Physical examination ——
    val colour: String? = null,
    val semenPh: Double? = null,
    val volumeMl: Double? = null,
    val viscosity: String? = null,
    val appearance: String? = null,
    val liquefactionTimeMin: Double? = null,

    // Optional extra micro fields entered by lab
    val fructose: String? = null,
    val vitalityAlivePercent: Double? = null,
    val vitalityDeadPercent: Double? = null,
    val pusCells: String? = null,
    val roundCells: String? = null,

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
