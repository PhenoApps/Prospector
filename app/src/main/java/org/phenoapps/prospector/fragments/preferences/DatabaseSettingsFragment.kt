package org.phenoapps.prospector.fragments.preferences

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.contracts.OpenDocumentFancy
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.utils.*
import org.phenoapps.utils.BaseDocumentTreeUtil
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

@AndroidEntryPoint
class DatabaseSettingsFragment : PreferenceFragmentCompat(), CoroutineScope by MainScope() {

    companion object {
        const val TAG = "DatabaseSettings"
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private val importLauncher = registerForActivityResult(OpenDocumentFancy()) { uri ->

        uri?.let { nonNullUri ->

            import(nonNullUri)
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->

        uri?.let { nonNullUri ->

            export(nonNullUri)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.database_preferences, rootKey)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference<Preference>(mKeyUtil.importDatabase)?.let { pref ->

            pref.setOnPreferenceClickListener { _ ->

                importUserChoice()

                true
            }

        }

        findPreference<Preference>(mKeyUtil.exportDatabase)?.let { pref ->

            context?.let { ctx ->

                pref.setOnPreferenceClickListener { _ ->

                    if (BaseDocumentTreeUtil.isEnabled(ctx)) {

                        exportDocument(ctx)

                    } else exportUserChoice()

                    true
                }
            }
        }

        findPreference<Preference>(mKeyUtil.deleteDatabase)?.let { pref ->

            context?.let { ctx ->

                pref.setOnPreferenceClickListener { _ ->

                    (activity as? MainActivity)?.askDeleteDatabase {

                        launch {

                            withContext(Dispatchers.Default) {

                                ProspectorDatabase.getInstance(ctx)
                                    .clearAllTables()

                            }

                            activity?.runOnUiThread {

                                (activity as? MainActivity)?.notify(R.string.database_reset_message)

                            }
                        }
                    }


                    true
                }
            }
        }
    }

    private fun export(uri: Uri) {

        context?.let { ctx ->

            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            val tempPref = File(ctx.cacheDir, "prospector_preferences.xml")
            val fos = FileOutputStream(tempPref)
            ObjectOutputStream(fos).use { oos ->

                oos.writeObject(prefs.all)
            }
            fos.close()

            DocumentFile.fromFile(tempPref).let { prefFile ->

                if (prefFile.exists()) {

                    DocumentFile.fromFile(ctx.getDatabasePath(ProspectorDatabase.DATABASE_NAME)).let { dbFile ->

                        ctx.contentResolver.openOutputStream(uri)?.use { outputStream ->

                            ZipUtil.zip(ctx, arrayOf(dbFile, prefFile), outputStream)
                        }
                    }
                }
            }

            tempPref.delete()
        }
    }

    private fun import(uri: Uri) {

        val path = uri.toString()
        val size = path.length
        val extIndex = path.lastIndexOf(".")
        val ext = path.substring(extIndex, size)

        context?.let { ctx ->

            when (ext) {
                ".zip" -> {
                    importZipFile(ctx, uri)
                }
                ".db" -> {
                    importDatabaseFile(ctx, uri)
                }
            }
        }
    }

    /**
     * Room database uses WAL (Write ahead log) and SHM (Shared memory) files that need to be
     * unzipped to the same directoy as the database.
     */
    private fun importZipFile(ctx: Context, uri: Uri) {
        ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
            DocumentFile.fromFile(ctx.getDatabasePath(ProspectorDatabase.DATABASE_NAME)).let { dbFile ->
                ctx.contentResolver.openOutputStream(dbFile.uri)?.use { dbStream ->
                    ZipUtil.unzip(ctx, inputStream, dbStream)
                }
            }
        }
    }

    private fun importDatabaseFile(ctx: Context, uri: Uri) {
        ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
            ctx.contentResolver.openOutputStream(DocumentFile
                .fromFile(ctx.getDatabasePath(ProspectorDatabase.DATABASE_NAME))
                .uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun importUserChoice() {

        importLauncher.launch("*/*")

    }

    /**
     * Is used when the document file directory is setup. This will copy the internal database
     * file to the Exports folder. as prospector_bundle_export_(CURRENT_TIME).zip
     */
    private fun exportDocument(ctx: Context) {

        BaseDocumentTreeUtil.getDirectory(ctx, R.string.dir_database)?.let { dir ->

            if (dir.exists()) {

                val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
                val time = DateUtil().getTime()
                val fileName = "prospector_bundle_export_$time.zip"
                val dbFileName = ProspectorDatabase.DATABASE_NAME
                val prefFileName = "preferences_$time.xml"

                dir.createFile("*/*", fileName)?.let { export ->

                    dir.createFile("*/*", dbFileName)?.let { databaseExport ->

                        dir.createFile("*/*", prefFileName)?.let { prefExport ->

                            if (databaseExport.exists() && prefExport.exists() && export.exists()) {

                                    DocumentTreeUtil.getFileOutputStream(ctx, R.string.dir_database, prefFileName)?.use { output ->

                                        ObjectOutputStream(output).use { oos ->

                                            oos.writeObject(prefs.all)
                                        }

                                        output.close()
                                    }

                                    DocumentFile.fromFile(ctx.getDatabasePath(ProspectorDatabase.DATABASE_NAME)).let { dbDoc ->

                                        DocumentTreeUtil.copy(ctx, dbDoc, databaseExport)

                                        DocumentTreeUtil.getFileOutputStream(ctx, R.string.dir_database, fileName)?.use { output ->

                                            ZipUtil.zip(ctx, arrayOf(databaseExport, prefExport), output)

                                        }
                                    }

                                    databaseExport.delete()
                                    prefExport.delete()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportUserChoice() {

        val time = DateUtil().getTime()
        val fileName = "prospector_database_export_$time.zip"

        exportLauncher.launch(fileName)
    }
}
