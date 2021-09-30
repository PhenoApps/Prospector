package org.phenoapps.prospector.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.*
import org.phenoapps.prospector.data.dao.ExperimentDao
import org.phenoapps.prospector.data.dao.SampleDao
import org.phenoapps.prospector.data.dao.ScanDao
import org.phenoapps.prospector.data.migrations.MigrationV2
import org.phenoapps.prospector.data.migrations.MigrationV3
import org.phenoapps.prospector.data.models.*
import java.io.File

@Database(entities = [Experiment::class, Scan::class, SpectralFrame::class, Sample::class],
        views = [SampleScanCount::class, SampleFramesCount::class, DeviceTypeExport::class],
    version = 3, exportSchema = true)
abstract class ProspectorDatabase : RoomDatabase() {


    private fun ifExists(path: String): Boolean {

        val db = File(path)

        if (db.exists()) {

            return true

        }

        db.parent?.let {

            val dir = File(it)

            if (!dir.exists()) {

                dir.mkdirs()

            }
        }


        return false
    }

    /**
     * The database must enable foreign keys using pragma to cascade deletes.
     */
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

    abstract fun experimentDao(): ExperimentDao
    abstract fun sampleDao(): SampleDao
    abstract fun scanDao(): ScanDao

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
                .addMigrations(MigrationV2())
                .addMigrations(MigrationV3())
                .build()

        }
    }
}