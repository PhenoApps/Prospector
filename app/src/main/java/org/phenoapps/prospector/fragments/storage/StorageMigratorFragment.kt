package org.phenoapps.prospector.fragments.storage

import android.graphics.Color
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import org.phenoapps.fragments.storage.PhenoLibMigratorFragment
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.DefineStorageActivity

class StorageMigratorFragment: PhenoLibMigratorFragment() {

    override val migrateButtonColor = Color.parseColor("#FF5722")
    override val skipButtonColor = Color.parseColor("#FF5722")
    override val backgroundColor = Color.parseColor("#03A9F4")

    override fun migrateStorage(from: DocumentFile, to: DocumentFile) {
        (activity as? DefineStorageActivity)?.enableBackButton(false)
        super.migrateStorage(from, to)
        (activity as? DefineStorageActivity)?.enableBackButton(true)
    }

    override fun navigateEnd() {
        activity?.runOnUiThread {

            Toast.makeText(context, R.string.frag_migrator_status_complete,
                Toast.LENGTH_SHORT).show()

            activity?.finish()
        }
    }
}