package org.phenoapps.prospector.data.models

import androidx.room.DatabaseView

@DatabaseView("""
SELECT DISTINCT S.eid as eid, S.name as name, S.date as date, S.note as note,
	(SELECT COUNT(*) 
	FROM scans
	WHERE S.eid = scans.eid and S.name = scans.name) as count
from samples as S
""")
data class SampleScanCount(
        val eid: Long,
        val name: String,
        val date: String,
        val note: String,
        val count: Int
)