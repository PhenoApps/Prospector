package org.phenoapps.prospector.fragments

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentScanViewModelFactory
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.databinding.FragmentExperimentListBinding
import org.phenoapps.prospector.utils.DateUtil
import java.io.File

class ExperimentListFragment : Fragment() {

    private lateinit var sViewModel: ExperimentScansViewModel

    private lateinit var mBinding: FragmentExperimentListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_experiment_list, container, false)

        val viewModel: ExperimentScansViewModel by viewModels {

            ExperimentScanViewModelFactory(
                    ExperimentScansRepository.getInstance(
                            ProspectorDatabase.getInstance(requireContext())
                                    .expScanDao()))

        }

        sViewModel = viewModel

        setupRecyclerView()

        setupButtons()

        startObservers()

        setHasOptionsMenu(true)

        return mBinding.root
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

        mBinding.onClick = View.OnClickListener {

            val value = mBinding.experimentIdEditText.text

            if (value.isNotBlank()) {

                sViewModel.insertExperiment(Experiment(value.toString()).apply {
                    this.date = DateUtil().getTime()
                })

                mBinding.experimentIdEditText.text.clear()

                Snackbar.make(mBinding.root,
                        "New Experiment $value added.", Snackbar.LENGTH_SHORT).show()

            } else {

                Snackbar.make(mBinding.root,
                        "You must enter an experiment name.", Snackbar.LENGTH_LONG).show()
            }
        }

    }

    private fun setupRecyclerView() {

        mBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        mBinding.recyclerView.adapter = ExperimentAdapter(requireContext())

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                return false

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val id = viewHolder.itemView.tag as Long

                sViewModel.deleteExperiment(id)

            }

        }).attachToRecyclerView(mBinding.recyclerView)
    }

    private fun startObservers() {

        sViewModel.experiments.observe(viewLifecycleOwner) {

            (mBinding.recyclerView.adapter as ExperimentAdapter)
                    .submitList(it)

            Handler().postDelayed({

                mBinding.recyclerView.scrollToPosition(0)

            }, 250)

        }
    }
}