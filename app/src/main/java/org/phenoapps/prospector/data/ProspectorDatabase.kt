package org.phenoapps.prospector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.phenoapps.prospector.data.dao.ExperimentScansDao
import org.phenoapps.prospector.data.models.*

@Database(entities = [Experiment::class, Scan::class, SpectralFrame::class],
        views = [ExperimentScans::class, ScanSpectralValues::class], version = 1)
abstract class ProspectorDatabase : RoomDatabase() {

    abstract fun expScanDao(): ExperimentScansDao

    companion object {

        //singleton pattern
        @Volatile private var instance: ProspectorDatabase? = null

        fun getInstance(ctx: Context): ProspectorDatabase {

            return instance ?: synchronized(this) {

                instance ?: buildDatabase(ctx).also { instance = it }
            }
        }

        private fun buildDatabase(ctx: Context): ProspectorDatabase {

            return Room.databaseBuilder(ctx, ProspectorDatabase::class.java, "PROSPECTOR")
                    .build()
        }
    }
}