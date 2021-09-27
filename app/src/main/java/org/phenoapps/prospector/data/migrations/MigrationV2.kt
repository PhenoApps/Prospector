package org.phenoapps.prospector.data.migrations

import android.database.sqlite.SQLiteException
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationV2: Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {

        with (database) {

            createNewScansTable()

        }
    }

    //backup technique for deleting columns in SQLite https://www.sqlite.org/faq.html#q11
    private fun SupportSQLiteDatabase.createNewScansTable() {

        execSQL("CREATE TEMP TABLE scans_backup(eid INT NOT NULL, name TEXT NOT NULL, date TEXT NOT NULL, deviceType TEXT NOT NULL, color TEXT, deviceId TEXT, alias TEXT, operator TEXT, lightSource INT, sid INTEGER PRIMARY KEY AUTOINCREMENT)")

        execSQL("INSERT INTO scans_backup SELECT eid, name, date, deviceType, color, deviceId, alias, operator, lightSource, sid FROM scans")

        execSQL("DROP TABLE scans")

        execSQL("CREATE TABLE scans(eid INT NOT NULL, name TEXT NOT NULL, date TEXT NOT NULL, deviceType TEXT NOT NULL, color TEXT, deviceId TEXT, alias TEXT, operator TEXT, lightSource INT, sid INTEGER PRIMARY KEY AUTOINCREMENT, FOREIGN KEY (eid, name) REFERENCES samples(eid, name) ON UPDATE CASCADE ON DELETE CASCADE)")

        execSQL("INSERT INTO scans SELECT eid, name, date, deviceType, color, deviceId, alias, operator, lightSource, sid FROM scans_backup")

        execSQL("DROP TABLE scans_backup")

        execSQL("CREATE INDEX index_scans_eid ON scans (eid)")

        execSQL("CREATE INDEX index_scans_name ON scans (name)")

    }
}