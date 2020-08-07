package org.phenoapps.prospector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jjoe64.graphview.GraphView
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.databinding.ListItemGraphedScanBinding
import org.phenoapps.prospector.fragments.ScanListFragmentDirections
import org.phenoapps.prospector.utils.centerViewport
import org.phenoapps.prospector.utils.renderPotato
import org.phenoapps.prospector.utils.setViewportGrid
import org.phenoapps.prospector.utils.toPixelArray

class ScansAdapter(private val lifecycle: LifecycleOwner, val context: Context, private val viewModel: ExperimentSamplesViewModel) : ListAdapter<Scan,
        ScansAdapter.ScanGraphViewHolder>(DiffCallbacks.Companion.ScanDiffCallback()) {

    private val jobMap: MutableMap<ScanGraphViewHolder, Job> = HashMap()

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

                itemView.tag = scan.sid

                //if (holder in jobMap) viewModel.viewModelScope.cancel()

                //holder.setIsRecyclable(false)

                val graph = itemView.findViewById<GraphView>(R.id.graphView)

                //if (position == 0) graph.visibility = View.VISIBLE

                jobMap[holder] = viewModel.viewModelScope.launch {

                    val data = async {

                        return@async withContext(Dispatchers.IO) {

                            return@withContext viewModel.getSpectralValues(scan.eid, scan.sid ?: -1L)

                        }
                    }

                    val preGraph = async {

                        setViewportGrid(graph)

                    }

                    val frames = data.await()

                    preGraph.await()

                    val renderData = withContext(Dispatchers.Main) {

                        val wavelengths = frames.toPixelArray()

                        centerViewport(graph, wavelengths)

                        renderPotato(graph, wavelengths)

                    }

                }

                bind(scan, View.OnClickListener {

                    Navigation.findNavController(itemView).navigate(
                            ScanListFragmentDirections
                                    .actionToScanDetail(scan.eid, scan.name, scan.sid ?: -1L))

                })

//                } else {
//
//                    itemView.visibility = View.GONE
//
//                }
            }

        }
    }

    inner class ScanGraphViewHolder(
            private val binding: ListItemGraphedScanBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: Scan, onClick: View.OnClickListener) {

            with(binding) {

                if (itemView.tag == scan.sid) {

                    this.nameView.text = scan.date

                    this.onClick = onClick

                    this.minimizeOnClick = View.OnClickListener {

                        graphView.visibility = when (graphView.visibility) {
                            View.GONE -> View.VISIBLE
                            else -> View.GONE
                        }

                    }

                    this.scan = scan

                }
            }
        }
    }
}