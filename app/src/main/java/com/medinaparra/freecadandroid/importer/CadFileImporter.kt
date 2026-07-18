package com.medinaparra.freecadandroid.importer

import android.content.Context
import android.net.Uri
import com.medinaparra.freecadandroid.nativebridge.FreeCadNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

object CadFileImporter {

    suspend fun importFile(
        context: Context,
        uri: Uri,
        fileName: String,
        onStateChanged: (ImportState) -> Unit
    ): ImportTransaction? = withContext(Dispatchers.IO) {
        val lowerName = fileName.lowercase()
        onStateChanged(ImportState.Copying(0.0f))

        if (lowerName.endsWith(".fcstd") || lowerName.endsWith(".fcstd1")) {
            // Process FCStd (honest BRep extraction)
            try {
                val extractedFiles = mutableListOf<File>()
                context.contentResolver.openInputStream(uri)?.use { rawInputStream ->
                    val zipInputStream = ZipInputStream(rawInputStream)
                    var entry = zipInputStream.nextEntry
                    var entryIndex = 0
                    while (entry != null) {
                        if (entry.name.endsWith(".brp") || entry.name.endsWith(".brep")) {
                            val tempFile = File.createTempFile("fcstd_shape_${entryIndex}_", ".brp", context.cacheDir)
                            tempFile.outputStream().use { output ->
                                zipInputStream.copyTo(output)
                            }
                            extractedFiles.add(tempFile)
                            entryIndex++
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }

                if (extractedFiles.isEmpty()) {
                    onStateChanged(ImportState.Error("NO_BREP_GEOMETRY", "No se encontró geometría BRep (.brp) válida dentro de este archivo FCStd."))
                    return@withContext null
                }

                onStateChanged(ImportState.Importing)
                // Semicolon-separated path string for all extracted shapes
                val semicolonPaths = extractedFiles.joinToString(";") { it.absolutePath }
                
                // Perform provisional native import
                val result = FreeCadNative.importBrep(fileName, semicolonPaths)
                
                // Clean up extracted files immediately to prevent disk bloating
                extractedFiles.forEach { if (it.exists()) it.delete() }

                if (result != null && result.success) {
                    onStateChanged(ImportState.Triangulating)
                    val mesh = FreeCadNative.getSceneMesh(result.documentId)
                    if (mesh != null && mesh.vertexBuffer != null && mesh.vertexCount > 0) {
                        // Success! Return a completed transaction
                        onStateChanged(ImportState.Success(
                            documentId = result.documentId,
                            objectCount = result.objectCount,
                            vertexCount = result.vertexCount,
                            triangleCount = result.triangleCount
                        ))
                        return@withContext ImportTransaction(result.documentId)
                    } else {
                        // Failed validation (no actual triangles or vertices)
                        FreeCadNative.closeDocument(result.documentId)
                        onStateChanged(ImportState.Error("EMPTY_GEOMETRY", "El archivo FCStd no generó ninguna geometría o malla 3D."))
                        return@withContext null
                    }
                } else {
                    val code = result?.errorCode ?: "IMPORT_FAILED"
                    val msg = result?.errorMessage ?: FreeCadNative.getLastNativeError() ?: "Error de lectura nativa OpenCASCADE."
                    onStateChanged(ImportState.Error(code, msg))
                    return@withContext null
                }

            } catch (e: Exception) {
                onStateChanged(ImportState.Error("EXCEPTION", e.localizedMessage ?: "Excepción al procesar ZIP FCStd."))
                return@withContext null
            }

        } else if (lowerName.endsWith(".step") || lowerName.endsWith(".stp")) {
            // Process STEP / STP
            try {
                val tempFile = File.createTempFile("step_import_", ".step", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val tempCadFile = TemporaryCadFile(tempFile)
                onStateChanged(ImportState.Importing)

                val result = FreeCadNative.importStep(fileName, tempFile.absolutePath)
                tempCadFile.delete() // Clean up local temporary step file

                if (result != null && result.success) {
                    onStateChanged(ImportState.Triangulating)
                    val mesh = FreeCadNative.getSceneMesh(result.documentId)
                    if (mesh != null && mesh.vertexBuffer != null && mesh.vertexCount > 0) {
                        onStateChanged(ImportState.Success(
                            documentId = result.documentId,
                            objectCount = result.objectCount,
                            vertexCount = result.vertexCount,
                            triangleCount = result.triangleCount
                        ))
                        return@withContext ImportTransaction(result.documentId)
                    } else {
                        FreeCadNative.closeDocument(result.documentId)
                        onStateChanged(ImportState.Error("EMPTY_GEOMETRY", "El archivo STEP no generó ninguna geometría o malla 3D."))
                        return@withContext null
                    }
                } else {
                    val code = result?.errorCode ?: "IMPORT_FAILED"
                    val msg = result?.errorMessage ?: FreeCadNative.getLastNativeError() ?: "Error de lectura nativa OpenCASCADE."
                    onStateChanged(ImportState.Error(code, msg))
                    return@withContext null
                }

            } catch (e: Exception) {
                onStateChanged(ImportState.Error("EXCEPTION", e.localizedMessage ?: "Excepción al copiar archivo STEP."))
                return@withContext null
            }
        } else {
            onStateChanged(ImportState.Error("UNSUPPORTED_FORMAT", "Formato de archivo no soportado. Seleccione un archivo .STEP, .STP o .FCStd."))
            return@withContext null
        }
    }
}
