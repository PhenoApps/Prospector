package org.phenoapps.prospector.data.models

import androidx.room.DatabaseView

@DatabaseView("""
SELECT DISTINCT S.eid as eid, S.name as name, S.date as date, S.note as note,
	(SELECT COUNT(*) 
	FROM scans, spectral_frames
	WHERE S.eid = scans.eid AND S.name = scans.name AND scans.sid = spectral_frames.sid) as count
from samples as S
""")
data class SampleFramesCount(
        val eid: Long,
        val name: String,
        val date: String,
        val note: String,
        val count: Int
)