package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentScanViewModelFactory
import org.phenoapps.prospector.databinding.FragmentExperimentListBinding
import org.phenoapps.prospector.utils.DateUtil

class ExperimentListFragment : Fragment(), CoroutineScope by MainScope() {

    private val sExpScanModel: ExperimentScansViewModel by viewModels {

        ExperimentScanViewModelFactory(
                ExperimentScansRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private var mBinding: FragmentExperimentListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_experiment_list, container, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView()

            ui.setupButtons()

            ui.startObservers()

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

    private fun FragmentExperimentListBinding.setupButtons() {

        val newExpString = getString(R.string.new_experiment_prefix)

        val tryEnterNameString = getString(R.string.new_exp_must_enter_name)

        onClick = View.OnClickListener {

            val value = experimentIdEditText.text

            if (value.isNotBlank()) {

                sExpScanModel.insertExperiment(Experiment(value.toString()).apply {

                    this.date = DateUtil().getTime()

                })

                experimentIdEditText.text.clear()

                Snackbar.make(root,
                        "$newExpString: $value.", Snackbar.LENGTH_SHORT).show()

            } else {

                Snackbar.make(root,
                        tryEnterNameString, Snackbar.LENGTH_LONG).show()

            }
        }
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

                val id = viewHolder.itemView.tag as Long

                sExpScanModel.deleteExperiment(id)

            }

        }).attachToRecyclerView(recyclerView)
    }

    private fun FragmentExperimentListBinding.startObservers() {

        sExpScanModel.experiments.observe(viewLifecycleOwner) {

            (recyclerView.adapter as ExperimentAdapter)
                    .submitList(it)

            Handler().postDelayed({

                recyclerView.scrollToPosition(0)

            }, 250)

        }
    }
}