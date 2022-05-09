package org.phenoapps.prospector.activities

import BULB_FRAMES
import DEVICE_TYPE_LS1
import DEVICE_TYPE_NANO
import DEVICE_TYPE_NIR
import FIRST_CONNECT_ERROR_ON_LOAD
import LED_FRAMES
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.phenoapps.prospector.BuildConfig
import org.phenoapps.prospector.NavigationRootDirections
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.*
import org.phenoapps.prospector.data.viewmodels.MainActivityViewModel
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel
import org.phenoapps.prospector.data.viewmodels.devices.LinkSquareViewModel
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.interfaces.Spectrometer
import org.phenoapps.prospector.utils.*
import org.phenoapps.utils.IntentUtil
import java.io.File

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

    companion object {
        const val TAG = "MainActivity"
    }

    //flag to track when a device is connected, used to change the options menu icon
    var mConnected: Boolean = false

    private val mConnectionHandlerThread = HandlerThread("activity connection checker")

    private val sViewModel: MainActivityViewModel by viewModels()

    /**
     * This activity view model is used throughout all the fragments to update connection status.
     */
    var sDeviceViewModel: Spectrometer? = null

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private var mCitationDialog: AlertDialog? = null
    private var mAskForOperatorDialog: AlertDialog? = null
    private var mAskChangeOperatorDialog: AlertDialog? = null
    private var mConfirmFactoryResetDialog: AlertDialog? = null
    private var mFirstDeleteDatabaseDialog: AlertDialog? = null
    private var mSecondDeleteDatabaseDialog: AlertDialog? = null
    private var mAskLocationEnableDialog: AlertDialog? = null

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val mKeyUtil by lazy {
        KeyUtil(this)
    }

    private val startActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        startDeviceConnection()

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

        if (mConnectionHandlerThread.state == Thread.State.NEW) {
            mConnectionHandlerThread.looper
            mConnectionHandlerThread.start()
        }

        val maker = mPrefs.getString(mKeyUtil.deviceMaker, DEVICE_TYPE_LS1)
        sDeviceViewModel = if (maker == DEVICE_TYPE_LS1) {

            LinkSquareViewModel()

        } else {

            InnoSpectraViewModel()
        }

        mBinding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        setupNavController()

        mCitationDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_citation_title)
            .setMessage(getString(R.string.dialog_citation_string) + "\n\n" + getString(R.string.dialog_citation_message))
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

        mFirstDeleteDatabaseDialog = AlertDialog.Builder(this)
            .setTitle(R.string.database_reset)
            .setMessage(getString(R.string.database_reset_warning1))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                mSecondDeleteDatabaseDialog?.show()
            }
            .create()

        mSecondDeleteDatabaseDialog = AlertDialog.Builder(this)
            .setTitle(R.string.database_reset)
            .setMessage(getString(R.string.database_reset_warning2))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        mAskLocationEnableDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_ask_location)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        prefs.edit().putBoolean(FIRST_CONNECT_ERROR_ON_LOAD, true).apply()

        setupDirs()

        mSnackbar = SnackbarQueue()

        setupBotNav()

        runtimeBluetoothCheck()

        startConnectionWatcher()
    }

    private fun askForLocation(success: () -> Unit) {

        mAskLocationEnableDialog?.let { dialog ->

            if (!dialog.isShowing) {

                mAskLocationEnableDialog = AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_ask_location)
                    .setPositiveButton(android.R.string.ok) { d, _ ->
                        success()
                        d.dismiss()
                    }
                    .setNegativeButton(android.R.string.no) { d, _ ->
                        d.dismiss()
                    }
                    .show()
            }
        }
    }

    fun askDeleteDatabase(success: () -> Unit) {

        mFirstDeleteDatabaseDialog?.let { firstDialog ->

            if (!firstDialog.isShowing) {

                mSecondDeleteDatabaseDialog?.let { secondDialog ->

                    if (!secondDialog.isShowing) {

                        mFirstDeleteDatabaseDialog = AlertDialog.Builder(this)
                            .setTitle(R.string.database_reset)
                            .setMessage(getString(R.string.database_reset_warning1))
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                                mSecondDeleteDatabaseDialog?.show()
                            }
                            .create()

                        mSecondDeleteDatabaseDialog = AlertDialog.Builder(this)
                            .setTitle(R.string.database_reset)
                            .setMessage(getString(R.string.database_reset_warning2))
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                success()
                                dialog.dismiss()
                            }
                            .create()

                        mFirstDeleteDatabaseDialog?.show()
                    }
                }
            }
        }
    }

    fun askSampleImport() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)


        //on first load ask user if they want to load sample data
        if (prefs.getBoolean("FIRST_LOAD_SAMPLE_DATA", true)) {

            prefs.edit().putBoolean("FIRST_LOAD_SAMPLE_DATA", false).apply()

            Dialogs.onOk(AlertDialog.Builder(this),
                getString(R.string.activity_main_sample_data_title),
                getString(R.string.cancel),
                getString(R.string.ok)) {


                if (it) {

                    startLoadSampleData()

                }
            }
        }
    }

    private fun startConnectionWatcher() {

        Handler(mConnectionHandlerThread.looper).postDelayed({

            val last = mConnected

            mConnected = sDeviceViewModel?.isConnected() ?: false

            var name = sDeviceViewModel?.getDeviceInfo()?.deviceId

            if (mConnected) {
                mPrefs.edit().putString(mKeyUtil.lastConnectedDeviceId, name).apply()
            } else {
                name = mPrefs.getString(mKeyUtil.lastConnectedDeviceId, "")
                mPrefs.edit().putString(mKeyUtil.lastConnectedDeviceId, "").apply()
            }

            if (last != mConnected) {

                runOnUiThread {

                    notify(if (mConnected) getString(R.string.connected, name)
                    else getString(R.string.disconnect, name))

                }
            }

            startConnectionWatcher()

        }, 1500)
    }

    /**
     * Displays a snack bar message.
     */
    fun notify(message: String) {

        runOnUiThread {
            mSnackbar.push(
                SnackbarQueue
                    .SnackJob(
                        mBinding.actMainCoordinatorLayout,
                        message
                    )
            )
        }
    }

    fun notify(id: Int) {

        notify(getString(id))

    }

    fun notifyButton(text: String, undo: String, onClick: () -> Unit) {

        runOnUiThread {
            mSnackbar.push(SnackbarQueue.SnackJob(mBinding.actMainCoordinatorLayout, text, undo) {

                onClick()

            })
        }
    }

    private fun startLoadSampleData() {

        launch {

            withContext(Dispatchers.IO) {

                loadSampleData()

                runOnUiThread {

                    notify(R.string.samples_loaded)

                }
            }
        }
    }

    private val permissionGranter = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

        if (permissions.all { it.value }) {

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {

                startActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

            } else {

                startDeviceConnection()

            }

            val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var gps = false
            var net = false

            try {

                gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

            } catch (ex: java.lang.Exception) {

                ex.printStackTrace()

            }

            try {

                net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            } catch (e: java.lang.Exception) {

                e.printStackTrace()

            }

            if (!(net || gps)) {

                askForLocation {

                    startActivityLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

                }
            }
        }
    }

    private val introActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            setupActivity()

        }
    }

    fun switchInnoSpectra() {

        stopDeviceConnection()

        mPrefs.edit().putString(mKeyUtil.deviceMaker, DEVICE_TYPE_NANO).apply()

        sDeviceViewModel = InnoSpectraViewModel()

        runtimeBluetoothCheck()

    }

    fun switchLinkSquare() {

        stopDeviceConnection()

        mPrefs.edit().putString(mKeyUtil.deviceMaker, DEVICE_TYPE_LS1).apply()

        sDeviceViewModel = LinkSquareViewModel()

        startDeviceConnection()
    }

    private fun runtimeBluetoothCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionGranter.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            permissionGranter.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        Log.d(TAG, sViewModel.toString())

        if ("release" in BuildConfig.FLAVOR) {

            Thread.setDefaultUncaughtExceptionHandler { _, throwable ->

                Log.e("ProspectorCrash", throwable.message ?: "Unknown Error")

                throwable.printStackTrace()

            }
        }

        //check cold load, load sample data and navigate to intro activity
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (prefs.getBoolean("FIRST_LOAD", true)) {

            prefs.edit().putBoolean("FIRST_LOAD", false).apply()

            introActivityResult.launch(Intent(this, IntroActivity::class.java))

        } else {

            setupActivity()

        }
    }

    fun showAskFactoryReset(function: () -> Unit) {

        runtimeBluetoothCheck()

        mConfirmFactoryResetDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_confirm_factory_reset)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->

                function()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->

                dialog.dismiss()
            }.create()

        mConfirmFactoryResetDialog?.show()
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

    fun showCitationDialog(uri: Uri) {
        runOnUiThread {
            if (mCitationDialog != null && mCitationDialog?.isShowing != true) {
                mCitationDialog?.show()
            }

            if (mPrefs.getBoolean(mKeyUtil.shareEnabled, false)) {
                val subject = getString(R.string.share_file_subject)
                val text = getString(R.string.share_file_text)

                IntentUtil.shareFile(this, uri, subject, text)
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
        launch {
            withContext(Dispatchers.IO) {
                sDeviceViewModel?.connect(this@MainActivity.applicationContext)
            }
        }
    }

    private fun stopDeviceConnection() {
        launch {
            withContext(Dispatchers.IO) {
                if (sDeviceViewModel?.isConnected() == true)
                    sDeviceViewModel?.disconnect(this@MainActivity)

            }
        }
    }

    override fun onDestroy() {

        stopDeviceConnection()

        mCitationDialog?.dismiss()
        mAskChangeOperatorDialog?.dismiss()
        mAskForOperatorDialog?.dismiss()
        mConfirmFactoryResetDialog?.dismiss()
        mFirstDeleteDatabaseDialog?.dismiss()
        mSecondDeleteDatabaseDialog?.dismiss()
        mAskLocationEnableDialog?.dismiss()

        mConnectionHandlerThread.quit()

        super.onDestroy()

    }

    override fun onPause() {

        launch {
            withContext(Dispatchers.IO) {
                sDeviceViewModel?.reset(this@MainActivity)
            }
        }

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

                    notify(R.string.double_back_press)

                    Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                }
                R.id.linksquare_settings_fragment -> {

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
