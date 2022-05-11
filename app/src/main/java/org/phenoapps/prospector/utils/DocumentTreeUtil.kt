package org.phenoapps.prospector.utils

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.FileNotFoundException
import java.io.OutputStream

class DocumentTreeUtil {

    /**
     * Static functions to be used to handle exports.
     * These functions will attempt to create these directories if they do not exist.
     */
    companion object {

        const val TAG = "DocumentTreeUtil"

        /**
         * Copies one document file to another.
         */
        fun copy(context: Context?, from: DocumentFile?, to: DocumentFile?) {
            context?.let { ctx ->

                try {

                    to?.let { toFile ->
                        from?.let { fromFile ->
                            with (ctx.contentResolver) {
                                openOutputStream(toFile.uri)?.use { output ->
                                    openInputStream(fromFile.uri)?.use { input ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                    }

                } catch (f: FileNotFoundException) {

                    f.printStackTrace()

                }
            }
        }

        /**
         * Returns a document file based on the directory and a filename.
         */
        fun getFile(context: Context?, dir: Int, fileName: String): DocumentFile? {

            context?.let { ctx ->

                getDirectory(ctx, dir)?.let { dir ->

                    dir.findFile(fileName.replace(":", "_"))?.let { file ->

                        return file
                    }
                }
            }

            return null
        }

        /**
         * Returns an output stream for the given file.
         */
        fun getFileOutputStream(context: Context?, dir: Int, fileName: String): OutputStream? {

            try {

                getFile(context, dir, fileName)?.let { file ->

                    return context?.contentResolver?.openOutputStream(file.uri)
                }

            } catch (e: FileNotFoundException) {

                e.printStackTrace()

            }

            return null
        }

        /**
         * Returns one of the main sub directories s.a string id's shown in res/values/directories.xml
         */
        fun getDirectory(context: Context?, id: Int): DocumentFile? {

            context?.let { ctx ->

                val directoryName = ctx.getString(id)

                return createDir(ctx, directoryName)
            }

            return null
        }

        /**
         * Finds the persisted uri and creates the basic coordinate file structure if it doesn't exist.
         */
        private fun createDir(ctx: Context, parent: String): DocumentFile? {
            val persists = ctx.contentResolver.persistedUriPermissions
            if (persists.isNotEmpty()) {
                val uri = persists.first().uri
                DocumentFile.fromTreeUri(ctx, uri)?.let { tree ->
                    var exportDir = tree.findFile(parent)
                    if (exportDir == null) {
                        exportDir = tree.createDirectory(parent)
                    }

                    return exportDir
                }
            }

            return null
        }
    }
}