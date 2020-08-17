package org.phenoapps.prospector.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.Room
import androidx.room.RoomDatabase
import org.phenoapps.prospector.data.dao.ProspectorDao
import org.phenoapps.prospector.data.models.*
import java.io.File


@Database(entities = [Experiment::class, Scan::class, SpectralFrame::class, Sample::class],
        views = [SampleScanCount::class], version = 1)
abstract class ProspectorDatabase : RoomDatabase() {


    private fun ifExists(path: String): Boolean {

        val db = File(path)

        if (db.exists()) {

            return true

        }

        val dir = File(db.parent)

        if (!dir.exists()) {

            dir.mkdirs()

        }

        return false
    }

    override fun init(configuration: DatabaseConfiguration) {

        val path = configuration.context.getDatabasePath("PROSPECTOR").path

        if (ifExists(path)) {

            SQLiteDatabase.openDatabase(configuration.context
                    .getDatabasePath("PROSPECTOR").path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE)?.let { db ->

                db.rawQuery("PRAGMA foreign_keys=ON;", null).close()

                db.close()
            }

        }

        super.init(configuration)
    }

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