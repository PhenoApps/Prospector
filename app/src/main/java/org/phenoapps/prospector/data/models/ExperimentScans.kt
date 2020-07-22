package org.phenoapps.prospector.data.models

import androidx.room.DatabaseView


@DatabaseView("""
SELECT DISTINCT e.eid as "eid", e.date as "expDate", e.name as "expName", 
                s.sid as "sid", s.date as "scanDate", s.deviceId as "deviceId"
FROM experiments as e
LEFT JOIN scans as s ON s.eid = e.eid
""")
data class ExperimentScans(
        val eid: Long,
        val expDate: String?,
        val expName: String?,
        val sid: String?,
        val scanDate: String?,
        val deviceId: String?
)