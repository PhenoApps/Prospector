package org.phenoapps.prospector.activities

import BULB_FRAMES
import DEVICE_TYPE_NIR
import FIRST_CONNECT_ERROR_ON_LOAD
import LED_FRAMES
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.phenoapps.prospector.BuildConfig
import org.phenoapps.prospector.MainGraphDirections
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.*
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.MainActivityViewModel
import org.phenoapps.prospector.data.viewmodels.factory.MainViewModelFactory
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository
import org.phenoapps.prospector.data.viewmodels.repository.ScanRepository
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.utils.*
import java.io.File
import java.util.*

/**
 * The main activity controls device connection across all fragments.
 * The main fragment is the experiment list.
 *
 * Bottom toolbar navigation is controlled here.
 */
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }
    //flag to track when a device is connected, used to change the options menu icon
    var mConnected: Boolean = false

    private val sViewModel: MainActivityViewModel by viewModels {

        with(ProspectorDatabase.getInstance(this)) {
            MainViewModelFactory(
                    ExperimentRepository.getInstance(this.experimentDao()),
                    SampleRepository.getInstance(this.sampleDao()),
                    ScanRepository.getInstance(this.scanDao()))
        }
    }

    /**
     * This activity view model is used throughout all the fragments to update connection status.
     */
    val sDeviceViewModel: DeviceViewModel by viewModels()

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private fun setupDirs() {

        //create separate subdirectory foreach type of import
        val scans = File(this.externalCacheDir, "Scans")

        scans.mkdir()
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

        setupBotNav()

        setupNavController()

        //on first load ask user if they want to load sample data
        if (prefs.getBoolean("FIRST_LOAD", true)) {

            prefs.edit().putBoolean("FIRST_LOAD", true).apply()

            Dialogs.onOk(AlertDialog.Builder(this),
                    getString(R.string.activity_main_sample_data_title),
                    getString(R.string.cancel),
                    getString(R.string.ok)) {


                if (it) {
                    launch {
                        loadSampleData()
                    }
                }
            }
        }

        sDeviceViewModel.isConnectedLive().observeForever {

            it?.let { status ->

                mConnected = status

                mSnackbar.push(SnackbarQueue
                        .SnackJob(mBinding.root,
                                if (status) getString(R.string.connected)
                                else getString(R.string.disconnect)))
            }
        }

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

    }

    private suspend fun loadSampleData() = withContext(Dispatchers.IO) {

        launch(Dispatchers.IO) {

            //make sample experiment
            val eid = sViewModel.insertExperimentAsync(
                    Experiment("SampleDataFruit", DEVICE_TYPE_NIR, "sample data")).await()

            //make banana peel and orange peel samples
            sViewModel.insertSampleAsync(
                    Sample(eid, "orange peel", note = "sample data")).await()

            sViewModel.insertSampleAsync(
                    Sample(eid, "banana peel", note = "sample data")).await()

            //open the sample assets file
            assets.open("banana_and_orange.csv").reader().readLines().forEachIndexed { index, line ->

                //skip the header, otherwise insert the rows
                if (index > 0) {

                    val tokens = line.split(",")

                    try {

                        val experimentName = tokens[0]
                        val sampleName = tokens[1]
                        val scanDate = tokens[2]
                        val deviceType = tokens[3]
                        val deviceId = tokens[4]
                        val operator = tokens[5]
                        val lightSource = tokens[6]

                        //for each scan insert using coroutines
                        //get the tokens between the lightsource and experiment note headers
                        val waveRange = tokens.subList(6, tokens.size-1).joinToString(" ")

                        val sid = sViewModel.insertScanAsync(Scan(eid, sampleName, scanDate, deviceType,
                                deviceId = deviceId,
                                operator = operator,
                                lightSource = lightSource.toInt())).await()

                        sViewModel.insertFrame(sid, SpectralFrame(sid, 0, waveRange, 0))

                    } catch (e: Exception) {

                        e.printStackTrace()

                    }
                }
            }
        }

    }

    private fun setupBotNav() {

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
            }

            true
        }
    }

    fun startDeviceConnection() {

        launch {

            sDeviceViewModel.connect(this@MainActivity.applicationContext)

        }
    }

    private fun stopDeviceConnection() {

        launch {

            sDeviceViewModel.disconnect()

        }
    }

    override fun onDestroy() {

        stopDeviceConnection()

        super.onDestroy()

    }

    override fun onPause() {

        sDeviceViewModel.reset()

        super.onPause()
    }

    override fun onResume() {

        startDeviceConnection()

        super.onResume()
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
