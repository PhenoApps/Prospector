package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Experiment
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

        val newExpString = getString(R.string.new_experiment_prefix)

        val tryEnterNameString = getString(R.string.new_exp_must_enter_name)

        mBinding?.let { ui ->

            val value = ui.experimentIdEditText.text.toString().trim()

            if (value.isNotBlank()) {

                launch {

                    val eid = sExpSamples.insertExperiment(Experiment(value)).await()

                    Snackbar.make(ui.root,
                            "$newExpString: $value.", Snackbar.LENGTH_SHORT).show()

                    ui.experimentIdEditText.text.clear()

                    updateUi()
                }

            } else {

                Snackbar.make(ui.root,
                        tryEnterNameString, Snackbar.LENGTH_LONG).show()

            }
        }
    }

    private var mBinding: FragmentExperimentListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_experiment_list, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView()

            setupButtons()

            updateUi()

            return ui.root

        }

        setHasOptionsMenu(true)

        return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

//        inflater.inflate(R.menu.activity_main_toolbar, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

//        when(item.itemId) {
//
//        }
        return super.onOptionsItemSelected(item)
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

                        //mBinding?.recyclerView?.adapter?.notifyItemRemoved(viewHolder.adapterPosition)
                        //updateUi()

                        //queueScroll()

                    } else  mBinding?.recyclerView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)

                }
            }

        }).attachToRecyclerView(recyclerView)
    }

    private fun updateUi() {

        sExpSamples.experiments.observe(viewLifecycleOwner, Observer {

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