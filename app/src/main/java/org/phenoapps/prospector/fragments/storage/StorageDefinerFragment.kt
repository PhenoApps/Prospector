package org.phenoapps.prospector.fragments.storage

import android.graphics.Color
import android.net.Uri
import org.phenoapps.fragments.storage.PhenoLibStorageDefinerFragment
import org.phenoapps.prospector.activities.DefineStorageActivity

class StorageDefinerFragment: PhenoLibStorageDefinerFragment() {

    override val buttonColor = Color.parseColor("#FF5722")
    override val backgroundColor = Color.parseColor("#FFFFFF")

    override val actionToMigrator = StorageDefinerFragmentDirections
        .actionStorageDefinerToStorageMigrator()

    //default root folder name if user choose an incorrect root on older devices
    override val defaultAppName: String = "prospector"

    override fun onTreeDefined(treeUri: Uri) {
        (activity as? DefineStorageActivity)?.enableBackButton(false)
        super.onTreeDefined(treeUri)
        (activity as? DefineStorageActivity)?.enableBackButton(true)
    }
}