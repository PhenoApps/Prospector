package org.phenoapps.prospector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jjoe64.graphview.GraphView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.databinding.ListItemGraphedScanBinding
import org.phenoapps.prospector.fragments.ScanListFragmentDirections
import org.phenoapps.prospector.utils.AsyncLoadGraph

class ScansAdapter(private val lifecycle: LifecycleOwner, val context: Context, private val viewModel: ExperimentScansViewModel) : ListAdapter<Scan,
        ScansAdapter.ScanGraphViewHolder>(DiffCallbacks.Companion.ScanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanGraphViewHolder {

        return ScanGraphViewHolder(

                DataBindingUtil.inflate(

                        LayoutInflater.from(parent.context),

                        R.layout.list_item_graphed_scan, parent, false

                )
        )
    }

    override fun onBindViewHolder(holder: ScanGraphViewHolder, position: Int) {

        getItem(position).let { scan ->

            with(holder) {

                val graph = itemView.findViewById<GraphView>(R.id.graphView)

                if (scan != null) {

                    viewModel.forceSpectralValues(scan.eid, scan.sid).observe(lifecycle) { frames: List<SpectralFrame> ->

                        AsyncLoadGraph(context,
                                graph,
                                scan.sid,
                                context.getString(R.string.horizontal_axis),
                                context.getString(R.string.vertical_axis),
                                frames,
                                potatoRender = true).execute()

                    }

                    bind(scan, View.OnClickListener {

                        Navigation.findNavController(itemView).navigate(
                                ScanListFragmentDirections
                                        .actionToScanDetail(scan.eid, scan.sid))

                    })

                } else {

                    itemView.visibility = View.GONE

                }
            }
        }
    }

    inner class ScanGraphViewHolder(
            private val binding: ListItemGraphedScanBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: Scan, onClick: View.OnClickListener) {

            with(binding) {

                this.onClick = onClick

                this.scan = scan

                executePendingBindings()
            }
        }
    }
}