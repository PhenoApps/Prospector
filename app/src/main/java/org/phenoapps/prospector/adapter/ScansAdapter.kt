package org.phenoapps.prospector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jjoe64.graphview.GraphView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.databinding.ListItemGraphedScanBinding
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

                        AsyncLoadGraph(graph,
                                scan.sid,
                                context.getString(R.string.horizontal_axis),
                                context.getString(R.string.vertical_axis),
                                frames).execute()

                    }

                    bind(View.OnClickListener {

//                    val intent = Intent(context, GraphActivity::class.java)
//
//                    intent.putExtra("sid", scan.sid)
//
//                    intent.putExtra("eid", scan.eid)
//
//                    startActivity(context, intent, null)

                    }, scan)

                } else {

                    itemView.visibility = View.GONE

                }
            }
        }
    }

    inner class ScanGraphViewHolder(
            private val binding: ListItemGraphedScanBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, scan: Scan) {

            with(binding) {

                this.onClick = listener

                this.scan = scan

                executePendingBindings()
            }
        }
    }
}