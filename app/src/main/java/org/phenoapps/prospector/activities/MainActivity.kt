package org.phenoapps.prospector.activities

import BULB_FRAMES
import CONVERT_TO_WAVELENGTHS
import FIRST_CONNECT_ERROR_ON_LOAD
import LED_FRAMES
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.phenoapps.prospector.BuildConfig
import org.phenoapps.prospector.MainGraphDirections
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.*
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.utils.DateUtil
import org.phenoapps.prospector.utils.FileUtil
import org.phenoapps.prospector.utils.SnackbarQueue
import org.phenoapps.prospector.utils.observeOnce
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }

    //flag to track when a device is connected, used to change the options menu icon
    private var mConnected: Boolean = false

    private val sViewModel: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(this)
                                .expScanDao()))

    }

    private val sDeviceViewModel: DeviceViewModel by viewModels()

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private fun withData(function: CoroutineScope.(List<DeviceTypeExport>) -> Unit) {

        sViewModel.deviceTypeExports.observeOnce(this@MainActivity, Observer {

            it?.let { exports ->

                function(exports)

            }
        })
    }

    /**
     * Uses activity results contracts to create a document and call the export function
     */
    private fun exportFile(exportType: String) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        val convert = prefs.getBoolean(CONVERT_TO_WAVELENGTHS, false)

        val defaultFileNamePrefix = getString(R.string.default_csv_export_file_name)

        val ext = when(exportType) {
            "CSV" -> ".csv"
            else -> ".json"
        }

        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.CreateDocument()) { it?.let { uri ->

            withData { exports ->

                launch {

                    withContext(Dispatchers.IO) {

                        val start = System.nanoTime()

                        FileUtil(this@MainActivity).exportCsv(uri, exports, convert)

                        Log.d("ExportTime", (1e-9 * (System.nanoTime() - start)).toString())
                    }
                }
            }
        }}.launch("Output_${DateUtil().getTime()}")
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

    }

    private fun setupActivity() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        prefs.edit().putBoolean(FIRST_CONNECT_ERROR_ON_LOAD, true).apply()

        setupDirs()

        mBinding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        mSnackbar = SnackbarQueue()

        setupNavDrawer()

        setupNavController()

        supportActionBar.apply {
            title = ""
//            this?.let {
//                it.themedContext
//                setDisplayHomeAsUpEnabled(true)
//                setHomeButtonEnabled(true)
//            }
        }

        sDeviceViewModel.isConnectedLive().observeForever {

            it?.let { status ->

                mConnected = status

                mSnackbar.push(SnackbarQueue
                        .SnackJob(mBinding.root,
                                if (status) getString(R.string.connected)
                                else getString(R.string.disconnect)))

                invalidateOptionsMenu()
            }
        }
    }

    override fun onPause() {

        sDeviceViewModel.reset(this.applicationContext)

        super.onPause()
    }

    override fun onResume() {

        startDeviceConnection()

        super.onResume()
    }

    private fun startDeviceConnection() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        launch {

            sDeviceViewModel.connect(this@MainActivity.applicationContext)

//            //TODO: 3G might need to be disabled for connection to work
//            sDeviceViewModel.connection(this@MainActivity.applicationContext).observeOnce(this@MainActivity, {
//
//                it?.let {
//
//                    when (it) {
//
//                        is String -> {
//
//                            if (prefs.getBoolean(FIRST_CONNECT_ERROR_ON_LOAD, true)) {
//
//                                mSnackbar.push(SnackbarQueue.SnackJob(
//                                        mBinding.root,
//                                        getString(R.string.connection_error),
//                                        getString(R.string.settings)) {
//
//                                    mNavController.navigate(ExperimentListFragmentDirections.actionToSettings())
//                                })
//
//                                prefs.edit().putBoolean(FIRST_CONNECT_ERROR_ON_LOAD, false).apply()
//
//                            } else {
//
//                            }
//
//                        }
//
//                        is LinkSquareAPI.LSDeviceInfo -> {
//
////                        mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.connected)))
//
//                        }
//
//                        else -> {
//
////                        mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.connecting)))
//
//                        }
//                    }
//                }
//            })
        }

