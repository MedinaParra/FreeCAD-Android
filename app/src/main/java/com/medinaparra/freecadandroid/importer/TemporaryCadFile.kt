package com.medinaparra.freecadandroid.importer

import android.content.Context
import android.net.Uri
import java.io.File

class TemporaryCadFile(
    val file: File
) {
    fun delete() {
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        fun createFromUri(context: Context, uri: Uri, suffix: String): TemporaryCadFile? {
            return try {
                val tempFile = File.createTempFile("cad_import_", suffix, context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                TemporaryCadFile(tempFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
