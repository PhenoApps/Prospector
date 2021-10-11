package org.phenoapps.prospector.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.models.Experiment

@Dao
interface ExperimentDao {

    /** Select queries **/
    @Query("SELECT DISTINCT * FROM experiments ORDER BY date DESC")
    fun getExperiments(): LiveData<List<Experiment>>

    @Query("""
            SELECT E.eid AS id, E.deviceType AS deviceType, E.date AS date, E.name AS name, S.name AS sampleName, COUNT(*) AS count 
            FROM experiments AS E LEFT JOIN samples AS S ON S.eid = E.eid 
            GROUP BY E.eid""")
    fun getExperimentCounts(): LiveData<List<ExperimentAdapter.ExperimentListItem>>

    /**
     * Inserts
     */
    @Query("INSERT INTO experiments (name, deviceType, date) VALUES (:name, :deviceType, :date)")
    suspend fun insertExperiment(name: String, deviceType: String, date: String): Long

    /**
     * Deletes
     */
    @Query("DELETE FROM experiments WHERE eid = :eid")
    suspend fun deleteExperiment(eid: Long)

}