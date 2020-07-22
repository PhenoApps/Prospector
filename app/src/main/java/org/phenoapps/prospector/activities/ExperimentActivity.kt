package org.phenoapps.prospector.activities

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentScanViewModelFactory
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.utils.DateUtil
import java.io.File

class ExperimentActivity : AppCompatActivity() {

    private lateinit var sViewModel: ExperimentScansViewModel

    private lateinit var mBinding: ActivityMainBinding

    private val permissionCheck by lazy {

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            setupActivity()

        }
    }

    private fun writeStream(file: File, resourceId: Int) {

        if (!file.isFile) {

            val stream = resources.openRawResource(resourceId)

            file.writeBytes(stream.readBytes())

            stream.close()
        }

    }

    private fun setupDirs() {

        //create separate subdirectory foreach type of import
        val scans = File(this.externalCacheDir, "Scans")

        scans.mkdir()

        //create empty files for the examples
        val example = File(scans, "/scans_example.csv")

        //blocking code can be run with Dispatchers.IO
        CoroutineScope(Dispatchers.IO).launch {

            writeStream(example, R.raw.scans_example)

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        permissionCheck.launch(arrayOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA))

    }

    private fun setupActivity() {

        val viewModel: ExperimentScansViewModel by viewModels {

            ExperimentScanViewModelFactory(
                    ExperimentScansRepository.getInstance(
                            ProspectorDatabase.getInstance(this)
                                    .expScanDao()))

        }

        sViewModel = viewModel

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupDirs()

        setupRecyclerView()

        setupButtons()

        startObservers()

    }

    private fun setupButtons() {

        mBinding.onClick = View.OnClickListener {

            val value = mBinding.experimentIdEditText.text

            if (value.isNotBlank()) {

                sViewModel.insertExperiment(Experiment(value.toString()).apply {
                    this.date = DateUtil().getTime()
                })

                mBinding.experimentIdEditText.text.clear()

                Snackbar.make(mBinding.root,
                        "New Experiment $value added.", Snackbar.LENGTH_SHORT).show()

            } else {

                Snackbar.make(mBinding.root,
                        "You must enter an experiment name.", Snackbar.LENGTH_LONG).show()
            }
        }

    }

    private fun setupRecyclerView() {

        mBinding.recyclerView.layoutManager = LinearLayoutManager(this)

        mBinding.recyclerView.adapter = ExperimentAdapter(this)

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                return false

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val id = viewHolder.itemView.tag as Long

                sViewModel.deleteExperiment(id)

            }

        }).attachToRecyclerView(mBinding.recyclerView)
    }

    private fun startObservers() {

        sViewModel.experiments.observe(this) {

            (mBinding.recyclerView.adapter as ExperimentAdapter)
                    .submitList(it)

            Handler().postDelayed({

                mBinding.recyclerView.scrollToPosition(0)

            }, 250)

        }
    }
}