package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.ExperimentViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentViewModelFactory
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.databinding.FragmentExperimentListBinding
import org.phenoapps.prospector.utils.Dialogs

/**
 * The main data fragment that displays the top-level experiment hierarchy.
 * Whenever the data bot nav button is pressed, the app navigates to this fragment.
 */
class ExperimentListFragment : Fragment(), CoroutineScope by MainScope() {

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    /**
     * Used to query experiment list
     * This is a common pattern to use the fragment delegates to load view models.
     */
    private val sViewModel: ExperimentViewModel by viewModels {

        ExperimentViewModelFactory(
                ExperimentRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .experimentDao()))

    }

    /**
     * Navigates to the new experiment fragment whenever the fab button is pressed.
     */
    private val sOnNewExpClick = View.OnClickListener {

        findNavController().navigate(ExperimentListFragmentDirections
                .actionToNewExperiment())
    }

    private var mBinding: FragmentExperimentListBinding? = null

    /**
     * All permissions are checked here, if one is not accepted the app finishes.
     * Also here is where the instructions page is loaded (if on cold load)
     */
    private val checkPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

            //ensure all permissions are granted
            if (!granted.values.all { it }) {
                activity?.let {
                    it.setResult(android.app.Activity.RESULT_CANCELED)
                    it.finish()
                }
            } else {

                //check cold load
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)

                if (prefs.getBoolean("FIRST_LOAD", true)) {

                    prefs.edit().putBoolean("FIRST_LOAD", false).apply()

                    //navigate to instructions page
                    findNavController().navigate(ExperimentListFragmentDirections
                            .actionToConnectInstructions())
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_experiment_list, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView()

            ui.setupToolbar()

            setupButtons()

            updateUi()

            checkPermissions.launch(arrayOf(android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

        }

        //use the activity view model to access the current connection status
        sDeviceViewModel.isConnectedLive().observeForever { connected ->

            connected?.let { status ->

                with(mBinding?.experimentToolbar) {

                    this?.menu?.findItem(R.id.action_connection)
                            ?.setIcon(if (status) R.drawable.ic_bluetooth_connected_black_18dp
                                else R.drawable.ic_clear_black_18dp)

                }

            }
        }

        return mBinding?.root
    }

    /**
     * Set button click events for the toolbar. This only includes the connection button.
     * When clicked if disconnected -> navigate to instructions page and attempt to connect
     * otherwise -> disconnect
     */
    private fun FragmentExperimentListBinding.setupToolbar() {

        experimentToolbar.setOnMenuItemClickListener { item ->

            if (sDeviceViewModel.isConnected()) {

                sDeviceViewModel.reset()

            } else {

                findNavController().navigate(ExperimentListFragmentDirections
                        .actionToConnectInstructions())

                (activity as? MainActivity)?.startDeviceConnection()
            }

            true
        }
    }

    private fun setupButtons() {

        mBinding?.onClick = sOnNewExpClick

    }

    /**
     * Swipe event listener for the recycler view. This deletes experiments.
     */
    private val sItemTouch = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean { return false }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            //whenever an item is swiped to be removed, call a dialog to verify this action to the user
            Dialogs.onOk(AlertDialog.Builder(requireContext()),
                    getString(R.string.ask_delete_experiment),
                    getString(R.string.cancel),
                    getString(R.string.ok)) {

                if (it) {

                    val id = viewHolder.itemView.tag as Long

                    launch {

                        sViewModel.deleteExperiment(id)

                    }

                } else  mBinding?.recyclerView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)
            }
        }
    }

    private fun FragmentExperimentListBinding.setupRecyclerView() {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = ExperimentAdapter(requireContext())

        ItemTouchHelper(sItemTouch).attachToRecyclerView(recyclerView)
    }

    /**
     * Live data observer for the experiments list. This will update automatically when new experiments are added.
     * experimentCount is a live data query that also counts the number of samples in each experiment.
     * The query uses a left join which counts experiments as '1' even though they have no samples;
     * therefore, further logic is added in the adapter to ensure the count is 0 or 1.
     */
    private fun updateUi() {

        sViewModel.experimentCounts.observe(viewLifecycleOwner, {

            (mBinding?.recyclerView?.adapter as? ExperimentAdapter)
                    ?.submitList(it)

        })
    }
}