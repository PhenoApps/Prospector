package org.phenoapps.prospector.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationV3: Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {

        with (database) {

            createNewScansTable()

        }
    }

    //adding color column to spectral frames table
    private fun SupportSQLiteDatabase.createNewScansTable() {

        execSQL("ALTER TABLE spectral_frames ADD COLUMN color TEXT")

    }
}