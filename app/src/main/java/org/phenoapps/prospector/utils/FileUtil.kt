package org.phenoapps.prospector.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.phenoapps.interfaces.spectrometers.Spectrometer.Companion.DEVICE_TYPE_INDIGO
import org.phenoapps.interfaces.spectrometers.Spectrometer.Companion.DEVICE_TYPE_LS1
import org.phenoapps.interfaces.spectrometers.Spectrometer.Companion.DEVICE_TYPE_NIR
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.DeviceTypeExport
import kotlin.math.min

/**
 * handles file io for exporting and importing data; although this application currently doesn't import data.
 * Headers are loaded from string resources so they could potentially be translated to other languages.
 *
 * This class also includes functions for representing the data as a Posting in JSON, but is currently unused.
 */
open class FileUtil(private val ctx: Context) {

    private val experimentNameHeader: String by lazy { ctx.getString(R.string.export_experiment_name_header) }

    private val deviceTypeHeader: String by lazy { ctx.getString(R.string.export_device_type_header) }

    private val operatorHeader: String by lazy { ctx.getString(R.string.export_operator_header) }

    private val scanIdHeader: String by lazy { ctx.getString(R.string.scan_name_header) }

    private val scanDateHeader: String by lazy { ctx.getString(R.string.scan_date_header) }

    private val scanDeviceIdHeader: String by lazy { ctx.getString(R.string.scan_device_id_header) }

    private val specLightSourceHeader: String by lazy { ctx.getString(R.string.spec_light_source_header) }

    private val scanNoteHeader: String by lazy { ctx.getString(R.string.scan_note_header) }

    private val serialHeader: String by lazy { ctx.getString(R.string.serial_header) }

    private val humidityHeader: String by lazy { ctx.getString(R.string.humidity_header) }

    private val temperatureHeader: String by lazy { ctx.getString(R.string.temperature_header) }

//    private val scanModelHeaderString by lazy {
//        arrayOf(scanIdHeader, scanDateHeader, scanDeviceIdHeader,
//                specFrameIdHeader, specLightSourceHeader, specCountHeader,
//                specValuesHeader, scanNoteHeader)
//                .joinToString(",")
//    }
//
//    private val exportHeaderString by lazy {
//        arrayOf(scanDateHeader, scanDeviceIdHeader, scanIdHeader,
//                specFrameIdHeader, specLightSourceHeader, specCountHeader,
//                specValuesHeader, scanNoteHeader)
//                .joinToString(",")
//    }

//    /**
//     * This function returns True if the required headers exist in the input file
//     */
//    private fun validateHeaders(headers: List<String>, ensured: List<String>): Boolean {
//
//        return headers.intersect(ensured).size == ensured.size
//
//    }