//
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        if (mConnected) {

            menuInflater.inflate(R.menu.menu_top_bar_connected, menu)

        } else {

            menuInflater.inflate(R.menu.menu_top_bar, menu)

        }

        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {

            R.id.action_connected -> {

                //closes and reinitializes the device api
                sDeviceViewModel.reset(this)

            }

            R.id.action_disconnected -> {

                mNavController.navigate(MainGraphDirections
                        .actionToConnectInstructions())

                startDeviceConnection()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

//        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
//                .detectAll()
//                .penaltyLog()
//                .penaltyDeath()
//                .build())

        if ("release" in BuildConfig.FLAVOR) {

            //TODO add firebase analytics event on error

            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

                Log.e("ProspectorCrash", throwable.message)

                throwable.printStackTrace()

            }
        }

        setupActivity()

//        if ("demo" in BuildConfig.FLAVOR) {
//
//            launch {
//
////                loadDeveloperData()
//
//            }
//        }
    }

    private suspend fun loadDeveloperData() = withContext(Dispatchers.IO) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        val eid = sViewModel.insertExperiment(Experiment("Developer Experiment Data")).await()

        val numbers = (0..9)

        val devices = arrayOf("LinkSquare", "LinkSquareNIR")

        val samples = arrayOf("toast", "mango", "grape", "popcorn")

        val lightSources = arrayOf(0, 1)

        val start = System.nanoTime()

        repeat(1) {

            repeat(100) {

                val uuid = samples.random()

                sViewModel.insertSample(Sample(eid, uuid, DateUtil().getTime(), "Developer test note ${UUID.randomUUID().toString()}")).await()

                val sid = sViewModel.insertScan(Scan(eid, uuid, DateUtil().getTime(),
                        deviceId = "1-2-3-4",
                        deviceType = devices.random(),
                        lightSource = lightSources.random(),
                        operator = "Developer")).await()

//                val sid2 = sViewModel.insertScan(Scan(eid, uuid).also {
//                    it.deviceId="1-2-3-4"
//                    it.lightSource=0
//                    it.operator="Developer"
//                }).await()

                //val convert = prefs.getBoolean(CONVERT_TO_WAVELENGTHS, false)

                val pixelValues = (1..600).map { numbers.random().toDouble() }.joinToString(" ")

                sViewModel.insertFrame(sid, SpectralFrame(sid, 0, pixelValues, 0))

//                val waves = SpectralFrame(sid2, 0, pixelValues, 0).toWaveArray()
//
//                sViewModel.insertFrame(sid2, waves)

//                if (!convert) {
//
//
//                } else {
//
//
//                }

                //delay(1000)

            }
        }

        Log.d("Time", (1e-9 * (System.nanoTime() - start)).toString())

    }

    private fun setupNavDrawer() {

        val botNavView = mBinding.bottomNavView

        botNavView.inflateMenu(R.menu.menu_bot_nav)

        botNavView.setOnNavigationItemSelectedListener { menuItem ->

            when (menuItem.itemId) {

                R.id.action_nav_data -> {

                    mNavController.navigate(MainGraphDirections
                            .actionToExperiments())

                }
                R.id.action_nav_settings -> {

                    mNavController.navigate(MainGraphDirections
                            .actionToSettings())
                }
                R.id.action_nav_about -> {

                    mNavController.navigate(MainGraphDirections
                            .actionToAboutFragment())

                }
                R.id.action_nav_export -> {

                    exportFile("csv")
                }
            }

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

        super.onDestroy()

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

                        super.onBackPressed()

                        return
                    }

                    this.doubleBackToExitPressedOnce = true

                    Toast.makeText(this, getString(R.string.double_back_press), Toast.LENGTH_SHORT).show()

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
