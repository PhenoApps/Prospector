package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.FragmentExperimentListBinding
import org.phenoapps.prospector.utils.Dialogs

class ExperimentListFragment : Fragment(), CoroutineScope by MainScope() {

    private val sExpSamples: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private val sOnNewExpClick = View.OnClickListener {

        findNavController().navigate(ExperimentListFragmentDirections
                .actionToNewExperiment())
    }

    private var mBinding: FragmentExperimentListBinding? = null

    private val checkPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

            //ensure all permissions are granted
            if (!granted.values.all { it }) {
                activity?.let {
                    it.setResult(android.app.Activity.RESULT_CANCELED)
                    it.finish()
                }
            } else {

                val prefs = PreferenceManager.getDefaultSharedPreferences(context)

                if (prefs.getBoolean("FIRST_LOAD", true)) {

                    prefs.edit().putBoolean("FIRST_LOAD", false).apply()

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

            setupButtons()

            updateUi()

            checkPermissions.launch(arrayOf(android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

        }

        setHasOptionsMenu(true)

        (activity as? AppCompatActivity)?.supportActionBar?.title = ""

        return mBinding?.root
    }

    private fun setupButtons() {

        mBinding?.onClick = sOnNewExpClick

    }

    private fun FragmentExperimentListBinding.setupRecyclerView() {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = ExperimentAdapter(requireContext())

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                return false

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        getString(R.string.ask_delete_experiment),
                        getString(R.string.cancel),
                        getString(R.string.ok)) {

                    if (it) {

                        val id = viewHolder.itemView.tag as Long

                        launch {

                            sExpSamples.deleteExperiment(id)

                        }

                    } else  mBinding?.recyclerView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)

                }
            }

        }).attachToRecyclerView(recyclerView)
    }

    private fun updateUi() {

        sExpSamples.experimentCounts.observe(viewLifecycleOwner, {

            (mBinding?.recyclerView?.adapter as? ExperimentAdapter)
                    ?.submitList(it)

            //queueScroll()
        })
    }

    private fun queueScroll() {

        mBinding?.let { ui ->

            Handler().postDelayed({

                ui.recyclerView.scrollToPosition(0)

            }, 250)
        }
    }
}