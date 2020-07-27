package org.phenoapps.prospector.activities

import OPERATOR
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
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
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.fragments.ExperimentListFragmentDirections
import org.phenoapps.prospector.utils.SnackbarQueue
import java.io.File

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }

    private val sDeviceViewModel: DeviceViewModel by viewModels()

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mDrawerLayout: DrawerLayout

    private lateinit var mDrawerToggle: ActionBarDrawerToggle

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private val permissionCheck by lazy {

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            setupActivity()

        }
    }

    private fun disconnectDeviceAsync() {

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
        CoroutineScope(Dispatchers.IO).launch {

            writeStream(example, R.raw.scans_example)

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

        if (!sDeviceViewModel.isConnected()) {

            async {

                sDeviceViewModel.connection(this@MainActivity).observe(this@MainActivity, Observer {

                    it?.let {

                        when (it) {

                            is String -> {

                                mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, getString(R.string.connection_error)))

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


        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setupActivity()

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

        async {

            disconnectDeviceAsync()

        }

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
                else -> super.onBackPressed()
            }
        }
    }
}
