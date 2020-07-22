//package org.phenoapps.prospector
//
//import android.content.ContentValues
//import android.content.Context
//import android.database.Cursor
//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteOpenHelper
//import android.media.MediaScannerConnection
//import android.os.Environment
//import android.util.Log
//import java.io.File
//import java.io.FileOutputStream
//import java.io.OutputStream
//
//class DatabaseManager(context: Context?) : SQLiteOpenHelper(context, TABLE_NAME, null, 12) {
//    override fun onCreate(db: SQLiteDatabase) {
//        val experimentTable = "CREATE TABLE " + EXPERIMENT_TABLE + " (" +
//                EXP0 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                EXP1 + " TEXT)"
//
//        // does this get called in each instance or only once?
//        val createTable = "CREATE TABLE " + TABLE_NAME + " (" +
//                COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                COL1 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
//                COL2 + " TEXT, " +
//                COL3 + " TEXT, " +
//                COL4 + " TEXT, " +
//                COL5 + " TEXT, " +
//                COL6 + " TEXT, " +
//                COL7 + " TEXT, " +
//                COL8 + " TEXT, " +
//                COL9 + " TEXT, " +
//                COL10 + " TEXT, " +
//                COL11 + " TEXT, " +
//                COL12 + " INTEGER," +
//                "FOREIGN KEY (" + COL12 + ") REFERENCES scans(experiment) ON DELETE CASCADE)" //added experiment column
//        db.execSQL(experimentTable)
//        db.execSQL(createTable)
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
//        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
//        onCreate(db)
//    }
//
//    fun insertExperiment(name: String?) {
//
//        val db = this.writableDatabase
//
//        val contentValues = ContentValues()
//
//        // insert "experiment"
//        contentValues.put(EXP1, name)
//
//        db.replace(EXPERIMENT_TABLE, null, contentValues)
//    }
//
//    fun insertData_fromLinkSquare(item: String): Boolean {
//        /**
//         * Data is passed to DB in the following format:
//         * COL2 = "deviceID";
//         * COL3 = "observationUnitID";
//         * COL4 = "observationUnitName";
//         * COL5 = "observationUnitBarcode";
//         * COL6 = "frameNumber";
//         * COL7 = "lightSource";
//         * COL8 = "spectralValuesCount";
//         * COL9 = "spectralValues";
//         *
//         * NOTE: this format should NOT CHANGE!!
//         * The DatabaseManager parser is dependant on correctly formatted data!!
//         */
//        val parts = item.split("~".toRegex()).toTypedArray()
//        val db = this.writableDatabase
//        val contentValues = ContentValues()
//
//        // insert "deviceID"
//        contentValues.put(COL2, parts[0])
//
//        // insert "observationUnitID"
//        contentValues.put(COL3, parts[1])
//
//        // insert "observationUnitName"
//        contentValues.put(COL4, parts[2])
//
//        // insert "observationUnitBarcode"
//        contentValues.put(COL5, parts[3])
//
//        // insert "frameNumber"
//        contentValues.put(COL6, parts[4])
//
//        // insert "lightSource"
//        contentValues.put(COL7, parts[5])
//
//        // insert "spectralValuesCount"
//        contentValues.put(COL8, parts[6])
//
//        // insert "spectralValues"
//        contentValues.put(COL9, parts[7])
//
//        // insert "scanNote"
//        contentValues.put(COL11, parts[8])
//
////        TODO: get current experiment and add to DB contentValues.put(COL12, parts[9]);
//        Log.d(TAG, "addData: Adding $item to $TABLE_NAME")
//        val result = db.insert(TABLE_NAME, null, contentValues)
//        return result != -1L
//    }
//
//    fun insertData_fromDatabaseCSV(data: String): Boolean {
//        val parts = data.split(",".toRegex()).toTypedArray()
//        val contentValues = ContentValues()
//        contentValues.put(COL1, parts[1])
//        contentValues.put(COL2, parts[2])
//        contentValues.put(COL3, parts[3])
//        contentValues.put(COL4, parts[4])
//        contentValues.put(COL5, parts[5])
//        contentValues.put(COL6, parts[6])
//        contentValues.put(COL7, parts[7])
//        contentValues.put(COL8, parts[8])
//        contentValues.put(COL9, parts[9])
//        contentValues.put(COL10, parts[10])
//        contentValues.put(COL11, parts[11])
//
//        //add experiment column
//        contentValues.put(COL12, parts[12])
//        val db = this.writableDatabase
//        val result = db.insert(TABLE_NAME, null, contentValues)
//        return result != -1L
//    }
//
//    val experiments: Cursor
//        get() {
//            val db = this.readableDatabase
//            return db.query(EXPERIMENT_TABLE, null, null, null, null, null, null)
//        }
//
//    val all: Cursor
//        get() {
//            val db = this.writableDatabase
//            val query = "SELECT * FROM $TABLE_NAME"
//            return db.rawQuery(query, null)
//        }
//
//    fun deleteAll() {
//        val db = this.writableDatabase
//        val query = "DELETE FROM $TABLE_NAME"
//        db.execSQL(query)
//    }
//
//    fun deleteExperiment(eid: Int) {
//        val db = this.writableDatabase
//        val query = "DELETE FROM $EXPERIMENT_TABLE WHERE $EXP0 = '$eid'"
//        db.execSQL(query)
//    }
//
//    val count_id: Int
//        get() {
//            val db = this.writableDatabase
//            val query = "SELECT COUNT($COL0) FROM $TABLE_NAME"
//            val data = db.rawQuery(query, null)
//            data.moveToNext()
//            return data.getInt(0)
//        }
//
//    val count_observationScanName: Int
//        get() {
//            val db = this.writableDatabase
//            val query = "SELECT COUNT(DISTINCT $COL4) FROM $TABLE_NAME"
//            val data = db.rawQuery(query, null)
//            data.moveToNext()
//            return data.getInt(0)
//        }
//
//    //TODO fix import to not read experiment col
//    fun getAll_observationUnitName(experiment: Int): Cursor {
//        val db = this.writableDatabase
//        val query = "SELECT $COL4 FROM $TABLE_NAME WHERE $COL12 = '$experiment'"
//        return db.query(TABLE_NAME, null, "experiment = ?", arrayOf("test"), null, null, null)
//    }
//
//    fun update_observationUnitName(oldObservationUnitName: String, newObservationUnitName: String) {
//        val db = this.writableDatabase
//        val query = "UPDATE " + TABLE_NAME + " SET " + COL4 + " = '" +
//                newObservationUnitName + "' WHERE " + COL4 + " = '" + oldObservationUnitName + "'"
//        db.execSQL(query)
//    }
//
//    fun delete_observationUnitName(observationUnitName: String) {
//        val db = this.writableDatabase
//        val query = "DELETE FROM $TABLE_NAME WHERE $COL4 = '$observationUnitName'"
//        Log.d(TAG, "deleteName: query: $query")
//        db.execSQL(query)
//    }
//
//    fun isUnique_observationUnitName(observationUnitName: String, experiment: Int): Boolean {
//        val observationUnitNames = getAll_observationUnitName(experiment)
//        while (observationUnitNames.moveToNext()) {
//            if (observationUnitNames.getString(0) == observationUnitName) {
//                return false
//            }
//        }
//        return true
//    }
//
//    fun get_spectralValues_byObservationUnitName(observationUnitName: String): Cursor {
//        val db = this.writableDatabase
//        val query = "SELECT " + COL9 + " FROM " + TABLE_NAME +
//                " WHERE " + COL4 + " = '" + observationUnitName + "'"
//        return db.rawQuery(query, null)
//    }
//
//    fun getSpectralValues(eid: Int, oid: Int): Cursor {
//        val db = this.readableDatabase
//        return db.query(TABLE_NAME, null, "experiment = ?, localDatabaseID = ?", arrayOf(eid.toString(), oid.toString()), null, null, null)
//    }
//
//    fun get_spectralValues_byID(ID: Int): Cursor {
//        val db = this.writableDatabase
//        val query = "SELECT " + COL9 + " FROM " + TABLE_NAME +
//                " WHERE " + COL0 + " = '" + ID + "'"
//        return db.rawQuery(query, null)
//    }
//
//    fun get_ID_byObservationUnitName(observationUnitName: String): Cursor {
//        val db = this.writableDatabase
//        val query = "SELECT " + COL0 + " FROM " + TABLE_NAME +
//                " WHERE " + COL4 + " = '" + observationUnitName + "'"
//        return db.rawQuery(query, null)
//    }
//
//    //TODO add experiment string to export
//    fun export_toDatabaseCSV(): File? {
//        val data = all
//        var data_string: String?
//        var output: String
//        try {
//            // Create the directory and file
//            val sdCard = Environment.getExternalStorageDirectory()
//            val dir = File(sdCard.absoluteFile.toString() + "/Download")
//            dir.mkdirs()
//            val csv_file = File(dir, "Log.csv")
//            csv_file.createNewFile()
//            val out = FileOutputStream(csv_file)
//
//            // Store column names into output
//            output = ""
//            for (i in 0 until data.columnCount) {
//                output += data.getColumnName(i)
//                output += ","
//            }
//            output += "\n"
//
//            // Store column data into output
//            while (data.moveToNext()) {
//                for (i in 0 until data.columnCount) {
//                    data_string = data.getString(i)
//                    output += data_string ?: " "
//                    output += ","
//                }
//                output += "\n"
//            }
//
//            // Write data to output file
//            out.write(output.toByteArray()) // NOTE: this is most efficient if done as few times as possible
//
//            // Close the file
//            out.close()
//            return csv_file
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }
//
//    //TODO add experiment string to export
//    fun export_toDatabaseCSV_withOutputStream(outputStream: OutputStream) {
//        val data = all
//        var data_string: String?
//        var output: String
//        try {
//            // Store column names into output
//            output = ""
//            for (i in 0 until data.columnCount) {
//                output += data.getColumnName(i)
//                output += ","
//            }
//            output += "\n"
//
//            // Store column data into output
//            while (data.moveToNext()) {
//                for (i in 0 until data.columnCount) {
//                    data_string = data.getString(i)
//                    output += data_string ?: " "
//                    output += ","
//                }
//                output += "\n"
//            }
//
//            // Write data to output file
//            outputStream.write(output.toByteArray())
//
//            // Close the file
//            outputStream.flush()
//            outputStream.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    //TODO add experiment string to export
//    fun export_toSCiO(): File? {
//        // Based on: https://cassava-test.sgn.cornell.edu/breeders/nirs/
//        val data = all
//        val output = StringBuilder() // TODO: figure out if storing all data in one string is okay procedure
//        try {
//            // Create the directory and file
//            val sdCard = Environment.getExternalStorageDirectory()
//            val dir = File(sdCard.absoluteFile.toString() + "/Download")
//            dir.mkdirs()
//            val csv_file = File(dir, "Log_SCiO.csv")
//            csv_file.createNewFile()
//            val out = FileOutputStream(csv_file)
//
//            // add metadata
//            output.append("""
//    name,testName
//
//    """.trimIndent())
//            output.append("""
//    user,testUser
//
//    """.trimIndent())
//            output.append("""
//    description,dummyData
//
//    """.trimIndent())
//            output.append("num_records,$count_id\n")
//            output.append("num_samples,$count_observationScanName\n")
//            output.append("""
//    num_auto_attributes,0
//
//    """.trimIndent())
//            output.append("""
//    num_custom_attributes,0
//
//    """.trimIndent())
//            output.append("num_wavelengths,$CURRENT_LINKSQURE_LENGTH\n")
//            output.append("wavelengths_start,$CURRENT_LINKSQURE_START\n")
//            output.append("""
//    wavelengths_resolution,1
//
//    """.trimIndent())
//
//            // add column names
//            output.append("id,sample_id,sampling_time,User_input_id,device_id,comment,temperature,location,outlier,")
//            for (i in 0 until CURRENT_LINKSQURE_LENGTH) {
//                output.append("spectrum_$i + $CURRENT_LINKSQURE_START,")
//            }
//            output.append("\n")
//
//            // add column types
//            output.append("int,unicode,datetime,unicode,unicode,NoneType,float,NoneType,str,")
//            for (i in 0 until CURRENT_LINKSQURE_LENGTH) {
//                output.append("float,")
//            }
//            output.append("\n")
//
//            // add data
//            while (data.moveToNext()) {
//                output.append(data.getString(0) + ",") // id
//                output.append(data.getString(0) + ",") // sample_id
//                output.append(data.getString(1) + ",") // sampling_time
//                output.append(data.getString(4) + ",") // User_input_id
//                output.append(data.getString(2) + ",") // device_id
//                output.append("None,") // comment
//                output.append("0,") // temperature
//                output.append("None,") // location
//                output.append("no,") // outlier
//
//                // spectral values
//                val spectralValues = data.getString(9).split(" ".toRegex()).toTypedArray()
//                for (i in spectralValues.indices) {
//                    output.append(spectralValues[i] + ",")
//                }
//                output.append("\n")
//            }
//
//            // Write data to output file
//            out.write(output.toString().toByteArray()) // NOTE: this is most efficient if done as few times as possible
//
//            // Close the file
//            out.close()
//            return csv_file
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }
//
//    //TODO add experiment string to export
//    fun export_toSCiO_withOutputStream(outputStream: OutputStream) {
//        // Based on: https://cassava-test.sgn.cornell.edu/breeders/nirs/
//        val data = all
//        val output = StringBuilder() // TODO: figure out if storing all data in one string is okay procedure
//        try {
//            // add metadata
//            output.append("""
//    name,testName
//
//    """.trimIndent())
//            output.append("""
//    user,testUser
//
//    """.trimIndent())
//            output.append("""
//    description,dummyData
//
//    """.trimIndent())
//            output.append("num_records,$count_id\n")
//            output.append("num_samples,$count_observationScanName\n")
//            output.append("""
//    num_auto_attributes,0
//
//    """.trimIndent())
//            output.append("""
//    num_custom_attributes,0
//
//    """.trimIndent())
//            output.append("num_wavelengths,$CURRENT_LINKSQURE_LENGTH\n")
//            output.append("wavelengths_start,$CURRENT_LINKSQURE_START\n")
//            output.append("""
//    wavelengths_resolution,1
//
//    """.trimIndent())
//
//            // add column names
//            output.append("id,sample_id,sampling_time,User_input_id,device_id,comment,temperature,location,outlier,")
//            for (i in 0 until CURRENT_LINKSQURE_LENGTH) {
//                output.append("spectrum_$i + $CURRENT_LINKSQURE_START,")
//            }
//            output.append("\n")
//
//            // add column types
//            output.append("int,unicode,datetime,unicode,unicode,NoneType,float,NoneType,str,")
//            for (i in 0 until CURRENT_LINKSQURE_LENGTH) {
//                output.append("float,")
//            }
//            output.append("\n")
//
//            // add data
//            while (data.moveToNext()) {
//                output.append(data.getString(0) + ",") // id
//                output.append(data.getString(0) + ",") // sample_id
//                output.append(data.getString(1) + ",") // sampling_time
//                output.append(data.getString(4) + ",") // User_input_id
//                output.append(data.getString(2) + ",") // device_id
//                output.append("None,") // comment
//                output.append("0,") // temperature
//                output.append("None,") // location
//                output.append("no,") // outlier
//
//                // spectral values
//                val spectralValues = data.getString(9).split(" ".toRegex()).toTypedArray()
//                for (i in spectralValues.indices) {
//                    output.append(spectralValues[i] + ",")
//                }
//                output.append("\n")
//            }
//
//            // Write data to output file
//            outputStream.write(output.toString().toByteArray()) // NOTE: this is most efficient if done as few times as possible
//
//            // Close the file
//            outputStream.flush()
//            outputStream.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    //TODO add experiment string to export
//    fun export_toBrAPI(): File? {
//        // Based on: https://github.com/plantbreeding/API/issues/399
//        val data = all
//        val output = StringBuilder()
//        try {
//            // Create the directory and file
//            val sdCard = Environment.getExternalStorageDirectory()
//            val dir = File(sdCard.absoluteFile.toString() + "/Download")
//            dir.mkdirs()
//            val csv_file = File(dir, "Log_BrAPI.json")
//            csv_file.createNewFile()
//            val out = FileOutputStream(csv_file)
//
//            // open Json object
//            output.append("\"log\": [\n")
//
//            // formatting looks bad here so that it looks nice after export
//            while (data.moveToNext()) {
//                output.append("{\n")
//                output.append("""	"deviceID": "${data.getString(2)}",
//""") // deviceID
//                output.append("""	"timestamp": "${data.getString(1)}",
//""") // timestamp
//                output.append("""	"observationUnitID": "${data.getString(3)}",
//""") // observationUnitID
//                output.append("""	"observationUnitName": "${data.getString(4)}",
//""") // observationUnitName
//                output.append("""	"observationUnitBarcode": "${data.getString(5)}",
//""") // observationUnitBarcode
//                output.append("""	"scanDbID": "${data.getString(10)}",
//""") // scanDbID
//                output.append("""	"scanPUI": "${data.getString(0)}",
//""") // scanPUI
//
//                // spectralValues
//                output.append("\t\"spectralValues\": [\n\t\t")
//                val spectralValues = data.getString(9).split(" ".toRegex()).toTypedArray()
//                for (i in 0..4) {
//                    output.append("""{
//			"wavelength": ${CURRENT_LINKSQURE_START + i},
//""")
//                    output.append("""			"spectralValue": ${spectralValues[i]}
//		},
//		""")
//                }
//                output.append("]\n\t},\n")
//            }
//            output.append("]")
//
//            // Write data to output file
//            out.write(output.toString().toByteArray()) // NOTE: this is most efficient if done as few times as possible
//
//            // Close the file
//            out.close()
//            return csv_file
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }
//
//    //TODO add experiment string to export
//    fun export_toBrAPI_withOutputStream(outputStream: OutputStream) {
//        // Based on: https://github.com/plantbreeding/API/issues/399
//        val data = all
//        val output = StringBuilder()
//        try {
//            // open Json object
//            output.append("\"log\": [\n")
//
//            // formatting looks bad here so that it looks nice after export
//            while (data.moveToNext()) {
//                output.append("{\n")
//                output.append("""	"deviceID": "${data.getString(2)}",
//""") // deviceID
//                output.append("""	"timestamp": "${data.getString(1)}",
//""") // timestamp
//                output.append("""	"observationUnitID": "${data.getString(3)}",
//""") // observationUnitID
//                output.append("""	"observationUnitName": "${data.getString(4)}",
//""") // observationUnitName
//                output.append("""	"observationUnitBarcode": "${data.getString(5)}",
//""") // observationUnitBarcode
//                output.append("""	"scanDbID": "${data.getString(10)}",
//""") // scanDbID
//                output.append("""	"scanPUI": "${data.getString(0)}",
//""") // scanPUI
//
//                // spectralValues
//                output.append("\t\"spectralValues\": [\n\t\t")
//                val spectralValues = data.getString(9).split(" ".toRegex()).toTypedArray()
//                for (i in 0..4) {
//                    output.append("""{
//			"wavelength": ${CURRENT_LINKSQURE_START + i},
//""")
//                    output.append("""			"spectralValue": ${spectralValues[i]}
//		},
//		""")
//                }
//                output.append("]\n\t},\n")
//            }
//            output.append("]")
//
//            // Write data to output file
//            outputStream.write(output.toString().toByteArray()) // NOTE: this is most efficient if done as few times as possible
//
//            // Close the file
//            outputStream.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    companion object {
//        private const val TAG = "DatabaseManager"
//
//        //experiment table
//        private const val EXPERIMENT_TABLE = "experiments"
//        private const val EXP0 = "eid"
//        private const val EXP1 = "name"
//
//        //Main table
//        private const val TABLE_NAME = "scans"
//        private const val COL0 = "localDatabaseID"
//        private const val COL1 = "scanTime"
//        private const val COL2 = "deviceID"
//        private const val COL3 = "observationUnitID"
//        private const val COL4 = "observationUnitName"
//        private const val COL5 = "observationUnitBarcode"
//        private const val COL6 = "frameNumber"
//        private const val COL7 = "lightSource"
//        private const val COL8 = "spectralValuesCount"
//        private const val COL9 = "spectralValues"
//        private const val COL10 = "serverDatabaseID"
//        private const val COL11 = "scanNote"
//
//        //TODO add column for 'person' or 'operator'
//        //added experiment column foreign key to experiment table
//        private const val COL12 = "experiment"
//        private const val CURRENT_LINKSQURE_LENGTH = 600 // TODO: remove dependance on this
//        private const val CURRENT_LINKSQURE_START = 400
//        fun scanFile(ctx: Context?, filepath: File) {
//            MediaScannerConnection.scanFile(ctx, arrayOf(filepath.absolutePath), null, null)
//        }
//    }
//}