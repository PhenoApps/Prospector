package org.phenoapps.prospector.fragments

import ALPHA_ASC
import ALPHA_DESC
import DATE_ASC
import DATE_DESC
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.viewmodels.ExperimentViewModel
import org.phenoapps.prospector.databinding.FragmentExperimentListBinding
import org.phenoapps.prospector.utils.Dialogs
import java.util.*

/**
 * The main data fragment that displays the top-level experiment hierarchy.
 * Whenever the data bot nav button is pressed, the app navigates to this fragment.
 */
@WithFragmentBindings
@AndroidEntryPoint
class ExperimentListFragment : ConnectionFragment(R.layout.fragment_experiment_list), CoroutineScope by MainScope() {

    companion object {
        const val REQUEST_STORAGE_DEFINER = "org.phenoapps.prospector.requests.storage_definer"
    }

    /**
     * Used to query experiment list
     * This is a common pattern to use the fragment delegates to load view models.
     */
    private val sViewModel: ExperimentViewModel by viewModels()

    /**
     * Navigates to the new experiment fragment whenever the fab button is pressed.
     */
    private val sOnNewExpClick = View.OnClickListener {

        if (findNavController().currentDestination?.id == R.id.experiment_list_fragment) {
            findNavController().navigate(ExperimentListFragmentDirections
                .actionToNewExperiment())
        }
    }

    private var mBinding: FragmentExperimentListBinding? = null

    private var mSortState = ALPHA_ASC

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_experiment_list, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView()

            ui.setupToolbar()

            setupButtons()

            updateUi()

            //ask the user once, otherwise use the settings to define the storage location
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getBoolean("STORAGE_DEFINE", true)) {

                prefs.edit().putBoolean("STORAGE_DEFINE", false).apply()

                setFragmentResultListener(REQUEST_STORAGE_DEFINER) { code, bundle ->

                    if (code == REQUEST_STORAGE_DEFINER) {

                        (activity as? MainActivity)?.askSampleImport()

                    }
                }

                findNavController().navigate(ExperimentListFragmentDirections
                    .actionToStorageDefiner())

            } else (activity as? MainActivity)?.askSampleImport()
        }

        return mBinding?.root
    }

    /**
     * Set button click events for the toolbar. This only includes the connection button.
     * When clicked if disconnected -> navigate to instructions page and attempt to connect
     * otherwise -> disconnect
     */
    private fun FragmentExperimentListBinding.setupToolbar() {

        toolbar.setOnMenuItemClickListener {

            when (it.itemId) {

                R.id.action_experiment_list_menu_sort -> {

                    mSortState = when (mSortState) {
                        ALPHA_ASC -> ALPHA_DESC
                        ALPHA_DESC -> DATE_ASC
                        DATE_ASC -> DATE_DESC
                        else -> ALPHA_ASC
                    }

                    Toast.makeText(context,
                        when (mSortState) {
                            ALPHA_ASC -> getString(R.string.sort_alpha_ascending)
                            ALPHA_DESC -> getString(R.string.sort_alpha_descending)
                            DATE_ASC -> getString(R.string.sort_date_ascending)
                            else -> getString(R.string.sort_date_descending)
                        }, Toast.LENGTH_SHORT
                    ).show()

                    updateUi()
                }

                R.id.action_connection -> {

                    val deviceViewModel = (activity as MainActivity).sDeviceViewModel

                    if (deviceViewModel?.isConnected() == true) {

                        deviceViewModel.reset(context)

                    } else {

                        if (findNavController().currentDestination?.id == R.id.experiment_list_fragment) {
                            findNavController().navigate(ExperimentListFragmentDirections
                                .actionToConnectInstructions())
                        }

                        (activity as? MainActivity)?.startDeviceConnection()
                    }
                }
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

                } else mBinding?.recyclerView?.adapter?.notifyItemChanged(viewHolder.absoluteAdapterPosition)
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

        sViewModel.getExperimentCounts().observe(viewLifecycleOwner) {

            (mBinding?.recyclerView?.adapter as? ExperimentAdapter)
                ?.submitList(when (mSortState) {

                    DATE_DESC -> {

                        it.sortedByDescending { x -> x.date }
                    }

                    DATE_ASC -> {

                        it.sortedBy { x -> x.date }
                    }

                    ALPHA_DESC -> {

                        it.sortedByDescending { x -> x.name }
                    }

                    else -> {

                        it.sortedBy { x -> x.name }
                    }
                })

            mBinding?.recyclerView?.adapter?.notifyItemRangeChanged(0, it.size)
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_data)

    }
}