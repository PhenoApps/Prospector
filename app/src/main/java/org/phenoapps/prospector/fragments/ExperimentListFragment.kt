package org.phenoapps.prospector.fragments

import ALPHA_ASC
import ALPHA_DESC
import DATE_ASC
import DATE_DESC
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaelflisar.changelog.ChangelogBuilder
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import com.michaelflisar.changelog.internal.ChangelogDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.BuildConfig
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.viewmodels.ExperimentViewModel
import org.phenoapps.prospector.databinding.FragmentExperimentListBinding
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.KeyUtil

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

    private val mKeys by lazy { KeyUtil(context) }

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

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            val version = BuildConfig.VERSION_CODE
            val savedVersion = prefs.getInt(mKeys.version, -1)

            if (version != savedVersion) {
                prefs.edit().putInt(mKeys.version, version).apply()
                showChangelog(managedShow = true, rateButton = true)

            }

            ui.fragExperimentListSbv.onClickSortOrder = { order ->
                mSortState = when (mSortState) {
                    ALPHA_ASC -> ALPHA_DESC
                    ALPHA_DESC -> ALPHA_ASC
                    DATE_ASC -> DATE_DESC
                    else -> DATE_ASC
                }

                updateUi()

                notifySortState()
            }

            ui.fragExperimentListSbv.onClickSortType = { type ->
                mSortState = when (mSortState) {
                    ALPHA_ASC -> DATE_ASC
                    ALPHA_DESC -> DATE_DESC
                    DATE_ASC -> ALPHA_ASC
                    else -> ALPHA_DESC
                }

                updateUi()

                notifySortState()
            }

            showDefiner()
        }

        return mBinding?.root
    }

    private fun showDefiner() {

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

    fun showChangelog(managedShow: Boolean, rateButton: Boolean) {

        (activity as? MainActivity)?.let { act ->
            val builder =  ChangelogBuilder()
                .withUseBulletList(true) // true if you want to show bullets before each changelog row, false otherwise
                .withManagedShowOnStart(managedShow) // library will take care to show activity/dialog only if the changelog has new infos and will only show this new infos
                .withRateButton(rateButton) // enable this to show a "rate app" button in the dialog => clicking it will open the play store; the parent activity or target fragment can also implement IChangelogRateHandler to handle the button click
                .withSummary(false, true) // enable this to show a summary and a "show more" button, the second paramter describes if releases without summary items should be shown expanded or not
                .withTitle(getString(R.string.changelog_title)) // provide a custom title if desired, default one is "Changelog <VERSION>"
                .withOkButtonLabel(getString(android.R.string.ok)) // provide a custom ok button text if desired, default one is "OK"
                .withSorter(ImportanceChangelogSorter())

            val dialog = ChangelogDialogFragment.create(builder, false)
            dialog.show(childFragmentManager, ChangelogDialogFragment::class.java.name)
        }
    }

    /**
     * Set button click events for the toolbar. This only includes the connection button.
     * When clicked if disconnected -> navigate to instructions page and attempt to connect
     * otherwise -> disconnect
     */
    private fun FragmentExperimentListBinding.setupToolbar() {

        toolbar.setOnMenuItemClickListener {

            when (it.itemId) {

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

    private fun notifySortState() {
        (activity as? MainActivity)?.notify(when (mSortState) {
            ALPHA_ASC -> R.string.sort_alpha_ascending
            ALPHA_DESC -> R.string.sort_alpha_descending
            DATE_ASC -> R.string.sort_date_ascending
            else -> R.string.sort_date_descending
        })
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

                        it.sortedByDescending { x -> x.name.lowercase() }
                    }

                    else -> {

                        it.sortedBy { x -> x.name.lowercase() }
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