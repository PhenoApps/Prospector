package org.phenoapps.prospector.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.phenoapps.prospector.data.dao.ProspectorDao
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame

@Database(entities = [Experiment::class, Scan::class, SpectralFrame::class, Sample::class], version = 1)
abstract class ProspectorDatabase : RoomDatabase() {

    abstract fun expScanDao(): ProspectorDao

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