package org.phenoapps.prospector.activities

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.phenoapps.intercross.util.Dialogs
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ScansAdapter
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentScanViewModelFactory
import org.phenoapps.prospector.databinding.ScanLayoutBinding
import org.phenoapps.prospector.utils.FileUtil

/*
The scan activity should import, export, and scan new samples into an experiment
Experiment id's are passed as arguments to this activity. Argument named: "experiment" type: int
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var sViewModel: ExperimentScansViewModel

    private lateinit var mBinding: ScanLayoutBinding

    private var mExpId: Long = -1L

    private val importScans by lazy {

        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            val scans = FileUtil(this).parseInputFile(mExpId, uri)

            scans.keys.forEach { key ->

                CoroutineScope(Dispatchers.IO).launch {

                    sViewModel.insertScan(key)

                    scans[key]?.forEach { frame ->

                        sViewModel.insertFrame(key.sid, frame)

                    }

                }

            }
        }
    }

    private val sOnClickDelete = View.OnClickListener {

        if (::sViewModel.isInitialized && mExpId != -1L) {

            sViewModel.deleteExperiment(mExpId)

            setResult(Activity.RESULT_OK)

            finish()

        }

    }

    private val sOnClickScan = View.OnClickListener {

        Dialogs.askForName(this, R.layout.dialog_layout_create_name, R.string.ask_new_scan_name, R.string.ok) {

            //TODO implement Link Square scan

            //TODO add result scan to DB

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val viewModel: ExperimentScansViewModel by viewModels {

            ExperimentScanViewModelFactory(
                    ExperimentScansRepository.getInstance(
                            ProspectorDatabase.getInstance(this).expScanDao()
                    )
            )
        }

        sViewModel = viewModel

        mBinding = DataBindingUtil.setContentView(this, R.layout.scan_layout)

        with (mBinding) {

            deleteOnClick = sOnClickDelete

            scanOnClick = sOnClickScan

        }

        // INIT TOOLBAR
        if (supportActionBar != null) {
            supportActionBar!!.setTitle(R.string.app_name)
            supportActionBar!!.themedContext
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        //check if experiment id is included in the arguments.
        val eid = intent.getLongExtra("experiment", -1L)

        if (eid != -1L) {

            mExpId = eid

            mBinding.recyclerView.adapter = ScansAdapter(this, this, sViewModel)

            startObservers()

        }

    }

    private fun startObservers() {

        sViewModel.getScans(mExpId).observe(this, Observer { data ->

            if (data.isNotEmpty()) {

                val exp = data.first()

                mBinding.expDate = exp.expDate

                mBinding.expName = exp.expName

                (mBinding.recyclerView.adapter as ScansAdapter).submitList(data.map {

                    it.sid?.let { sid ->

                        Scan(exp.eid, sid)
                                .apply {
                                    this.deviceId = it.deviceId
                                    this.date = it.scanDate
                                }

                    }
                })
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        MenuInflater(this@ScanActivity).inflate(R.menu.selection_main, menu)

        super.onCreateOptionsMenu(menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.import_scans -> importScans.launch("*/*")

            R.id.export_scans -> null//TODO

        }

        return super.onOptionsItemSelected(item)
    }
}