package org.phenoapps.prospector.fragments.storage

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.R
import org.phenoapps.prospector.fragments.ExperimentListFragment
import org.phenoapps.prospector.utils.DocumentTreeUtil.Companion.createFolderStructure
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class StorageDefinerFragment: Fragment(R.layout.fragment_storage_definer) {

    companion object {

        const val REQUEST_STORAGE_MIGRATOR = "org.phenoapps.prospector.requests.storage_migrator"

    }

    private val mPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        result?.let { permissions ->
            if (!permissions.containsValue(false)) {
                //input is an optional uri that would define the folder to start from
                mDocumentTree.launch(null)
            } else {

                setFragmentResult(ExperimentListFragment.REQUEST_STORAGE_DEFINER,
                    bundleOf("result" to false))

                findNavController().popBackStack()
            }
        }
    }

    private val mDocumentTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->

        uri?.let { nonNulluri ->

            context?.let { ctx ->

                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                with (context?.contentResolver) {

                    //add new uri to persistable that the user just picked
                    this?.takePersistableUriPermission(nonNulluri, flags)

                    //release old storage directory from persistable if it exists
                    val oldPermitted = this?.persistedUriPermissions
                    if (oldPermitted != null && oldPermitted.isNotEmpty()) {
                        this?.persistedUriPermissions?.forEach {
                            if (it.uri != nonNulluri) {
                                releasePersistableUriPermission(it.uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }
                        }
                    }

                    DocumentFile.fromTreeUri(ctx, nonNulluri)?.let { root ->

                        val executor = Executors.newFixedThreadPool(2)
                        executor.execute {
                            root.createFolderStructure(context)
                        }
                        executor.shutdown()
                        executor.awaitTermination(10000, TimeUnit.MILLISECONDS)

                        setFragmentResult(ExperimentListFragment.REQUEST_STORAGE_DEFINER,
                            bundleOf("result" to true))

                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

                        if (prefs.getBoolean("FIRST_MIGRATE", true)) {

                            prefs.edit().putBoolean("FIRST_MIGRATE", false).apply()

                            findNavController().popBackStack()

                        } else {

                            navigateToMigrator()

                        }
                    }
                }
            }
        }
    }

    private fun navigateToMigrator() {

        setFragmentResultListener(REQUEST_STORAGE_MIGRATOR) { _, _ ->

            findNavController().popBackStack()
        }

        findNavController().navigate(StorageDefinerFragmentDirections
            .actionStorageDefinerToStorageMigrator())

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val defineButton = view.findViewById<Button>(R.id.frag_storage_definer_choose_dir_btn)
        val skipButton = view.findViewById<Button>(R.id.frag_storage_definer_skip_btn)

        skipButton?.setOnClickListener { _ ->

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            if (prefs.getBoolean("FIRST_MIGRATE", true)) {

                prefs.edit().putBoolean("FIRST_MIGRATE", false).apply()

                findNavController().popBackStack()

            } else navigateToMigrator()
        }

        defineButton?.setOnClickListener { _ ->

            launchDefiner()

        }
    }

    private fun launchDefiner() {
        context?.let { ctx ->

            //request runtime permissions for storage
            if (ActivityCompat.checkSelfPermission(ctx,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                //input is an optional uri that would define the folder to start from
                mDocumentTree.launch(null)

            } else {

                mPermissions.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

            }
        }
    }
}