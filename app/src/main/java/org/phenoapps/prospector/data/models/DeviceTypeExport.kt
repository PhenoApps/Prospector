package org.phenoapps.prospector.data.models

import androidx.room.DatabaseView

@DatabaseView("""
SELECT 
    E.eid as experimentId,
    E.name as experiment, 
    S.name as sample, 
    S.date as date, 
    S.deviceType as deviceType,     
    S.deviceId as deviceId,
    S.serial as serial,
    S.humidity as humidity,
    S.temperature as temperature,
    S.operator as operator, 
    F.lightSource as lightSource, 
    F.spectralValues as spectralData, 
    F.wavelengths as wavelengths,
    SA.note as note
FROM experiments as E
INNER JOIN samples as SA ON SA.eid = E.eid
INNER JOIN scans as S ON S.eid = E.eid AND SA.name = S.name
INNER JOIN spectral_frames as F on F.sid = S.sid
""")
data class DeviceTypeExport(
    val experimentId: Long,
    val experiment: String,
    val sample: String,
    val date: String,
    val deviceType: String,
    val deviceId: String,
    val serial: String?,
    val humidity: String?,
    val temperature: String?,
    val operator: String,
    val lightSource: String,
    val spectralData: String,
    val wavelengths: String?,
    val note: String
)