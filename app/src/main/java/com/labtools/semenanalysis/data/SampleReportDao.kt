package com.labtools.semenanalysis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: SampleReportEntity)

    @Update
    suspend fun update(report: SampleReportEntity)

    @Query("SELECT * FROM sample_reports WHERE sampleId = :sampleId LIMIT 1")
    suspend fun getById(sampleId: String): SampleReportEntity?

    /** Live list for the History/Library screen, newest first. */
    @Query("SELECT * FROM sample_reports ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<SampleReportEntity>>

    /**
     * One-shot snapshot of every reviewed sample — this is the export point
     * for building a labeled training set for a future real detector/tracker.
     * Unreviewed rows are excluded because an uncorrected computer guess is
     * not a ground-truth label.
     */
    @Query("SELECT * FROM sample_reports WHERE reviewedByHuman = 1")
    suspend fun getAllReviewed(): List<SampleReportEntity>

    @Query("DELETE FROM sample_reports WHERE sampleId = :sampleId")
    suspend fun delete(sampleId: String)
}
