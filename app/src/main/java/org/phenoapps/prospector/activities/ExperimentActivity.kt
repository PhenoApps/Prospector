package org.phenoapps.prospector.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.phenoapps.prospector.DatabaseManager
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.databinding.ActivityMainBinding
import org.phenoapps.prospector.models.Experiment
import java.text.SimpleDateFormat
import java.util.*

class ExperimentActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mDatabase: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        mDatabase = DatabaseManager(this)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mBinding.recyclerView.adapter = ExperimentAdapter(mBinding.root.context)

        updateExperimentList()

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val name = viewHolder.itemView.findViewById<TextView>(R.id.experiment_name).text

                mDatabase.deleteExperiment(name.toString())

                updateExperimentList()

            }

        }).attachToRecyclerView(mBinding.recyclerView)

        mBinding.onClick = View.OnClickListener {

            val value = mBinding.experimentIdEditText.text

            if (value.isNotEmpty()) {

                val cal = Calendar.getInstance()

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault())

                mDatabase.insertExperiment(value.toString())

                updateExperimentList()

                mBinding.experimentIdEditText.text.clear()

                Snackbar.make(mBinding.root,
                        "New Experiment $value added.", Snackbar.LENGTH_SHORT).show()

            } else {

                Snackbar.make(mBinding.root,
                        "You must enter an experiment name.", Snackbar.LENGTH_LONG).show()
            }
        }

        mBinding.executePendingBindings()
    }

    private fun updateExperimentList() {

        val list = ArrayList<Experiment>()

        val cursor = mDatabase.experiments

        if (cursor.moveToFirst()) {

            do {

                val id = cursor.getInt(cursor.getColumnIndex("localDatabaseID"))

                val name = cursor.getString(cursor.getColumnIndex("experiment"))

                list.add(Experiment(id, name))

            } while (cursor.moveToNext())
        }

        mBinding.recyclerView.layoutManager = LinearLayoutManager(this)

        (mBinding.recyclerView.adapter as ExperimentAdapter).submitList(list)

    }
}