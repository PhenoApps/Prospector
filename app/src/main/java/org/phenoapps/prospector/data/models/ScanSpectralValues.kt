package org.phenoapps.prospector.data.models

import androidx.room.DatabaseView

@DatabaseView("""
SELECT DISTINCT s.sid as "sid", s.eid as "eid", s.date as "scanDate", s.deviceId as "deviceId",
                sf.fid as "frameId", sf.count as "count", sf.spectralValues as "spectralValues"
FROM scans as s
LEFT JOIN spectral_frames as sf ON s.sid = sf.sid
""")
data class ScanSpectralValues(
        val sid: String,
        val eid: Long,
        val scanDate: String,
        val deviceId: String,
        val frameId: Int?,
        val count: Int?,
        val spectralValues: String?
)