package org.phenoapps.prospector.activities

import BULB_FRAMES
import CONVERT_TO_WAVELENGTHS
import EXPORT_TYPE
import FIRST_CONNECT_ERROR_ON_LOAD
import LED_FRAMES
import OPERATOR
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import kotlinx.coroutines.*
import org.phenoapps.prospector.BuildConfig
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.fragments.ExperimentListFragmentDirections
import org.phenoapps.prospector.utils.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }

    private val sViewModel: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(this)
                                .expScanDao()))

    }

    private val sDeviceViewModel: DeviceViewModel by viewModels()

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mDrawerLayout: DrawerLayout

    private lateinit var mDrawerToggle: ActionBarDrawerToggle

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private val permissionCheck by lazy {


        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            setupActivity()

        }
    }

    private fun withData(function: CoroutineScope.(List<Experiment>, List<Sample>, List<Scan>, List<SpectralFrame>) -> Unit) {

        sViewModel.experiments.observe(this@MainActivity, Observer {

            it?.let { expList ->

                sViewModel.samples.observe(this@MainActivity, Observer {

                    it?.let { samplesList ->

                        sViewModel.scans.observe(this@MainActivity, Observer {

                            it?.let { scanList ->

                                sViewModel.frames.observe(this@MainActivity, Observer {

                                    it?.let { frameList ->

                                        function(expList, samplesList, scanList, frameList)

                                    }
                                })
                            }
                        })
                    }
                })
            }
        })
    }

    /**
     * Function that starts export depending on the preferences.
     * Asks a dialog for export type, or immediately creates json/csv.
     */
    private fun askExport() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val askType = getString(R.string.export_type_always_ask)
        val jsonType = getString(R.string.export_type_brapi)
        val csvType = getString(R.string.export_type_csv)

        when (val exportType = prefs.getString(EXPORT_TYPE, askType) ?: askType) {

            askType -> {

                Dialogs.askForExportType(AlertDialog.Builder(this@MainActivity),
                        getString(R.string.ask_export_type_title),
                        arrayOf(csvType, jsonType)) { type ->

                    exportFile(type)

                }

            } else -> exportFile(exportType)
        }
    }

    /**
     * Uses activity results contracts to create a document and call the export function
     */
    private fun exportFile(exportType: String) {

        val jsonType = getString(R.string.export_type_brapi)

        val csvType = getString(R.string.export_type_csv)

        val defaultFileNamePrefix = getString(R.string.default_csv_export_file_name)

        val ext = when(exportType) {
            csvType -> ".csv"
            else -> ".json"
        }

        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->

            withData { experiments, samples, scans, frames ->

                launch {

                    withContext(Dispatchers.IO) {

                        when (exportType) {

                            jsonType -> {

                                FileUtil(this@MainActivity).exportJson(uri, experiments, samples, scans, frames)

                            }
                            else -> {

                                FileUtil(this@MainActivity).exportCsv(uri, experiments, samples, scans, frames)
                            }
                        }
                    }
                }
            }
        }.launch("${defaultFileNamePrefix}_${DateUtil().getTime()}$ext")
    }

    private suspend fun disconnectDeviceAsync() {

        sDeviceViewModel.disconnect()

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
        launch {

            withContext(Dispatchers.IO) {

                writeStream(example, R.raw.scans_example)
            }
        }
    }

    private fun setupNavController() {

        mNavController = Navigation.findNavController(this@MainActivity, R.id.nav_fragment)

        mNavController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {

                R.id.experiment_list_fragment -> {

                    mDrawerToggle.isDrawerIndicatorEnabled = true

                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                }
                else -> {

                    mDrawerToggle.isDrawerIndicatorEnabled = false

                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

                }
            }
        }

    }

    private fun setupActivity() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val uuid = UUID.randomUUID().toString()

        prefs.edit().putBoolean(uuid, true).apply()

        if (prefs.getBoolean(uuid, false)) {

            Dialogs.largeNotify(AlertDialog.Builder(this@MainActivity),
                    getString(R.string.initial_setup_instructions)
            )
        }

        prefs.edit().putBoolean(FIRST_CONNECT_ERROR_ON_LOAD, true).apply()

        setupDirs()

        mBinding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        mSnackbar = SnackbarQueue()

        setupNavDrawer()

        setupNavController()

        supportActionBar.apply {
            title = ""
            this?.let {
                it.themedContext
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
            }
        }

        //TODO: 3G might need to be disabled for connection to work
        sDeviceViewModel.connection(this@MainActivity).observe(this@MainActivity, Observer {

            it?.let {

                when (it) {

                    is String -> {

                        if (prefs.getBoolean(FIRST_CONNECT_ERROR_ON_LOAD, true)) {

                            mSnackbar.push(SnackbarQueue.SnackJob(
                                    mBinding.root,
                                    getString(R.string.connection_error),
                                    getString(R.string.settings)) {

                                mNavController.navigate(ExperimentListFragmentDirections.actionToSettings())
                            })

                            prefs.edit().putBoolean(FIRST_CONNECT_ERROR_ON_LOAD, false).apply()

                        } else {

                        }

                    }

                    is LinkSquareAPI.LSDeviceInfo -> {

                        mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.connected)))

                    }

                    else -> {

                        mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.connecting)))


                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if ("release" in BuildConfig.FLAVOR) {

            //TODO add firebase analytics event on error

            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

                Log.e("ProspectorCrash", throwable.message)

                throwable.printStackTrace()

            }
        }

        setupActivity()

        if ("demo" in BuildConfig.FLAVOR) {

            launch {

                loadDeveloperData()

            }
        }
    }

    private suspend fun loadDeveloperData() = withContext(Dispatchers.IO) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        val eid = sViewModel.insertExperiment(Experiment("Developer Experiment Data")).await()

        val numbers = (0..9)

        repeat(2) {

            val uuid = UUID.randomUUID().toString()

            sViewModel.insertSample(Sample(eid, uuid).apply {
                this.note = "Developer test note ${UUID.randomUUID().toString()}"
            }).await()

            repeat(2) {

                val sid = sViewModel.insertScan(Scan(eid, uuid).also {
                    it.deviceId="1-2-3-4"
                    it.lightSource=0
                    it.operator="Developer"
                }).await()

                val pixelValues = (1..600).map { numbers.random().toDouble() }.joinToString(" ")

                val convert = prefs.getBoolean(CONVERT_TO_WAVELENGTHS, false)

                if (!convert) {

                    sViewModel.insertFrame(sid, SpectralFrame(sid, 0, pixelValues, 0))

                } else {

                    sViewModel.insertFrame(sid, SpectralFrame(sid, 0, pixelValues, 0).toWaveArray())

                }

                //delay(1000)

            }
        }
    }

    private fun setupNavDrawer() {

        mDrawerLayout = mBinding.drawerLayout

        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

                super.onDrawerSlide(drawerView, slideOffset)

                closeKeyboard()
            }
        }

        mDrawerToggle.isDrawerIndicatorEnabled = true

        mDrawerLayout.addDrawerListener(mDrawerToggle)

        // Setup drawer view
        val nvDrawer = findViewById<NavigationView>(R.id.nvView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        nvDrawer.getHeaderView(0).apply {
            findViewById<TextView>(R.id.navHeaderText)
                    .text = prefs.getString(OPERATOR, "")
        }

        nvDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.action_nav_export -> {

                    askExport()

                }
                R.id.action_nav_settings -> {

                    mNavController.navigate(ExperimentListFragmentDirections.actionToSettings())
                }
                R.id.action_nav_about -> {

                    //mNavController.navigate(ExperimentListFragmentDirections.actionToAbout())

                }
            }

            mDrawerLayout.closeDrawers()

            true
        }


    }

    private fun closeKeyboard() {

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onDestroy() {

        launch {

            disconnectDeviceAsync()

        }

        coroutineContext.cancel()

        super.onDestroy()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val dl = findViewById<DrawerLayout>(R.id.drawer_layout)

        closeKeyboard()

        if (mDrawerToggle.onOptionsItemSelected(item)) {

            return true
        }

        when (item.itemId) {

            android.R.id.home -> {

                mNavController.currentDestination?.let {

                    when (it.id) {

                        R.id.experiment_list_fragment -> {

                            dl.openDrawer(GravityCompat.START)

                        }

                        /**
                         * Disable back button if the number of selected frames is less than 1.
                         */
                        R.id.settings_fragment -> {

                            onSettingsBackPressed()

                        }

                        //go back to the last fragment instead of opening the navigation drawer
                        else -> mNavController.popBackStack()
                    }
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (::mDrawerToggle.isInitialized) {

            mDrawerToggle.syncState()

        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    /**
     * Uses nav controller to change what the back press does depending on the fragment id.
     */
    override fun onBackPressed() {

        mNavController.currentDestination?.let { it ->

            when (it.id) {

                //go back to the last fragment instead of opening the navigation drawer
                R.id.experiment_list_fragment -> {

                    if (doubleBackToExitPressedOnce) {

                        super.onBackPressed();

                        return
                    }

                    this.doubleBackToExitPressedOnce = true;

                    Toast.makeText(this, getString(R.string.double_back_press), Toast.LENGTH_SHORT).show();

                    Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                }
                R.id.settings_fragment -> {

                    onSettingsBackPressed()

                }
                else -> super.onBackPressed()
            }
        }
    }

    /**
     * Function that is called when the nav toggle or back button is pressed on the Settings fragment
     */
    private fun onSettingsBackPressed() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        val ledFrames = prefs.getInt(LED_FRAMES, 0)

        val bulbFrames = prefs.getInt(BULB_FRAMES, 0)

        if (ledFrames + bulbFrames < 1) {

            mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root,
                    getString(R.string.settings_error_num_frames_must_exceed_zero)))

        } else mNavController.popBackStack()
    }
}
