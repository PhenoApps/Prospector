package org.phenoapps.prospector.activities

import BULB_FRAMES
import DEVICE_TYPE_NIR
import FIRST_CONNECT_ERROR_ON_LOAD
import LED_FRAMES
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.phenoapps.prospector.BuildConfig
import org.phenoapps.prospector.NavigationRootDirections
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.*
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.MainActivityViewModel
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
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }
    //flag to track when a device is connected, used to change the options menu icon
    var mConnected: Boolean = false

    private val sViewModel: MainActivityViewModel by viewModels()

    /**
     * This activity view model is used throughout all the fragments to update connection status.
     */
    val sDeviceViewModel: DeviceViewModel by viewModels()

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private var mCitationDialog: AlertDialog? = null
    private var mAskForOperatorDialog: AlertDialog? = null
    private var mAskChangeOperatorDialog: AlertDialog? = null

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val mKeyUtil by lazy {
        KeyUtil(this)
    }

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

        mSnackbar = SnackbarQueue()

        setupBotNav()

        //on first load ask user if they want to load sample data
        if (prefs.getBoolean("FIRST_LOAD", true)) {

            prefs.edit().putBoolean("FIRST_LOAD", true).apply()

            Dialogs.onOk(AlertDialog.Builder(this),
                    getString(R.string.activity_main_sample_data_title),
                    getString(R.string.cancel),
                    getString(R.string.ok)) {


                if (it) {

                    startLoadSampleData()

                }
            }
        }

        startConnectionWatcher()
    }

    private fun startConnectionWatcher() {

        val check = object : TimerTask() {

            override fun run() {

                runOnUiThread {

                    val last = mConnected

                    mConnected = sDeviceViewModel.isConnected()

                    if (last != mConnected) {
                        mSnackbar.push(
                            SnackbarQueue
                                .SnackJob(
                                    mBinding.actMainCoordinatorLayout,
                                    if (mConnected) getString(R.string.connected)
                                    else getString(R.string.disconnect)
                                )
                        )
                    }
                }
            }
        }

        Timer().cancel()

        Timer().purge()

        Timer().scheduleAtFixedRate(check, 0, 1500)
    }

    /**
     * Displays a snack bar message.
     */
    fun notify(message: String) {

        mSnackbar.push(
            SnackbarQueue
                .SnackJob(
                    mBinding.actMainCoordinatorLayout,
                    message
                )
        )
    }

    private fun startLoadSampleData() {

        lifecycleScope.launch {

            loadSampleData()

            mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.samples_loaded)))

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if ("release" in BuildConfig.FLAVOR) {

            //TODO add firebase analytics event on error

            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->

                Log.e("ProspectorCrash", throwable.message ?: "Unknown Error")

                throwable.printStackTrace()

            }
        }

        mBinding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        setupNavController()

        //check cold load, load sample data and navigate to intro activity
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (prefs.getBoolean("FIRST_LOAD", true)) {

            prefs.edit().putBoolean("FIRST_LOAD", false).apply()

            startLoadSampleData()

            startActivity(Intent(this, IntroActivity::class.java))

        }

        setupActivity()

        mCitationDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_citation_title)
            .setMessage(R.string.dialog_citation_message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        mAskForOperatorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_ask_input_operator_title)
            .setMessage(R.string.dialog_ask_input_operator_message)
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->

                dialog.dismiss()

                mNavController.navigate(R.id.action_to_settings,
                    bundleOf(mKeyUtil.argOpenOperatorSettings to true))

            }.create()

        mAskChangeOperatorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_ask_change_operator_title)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->

                dialog.dismiss()

                mNavController.navigate(R.id.action_to_settings,
                    bundleOf(mKeyUtil.argOpenOperatorSettings to true))
            }
            .setNeutralButton(R.string.dialog_dont_ask_again) { dialog, _ ->

                mPrefs.edit().putBoolean(mKeyUtil.verifyOperator, false).apply()

                dialog.dismiss()

            }
            .setNegativeButton(R.string.no) { dialog, _ ->

                dialog.dismiss()

            }.create()
    }

    private fun showAskOperatorDialog() {
        runOnUiThread {
            if (mAskForOperatorDialog != null && mAskForOperatorDialog?.isShowing != true) {
                mAskForOperatorDialog?.show()
            }
        }
    }

    private fun showAskChangeOperatorDialog() {
        runOnUiThread {
            if (mAskChangeOperatorDialog != null && mAskChangeOperatorDialog?.isShowing != true) {
                mAskChangeOperatorDialog?.show()
            }
        }
    }

    fun showCitationDialog() {
        runOnUiThread {
            if (mCitationDialog != null && mCitationDialog?.isShowing != true) {
                mCitationDialog?.show()
            }
        }
    }

    fun setToolbar(id: Int) {

        mBinding.bottomNavView.menu.findItem(id).isEnabled = false

        mBinding.bottomNavView.selectedItemId = id

        mBinding.bottomNavView.menu.findItem(id).isEnabled = true
    }

    private suspend fun loadSampleData() {

        //make sample experiment
        val eid = sViewModel.insertExperimentAsync(
                Experiment("Sample Experiment", DEVICE_TYPE_NIR, "Example data loaded on first install.")).await()

        val samples = listOf("Light Red Kidney Beans",
                "Navy Beans",
                "Pinto Beans",
                "Dark Red Kidney Beans",
                "Red Beans",
                "Great Northern Beans",
                "White Kidney Beans",
                "Black Beans",
                "Sorghum",
                "Corn",
                "Soybean",
                "Wheat"
        )

        for (s in samples) {

            sViewModel.insertSampleAsync(
                Sample(eid, s, note = "sample data")).await()

        }

        withContext(Dispatchers.IO) {

            //open the sample assets file
            assets.open("examples.csv").reader().readLines().forEachIndexed { index, line ->

                //skip the header, otherwise insert the rows
                if (index > 0) {

                    val tokens = line.split(",")

                    try {

                        //val experimentName = tokens[0]
                        val sampleName = tokens[1]
                        val scanDate = tokens[2]
                        val deviceType = tokens[3]
                        val deviceId = tokens[4]
                        val operator = tokens[5]
                        val lightSource = tokens[6]

                        //for each scan insert using coroutines
                        //get the tokens between the lightsource and experiment note headers
                        val waveRange = tokens.subList(6, tokens.size - 1).joinToString(" ")

                        val sid = sViewModel.insertScanAsync(
                            Scan(
                                eid, sampleName, scanDate, deviceType,
                                deviceId = deviceId,
                                operator = operator,
                                lightSource = lightSource.toInt()
                            )
                        ).await()

                        sViewModel.insertFrame(sid, SpectralFrame(sid, 0, waveRange, lightSource.toInt()))

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

                    mNavController.navigate(NavigationRootDirections
                        .actionToExperiments())

                }
                R.id.action_nav_settings -> {

                    mNavController.navigate(NavigationRootDirections
                        .actionToSettings())

                }
                R.id.action_nav_about -> {

                    mNavController.navigate(NavigationRootDirections
                        .actionToAboutFragment())

                }
            }

            true
        }
    }

    fun startDeviceConnection() {

        lifecycleScope.launch {

            sDeviceViewModel.connect(this@MainActivity.applicationContext)

        }
    }

    private fun stopDeviceConnection() {

        lifecycleScope.launch {

            sDeviceViewModel.disconnect()

        }
    }

    override fun onDestroy() {

        stopDeviceConnection()

        mCitationDialog?.dismiss()
        mAskChangeOperatorDialog?.dismiss()
        mAskForOperatorDialog?.dismiss()

        super.onDestroy()

    }

    override fun onPause() {

        sDeviceViewModel.reset()

        super.onPause()
    }

    override fun onResume() {

        startDeviceConnection()

        checkLastOpened()

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

                    Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
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

    /**
     * Simple function that checks if the activity was opened >24hrs ago.
     * If the condition is met, it asks the user to reenter the operator id.
     */
    private fun checkLastOpened() {

        if (mPrefs.getBoolean(mKeyUtil.verifyOperator, true)) {

            val lastOpen: Long = mPrefs.getLong(mKeyUtil.lastTimeAppOpened, 0L)

            val systemTime = System.nanoTime()

            val nanosInOneDay = 1e9.toLong() * 3600 * 24

            if (lastOpen == 0L || systemTime - lastOpen > nanosInOneDay) {

                if ((mPrefs.getString(mKeyUtil.operator, "") ?: "").isBlank()) {

                    showAskOperatorDialog()

                } else {

                    showAskChangeOperatorDialog()
                }
            }

            updateLastOpenedTime()
        }

    }

    private fun updateLastOpenedTime() {
        mPrefs.edit().putLong(mKeyUtil.lastTimeAppOpened, System.nanoTime()).apply()
    }
}
