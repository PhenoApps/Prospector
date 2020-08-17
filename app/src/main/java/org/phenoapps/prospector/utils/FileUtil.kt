package org.phenoapps.prospector.utils

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.JsonWriter
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.util.valueIterator
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.*
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.datastructures.Posting
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class FileUtil(private val ctx: Context) {

    /**
     * Cross table export file header fields.
     */

    private val experimentNameHeader: String by lazy { ctx.getString(R.string.export_experiment_name_header) }
    private val deviceTypeHeader: String by lazy { ctx.getString(R.string.export_device_type_header) }
    private val operatorHeader: String by lazy { ctx.getString(R.string.export_operator_header) }

    private val scanIdHeader: String by lazy { ctx.getString(R.string.scan_name_header) }

    private val scanDateHeader: String by lazy { ctx.getString(R.string.scan_date_header) }

    private val scanDeviceIdHeader: String by lazy { ctx.getString(R.string.scan_device_id_header) }

    private val specFrameIdHeader: String by lazy { ctx.getString(R.string.spec_frame_id_header) }

    private val specLightSourceHeader: String by lazy { ctx.getString(R.string.spec_light_source_header) }

    private val specCountHeader: String by lazy { ctx.getString(R.string.spec_spectral_count_header) }

    private val specValuesHeader: String by lazy { ctx.getString(R.string.spec_spectral_values_header) }

    private val scanNoteHeader: String by lazy { ctx.getString(R.string.scan_note_header) }

    private val scanModelHeaderString by lazy {
        arrayOf(scanIdHeader, scanDateHeader, scanDeviceIdHeader,
                specFrameIdHeader, specLightSourceHeader, specCountHeader,
                specValuesHeader, scanNoteHeader)
                .joinToString(",")
    }

    private val exportHeaderString by lazy {
        arrayOf(scanDateHeader, scanDeviceIdHeader, scanIdHeader,
                specFrameIdHeader, specLightSourceHeader, specCountHeader,
                specValuesHeader, scanNoteHeader)
                .joinToString(",")
    }

    fun parseInputFile(eid: Long, uri: Uri, viewModel: ExperimentSamplesViewModel) {

        val data = HashMap<Scan, ArrayList<SpectralFrame>>()

        val lines = parseTextFile(uri)

        if (lines.isNotEmpty()) {

            val headers = lines[0].split(",").map { it -> it.replace(" ", "")}

            //ensure the headers size > 0
            if (headers.isNotEmpty()) {

                loadData(eid, headers, lines-lines[0], viewModel)

            }

        }
    }

    /**
     * Headers can have spaces, be in different locations, and have ambiguous case.
     * //TODO show an error message if headers dont match
     */
    private fun loadData(eid: Long,
                         headers: List<String>,
                         lines: List<String>,
                         viewModel: ExperimentSamplesViewModel) {

        val requiredHeaders = scanModelHeaderString.split(",").map { it.trim().toLowerCase(Locale.ROOT) }.toSet()

        val importedHeaders = headers.map { it.trim().toLowerCase(Locale.ROOT) }.toSet()

        if ((requiredHeaders-importedHeaders).isEmpty()) {

            val headerToIndex = importedHeaders
                    .mapIndexed { index, s -> s to index }
                    .toMap()

            lines.forEach { it ->

                val row = it.split(",")

                headerToIndex[scanIdHeader]?.let { nameIndex ->

                    val scanId = row[nameIndex]

                    headerToIndex[scanDateHeader]?.let { dateIndex ->

                        val date = row[dateIndex]

                        headerToIndex[scanDeviceIdHeader]?.let { deviceIndex ->

                            val device = row[deviceIndex]

                            headerToIndex[specFrameIdHeader]?.let { frameIndex ->

                                val frameId = row[frameIndex]

                                headerToIndex[specLightSourceHeader]?.let { lightIndex ->

                                    val lightSource = row[lightIndex]

                                    headerToIndex[specCountHeader]?.let { countIndex ->

                                        val count = row[countIndex]

                                        headerToIndex[specValuesHeader]?.let { valuesIndex ->

                                            val values = row[valuesIndex]

                                            headerToIndex[scanNoteHeader]?.let { noteIndex ->

                                                val note = row[noteIndex]

                                                val scan = Scan(eid, scanId).apply {
                                                    this.date = date
                                                    this.deviceId = device
                                                }

                                                viewModel.viewModelScope.launch {

                                                    viewModel.insertSample(Sample(eid, scanId))

                                                    val sid = viewModel.insertScan(scan).await()

                                                    val specFrame = SpectralFrame(sid, frameId.toInt(), values, lightSource.toInt())

                                                    viewModel.insertFrame(sid, specFrame)

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This function returns True if the required headers exist in the input file
     */
    private fun validateHeaders(headers: List<String>, ensured: List<String>): Boolean {

        return headers.intersect(ensured).size == ensured.size

    }

    /*
    TODO: add wavelength export, might need BrApi for memory reasons
    Function must be called wrapped in a Dispatchers.IO coroutine context
     */
    suspend fun exportCsv(uri: Uri, exports: List<DeviceTypeExport>, convert: Boolean = false) = withContext(Dispatchers.IO) {

        val newline = System.lineSeparator()

        //false warning, this function is run always in Dispatchers.IO
        ctx.contentResolver.openOutputStream(uri)?.let { stream ->

            val writer = stream.buffered().writer()

            val prefixHeaders = "${experimentNameHeader},$scanIdHeader,$scanDateHeader,$deviceTypeHeader,$scanDeviceIdHeader,$operatorHeader,$specLightSourceHeader,"

            if (convert) {

                exports.firstOrNull()?.spectralData?.toWaveMap()?.let { firstWave ->

                    val headers = prefixHeaders + (firstWave.map { it.first }).joinToString(",") + ",$scanNoteHeader"

                    writer.write(headers)

                    writer.write(newline)

                    exports.forEach { export ->

                        val data = arrayOf(
                                export.experiment,
                                export.sample,
                                export.date,
                                export.deviceType,
                                export.deviceId,
                                export.operator,
                                export.lightSource
                        )

                        val wave = export.spectralData.toWaveMap()

                        writer.write("${data.joinToString(",")},${wave.map { it.second }.joinToString(",")},${export.note ?: ""}")

                        writer.write(newline)

                    }
                }

            } else {

                exports.firstOrNull()?.spectralData?.split(" ")?.size?.let { size ->

                    val headers = prefixHeaders + (1..size).joinToString(",") + ",$scanNoteHeader"

                    writer.write(headers)

                    writer.write(newline)

                    exports.forEach { e ->

                        val data = arrayOf(
                                e.experiment,
                                e.sample,
                                e.date,
                                e.deviceType,
                                e.deviceId,
                                e.operator,
                                e.lightSource
                        )

                        val frameData = e.spectralData.replace(" ", ",")

                        writer.write("${data.joinToString(",")},$frameData,${e.note}")

                        writer.write(newline)
                    }
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
    suspend fun exportJsonPosting(uri: Uri, exps: List<Experiment>, samples: List<Sample>, scans: List<Scan>, frames: List<SpectralFrame>, convert: Boolean) = withContext(Dispatchers.IO) {

        val writer = JsonWriter(OutputStreamWriter(ctx.contentResolver.openOutputStream(uri)))

        writer.setIndent(" ")

        try {

            writer.beginObject()

            writer.name("experiments")

            writer.beginArray()

            exps.forEach { experiment ->

                writer.beginObject()

                writer.name("name").value(experiment.name)

                writer.name("date").value(experiment.date)

                writer.name("scans")

                writer.beginArray()

                scans.filter { it.eid == experiment.eid }.forEach { scan ->

                    writer.beginObject()

                    writer.name("id").value(scan.sid)
                    writer.name("sample").value(scan.name)
                    writer.name("deviceId").value(scan.deviceId)
                    writer.name("deviceType").value(scan.deviceType)
                    writer.name("date").value(scan.date)
                    writer.name("operator").value(scan.operator)

                    writer.endObject()

                }

                writer.endArray()

                writer.endObject()

            }

            writer.endArray()

            //writer.endObject()

            writer.name("posting")

            writer.beginObject()

            val posting = Posting()

            frames.forEach { frame ->

                if (convert) {

                    val wave = frame.toWaveMap()

                    wave.forEach { key ->

                        posting[key.first, key.second] = frame.sid

                    }

                } else {

                    frame.spectralValues.split(" ").forEachIndexed { index, s ->

                        posting[index + 1.0, s.toDouble()] = frame.sid

                    }

                }
            }

            posting.keys.forEach { index ->

                writer.name(index.toString())

                writer.beginObject()

                posting[index]?.forEach { post ->

                    writer.name(post.key.toString())

                    writer.value(post.value.valueIterator().asSequence().joinToString(","))

                }

                writer.endObject()

            }

            writer.endObject()

            writer.endObject()

            Log.d("ProspectorPosting", posting.size.toString())

        } catch (e: IllegalStateException) {

            Log.d("ProspectorJsonError", e.message ?: "")

            e.printStackTrace()

        } finally {

            writer.close()

        }

    }

    suspend fun exportJson(uri: Uri, exports: List<DeviceTypeExport>, convert: Boolean) = withContext(Dispatchers.IO) {

        val writer = JsonWriter(OutputStreamWriter(ctx.contentResolver.openOutputStream(uri)))

        writer.setIndent(" ")

        try {

            writer.beginArray()

            exports.groupBy { it.experiment }.forEach { entry ->

                writer.beginObject()

                writer.name(entry.key)

                writer.beginArray()

                entry.value.groupBy { it.sample }.forEach { samples ->

                    writer.beginObject()

                    writer.name(samples.key)

                    writer.beginArray()

                    samples.value.groupBy { it.lightSource }.forEach { source ->

                        writer.beginObject()

                        writer.name(source.key)

                        writer.beginArray()

                        source.value.forEach { scan ->

                            writer.beginObject()

                            writer.name("date").value(scan.date)

                            writer.name("operator").value(scan.operator)

                            writer.name("spectralValues")

                            writer.beginObject()

                            writer.setIndent("")

                            if (convert) {

                                val wave = scan.spectralData.toWaveMap()

                                wave.forEach { key ->

                                    writer.name(key.first.toString()).value(key.second.toString())

                                }

                            } else {

                                val tokens = scan.spectralData.split(" ")

                                tokens.forEachIndexed { index, s ->

                                    writer.name((index+1).toString()).value(s)

                                }

                            }

                            writer.endObject()

                            writer.setIndent(" ")

                            writer.endObject()
                        }

                        writer.endArray()

                        writer.endObject()
                    }

                    writer.endArray()

                    writer.endObject()

                }

                writer.endArray()

                writer.endObject()
            }

            writer.endArray()

        } catch (e: IllegalStateException) {

            Log.d("ProspectorJsonError", e.message)

            e.printStackTrace()

        } finally {

            writer.flush()

            writer.close()

        }

    }

    fun export(uri: Uri, json: JSONArray) {

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                this?.let {

                    write(json.toString(4).toByteArray())

                    close()
                }

            }

        } catch (e: IOException) {

            e.printStackTrace()

        }

        MediaScannerConnection.scanFile(ctx, arrayOf(uri.path), arrayOf("*/*"), null)

    }

    fun export(uri: Uri, scans: Map<Experiment, List<SpectralFrame>>) {

        val newLine: ByteArray = System.getProperty("line.separator")?.toByteArray() ?: "\n".toByteArray()

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                this?.let {

                    write(exportHeaderString.toByteArray())

                    write(newLine)

                    scans.keys.forEachIndexed { index, key ->

                        val frames = scans[key]

                        frames?.forEach { specFrame ->

                            ///write("${key.scanDate},${key.deviceId},${key.sid},${specFrame.frameId},${specFrame.lightSource},${specFrame.count},${specFrame.spectralValues},none".toByteArray())

                            write(newLine)

                        }

                    }

                    close()
                }

            }

        } catch (exception: FileNotFoundException) {

            Log.e("IntFileNotFound", "Chosen uri path was not found: $uri")

        }

        MediaScannerConnection.scanFile(ctx, arrayOf(uri.path), arrayOf("*/*"), null)

    }


    @WorkerThread
    fun getFilePath(context: Context, uri: Uri): String? = context.run {
        when {

            Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ->
                getDataColumn(uri, null, null)

            else -> getPathKitkatPlus(uri)
        }
    }

    private fun Context.getPathKitkatPlus(uri: Uri): String? {
        when {
            DocumentsContract.isDocumentUri(applicationContext, uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                when {
                    uri.isExternalStorageDocument -> {
                        val parts = docId.split(":")
                        if ("primary".equals(parts[0], true)) {
                            return "${Environment.getExternalStorageDirectory()}/${parts[1]}"
                        }
                    }
                    uri.isDownloadsDocument -> {
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                docId.toLong()
                        )
                        return getDataColumn(contentUri, null, null)
                    }
                    uri.isMediaDocument -> {
                        val parts = docId.split(":")
                        val contentUri = when (parts[0].toLowerCase(Locale.ROOT)) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> return null
                        }
                        return getDataColumn(contentUri, "_id=?", arrayOf(parts[1]))
                    }
                }
            }
            "content".equals(uri.scheme, true) -> {
                return if (uri.isGooglePhotosUri) {
                    uri.lastPathSegment
                } else {
                    getDataColumn(uri, null, null)
                }
            }
            "file".equals(uri.scheme, true) -> {
                return uri.path
            }
        }
        return null
    }

    private fun Context.getDataColumn(uri: Uri, selection: String?, args: Array<String>?): String? {
        contentResolver?.query(uri, arrayOf("_data"), selection, args, null)?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow("_data"))
            }
        }
        return null
    }

    private val Uri.isExternalStorageDocument: Boolean
        get() = authority == "com.android.externalstorage.documents"

    private val Uri.isDownloadsDocument: Boolean
        get() = authority == "com.android.providers.downloads.documents"

    private val Uri.isMediaDocument: Boolean
        get() = authority == "com.android.providers.media.documents"

    private val Uri.isGooglePhotosUri: Boolean
        get() = authority == "com.google.android.apps.photos.content"

    private fun parseTextFile(it: Uri): List<String> {

        var ret = ArrayList<String>()

        try {

            val stream = ctx.contentResolver.openInputStream(it)

            stream?.let {

                val reader = BufferedReader(InputStreamReader(it))

                ret = ArrayList(reader.readLines())
            }
        } catch (fo: FileNotFoundException) {
            fo.printStackTrace()
        } catch (io: IOException) {
            io.printStackTrace()
        }

        return ret
    }
}