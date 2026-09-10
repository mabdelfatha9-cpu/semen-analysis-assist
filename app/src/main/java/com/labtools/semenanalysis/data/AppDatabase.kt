package com.labtools.semenanalysis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SampleReportEntity::class], version = 1, exportSchema = false)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sampleReportDao(): SampleReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "semen_analysis_library.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
