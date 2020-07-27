package org.phenoapps.prospector.utils

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.ExperimentScans
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class FileUtil(private val ctx: Context) {

    /**
     * Cross table export file header fields.
     */

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
                specValuesHeader)
                .joinToString(",")
    }

    fun parseInputFile(eid: Long, uri: Uri): Map<Scan, List<SpectralFrame>> {

        val data = HashMap<Scan, ArrayList<SpectralFrame>>()

        val lines = parseTextFile(uri)

        if (lines.isNotEmpty()) {

            val headers = lines[0].split(",").map { it -> it.replace(" ", "")}

            //ensure the headers size > 0
            if (headers.isNotEmpty()) {

                loadData(eid, headers, lines-lines[0], data)

            }

        }

        return data
    }

    /**
     * Headers can have spaces, be in different locations, and have ambiguous case.
     * //TODO show an error message if headers dont match
     */
    private fun loadData(eid: Long,
                         headers: List<String>,
                         lines: List<String>,
                         data: MutableMap<Scan, ArrayList<SpectralFrame>>) {

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
                                                    this.note = note
                                                }

                                                val specFrame = SpectralFrame(scanId, frameId.toInt(), count.toInt(), values, lightSource.toInt())

                                                if (scan !in data.keys) {

                                                    data[scan] = ArrayList()

                                                }

                                                data[scan]?.add(specFrame)

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

    fun export(uri: Uri, scans: Map<ExperimentScans, List<SpectralFrame>>) {

        val newLine: ByteArray = System.getProperty("line.separator")?.toByteArray() ?: "\n".toByteArray()

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                this?.let {

                    write(exportHeaderString.toByteArray())

                    write(newLine)

                    scans.keys.forEachIndexed { index, key ->

                        val frames = scans[key]

                        frames?.forEach { specFrame ->

                            write("${key.scanDate},${key.deviceId},${key.sid},${specFrame.frameId},${specFrame.lightSource},${specFrame.count},${specFrame.spectralValues}".toByteArray())

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