    /**
     * Function must be called wrapped in a Dispatchers.IO coroutine context
     * BlockingMethod suppressed because this function is called on Dispatchers.IO (background IO thread)
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun exportCsv(uri: Uri, exports: List<DeviceTypeExport>, convert: Boolean = false) = withContext(Dispatchers.IO) {

        //false warning, this function is run always in Dispatchers.IO
        ctx.contentResolver.openOutputStream(uri)?.use { stream ->

            val prefixHeaders = "${experimentNameHeader},$scanIdHeader,$scanDateHeader," +
                    "$deviceTypeHeader,$scanDeviceIdHeader,$serialHeader,$humidityHeader,$temperatureHeader,$operatorHeader," +
                    "$specLightSourceHeader,$scanNoteHeader,"

            if (convert) {

                val firstExport = exports.firstOrNull() //used to print header wavelength range, which is dependent on the device
                val first = firstExport?.toWaveArray(firstExport.deviceType)?.filter {

                    val deviceTypeMax = when(firstExport.deviceType) {

                        DEVICE_TYPE_NIR -> LinkSquareNIRExportRange.max

                        DEVICE_TYPE_LS1 -> LinkSquareExportRange.max

                        DEVICE_TYPE_INDIGO -> IndigoExportRange.max

                        else -> InnoSpectraExportRange.max
                    }

                    val deviceTypeMin = when(firstExport.deviceType) {

                        DEVICE_TYPE_NIR -> LinkSquareNIRExportRange.min

                        DEVICE_TYPE_LS1 -> LinkSquareExportRange.min

                        DEVICE_TYPE_INDIGO -> IndigoExportRange.min

                        else -> InnoSpectraExportRange.min
                    }

                    it.first in deviceTypeMin..deviceTypeMax

                }

                first?.let { firstWave ->

                    val headers = (prefixHeaders + (firstWave.map { it.first }).joinToString(","))
                        .split(",").toTypedArray()

                    val csvWriter = CSVPrinter(stream.buffered().writer(), CSVFormat.DEFAULT.withHeader(*headers))

                    exports.forEach { export ->

                        val data = arrayOf(
                            export.experiment,
                            export.sample,
                            export.date,
                            export.deviceType,
                            export.deviceId.replace("[\" ]", ""),
                            export.serial,
                            export.humidity,
                            export.temperature,
                            export.operator,
                            export.lightSource,
                            export.note
                        )

                        val wave = export.toWaveArray(firstExport.deviceType).filter {

                            val deviceTypeMax = when(firstExport.deviceType) {

                                DEVICE_TYPE_NIR -> LinkSquareNIRExportRange.max

                                DEVICE_TYPE_LS1 -> LinkSquareExportRange.max

                                DEVICE_TYPE_INDIGO -> IndigoExportRange.max

                                else -> InnoSpectraExportRange.max
                            }

                            val deviceTypeMin = when(firstExport.deviceType) {

                                DEVICE_TYPE_NIR -> LinkSquareNIRExportRange.min

                                DEVICE_TYPE_LS1 -> LinkSquareExportRange.min

                                DEVICE_TYPE_INDIGO -> IndigoExportRange.min

                                else -> InnoSpectraExportRange.min
                            }

                            it.first in deviceTypeMin..deviceTypeMax
                        }

                        csvWriter.printRecord(*(data + wave.map { it.second.toString() }.toTypedArray()))
                    }

                    csvWriter.flush()

                    csvWriter.close()
                }

            } else {

                exports.firstOrNull()?.let { export ->

                    val size = export.spectralData.split(" ").size

                    val wavelengths = export.wavelengths?.split(" ")

                    val headers = (prefixHeaders + if (export.deviceType in arrayOf(DEVICE_TYPE_LS1, DEVICE_TYPE_NIR))
                        ((1..size).joinToString(","))
                    else (wavelengths?.joinToString(","))).split(",").toTypedArray()

                    val csvWriter = CSVPrinter(stream.buffered().writer(), CSVFormat.DEFAULT.withHeader(*headers))

                    exports.forEach { e ->

                        val data = arrayOf(
                            e.experiment,
                            e.sample,
                            e.date,
                            e.deviceType,
                            export.deviceId.replace("[\" ]", ""),
                            e.serial,
                            e.humidity,
                            e.temperature,
                            e.operator,
                            e.lightSource,
                            e.note
                        )

                        val frameData = e.spectralData.split(" ")

                        csvWriter.printRecord(*(data + frameData))

                    }

                    csvWriter.flush()

                    csvWriter.close()
                }
            }
        }
    }

    /**
     * posting : {
     * 1 : [(441.2,[1,3,4,7]), (442.3,[2,5,6,8]), ...]
     * 2 :
     * 3 :
     * }
     *
     * experiments : [{ name, date ...,
     *  scans: [
     *      { samplename, lightsource, device, ...
     *          scanIndex: 1 }
     *  ]
     * ]
     *
     */
//    suspend fun exportJsonPosting(uri: Uri, exps: List<Experiment>, samples: List<Sample>, scans: List<Scan>, frames: List<SpectralFrame>, convert: Boolean) = withContext(Dispatchers.IO) {
//
//        val writer = JsonWriter(OutputStreamWriter(ctx.contentResolver.openOutputStream(uri)))
//
//        writer.setIndent(" ")
//
//        try {
//
//            writer.beginObject()
//
//            writer.name("experiments")
//
//            writer.beginArray()
//
//            exps.forEach { experiment ->
//
//                writer.beginObject()
//
//                writer.name("name").value(experiment.name)
//
//                writer.name("date").value(experiment.date)
//
//                writer.name("scans")
//
//                writer.beginArray()
//
//                scans.filter { it.eid == experiment.eid }.forEach { scan ->
//
//                    writer.beginObject()
//
//                    writer.name("id").value(scan.sid)
//                    writer.name("sample").value(scan.name)
//                    writer.name("deviceId").value(scan.deviceId)
//                    writer.name("deviceType").value(scan.deviceType)
//                    writer.name("date").value(scan.date)
//                    writer.name("operator").value(scan.operator)
//
//                    writer.endObject()
//
//                }
//
//                writer.endArray()
//
//                writer.endObject()
//
//            }
//
//            writer.endArray()
//
//            //writer.endObject()
//
//            writer.name("posting")
//
//            writer.beginObject()
//
//            val posting = Posting()
//
//            frames.forEach { frame ->
//
//                if (convert) {
//
//                    val wave = frame.toWaveMap()
//
//                    wave.forEach { key ->
//
//                        posting[key.first, key.second] = frame.sid
//
//                    }
//
//                } else {
//
//                    frame.spectralValues.split(" ").forEachIndexed { index, s ->
//
//                        posting[index + 1.0, s.toDouble()] = frame.sid
//
//                    }
//
//                }
//            }
//
//            posting.keys.forEach { index ->
//
//                writer.name(index.toString())
//
//                writer.beginObject()
//
//                posting[index]?.forEach { post ->
//
//                    writer.name(post.key.toString())
//
//                    writer.value(post.value.valueIterator().asSequence().joinToString(","))
//
//                }
//
//                writer.endObject()
//
//            }
//
//            writer.endObject()
//
//            writer.endObject()
//
//            Log.d("ProspectorPosting", posting.size.toString())
//
//        } catch (e: IllegalStateException) {
//
//            Log.d("ProspectorJsonError", e.message ?: "")
//
//            e.printStackTrace()
//
//        } finally {
//
//            writer.close()
//
//        }
//
//    }
//
//    suspend fun exportJson(uri: Uri, exports: List<DeviceTypeExport>, convert: Boolean) = withContext(Dispatchers.IO) {
//
//        val writer = JsonWriter(OutputStreamWriter(ctx.contentResolver.openOutputStream(uri)))
//
//        writer.setIndent(" ")
//
//        try {
//
//            writer.beginArray()
//
//            exports.groupBy { it.experiment }.forEach { entry ->
//
//                writer.beginObject()
//
//                writer.name(entry.key)
//
//                writer.beginArray()
//
//                entry.value.groupBy { it.sample }.forEach { samples ->
//
//                    writer.beginObject()
//
//                    writer.name(samples.key)
//
//                    writer.beginArray()
//
//                    samples.value.groupBy { it.lightSource }.forEach { source ->
//
//                        writer.beginObject()
//
//                        writer.name(source.key)
//
//                        writer.beginArray()
//
//                        source.value.forEach { scan ->
//
//                            writer.beginObject()
//
//                            writer.name("date").value(scan.date)
//
//                            writer.name("operator").value(scan.operator)
//
//                            writer.name("spectralValues")
//
//                            writer.beginObject()
//
//                            writer.setIndent("")
//
//                            if (convert) {
//
//                                val wave = scan.spectralData.toWaveMap()
//
//                                wave.forEach { key ->
//
//                                    writer.name(key.first.toString()).value(key.second.toString())
//
//                                }
//
//                            } else {
//
//                                val tokens = scan.spectralData.split(" ")
//
//                                tokens.forEachIndexed { index, s ->
//
//                                    writer.name((index+1).toString()).value(s)
//
//                                }
//
//                            }
//
//                            writer.endObject()
//
//                            writer.setIndent(" ")
//
//                            writer.endObject()
//                        }
//
//                        writer.endArray()
//
//                        writer.endObject()
//                    }
//
//                    writer.endArray()
//
//                    writer.endObject()
//
//                }
//
//                writer.endArray()
//
//                writer.endObject()
//            }
//
//            writer.endArray()
//
//        } catch (e: IllegalStateException) {
//
//            Log.d("ProspectorJsonError", e.message)
//
//            e.printStackTrace()
//
//        } finally {
//
//            writer.flush()
//
//            writer.close()
//
//        }
//
//    }
//
//    fun export(uri: Uri, json: JSONArray) {
//
//        try {
//
//            ctx.contentResolver.openOutputStream(uri).apply {
//
//                this?.let {
//
//                    write(json.toString(4).toByteArray())
//
//                    close()
//                }
//
//            }
//
//        } catch (e: IOException) {
//
//            e.printStackTrace()
//
//        }
//
//        MediaScannerConnection.scanFile(ctx, arrayOf(uri.path), arrayOf("*/*"), null)
//
//    }
}