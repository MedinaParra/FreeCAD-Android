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
            val extractedFiles = mutableListOf<File>()
            try {
                context.contentResolver.openInputStream(uri)?.use { rawInputStream ->
                    val zipInputStream = ZipInputStream(rawInputStream)
                    var entry = zipInputStream.nextEntry
                    var entryIndex = 0
                    var totalSize = 0L
                    while (entry != null) {
                        val entryNameLower = entry.name.lowercase()
                        if (entryNameLower.endsWith(".brp") || entryNameLower.endsWith(".brep")) {
                            val tempFile = File.createTempFile("fcstd_shape_${entryIndex}_", ".brp", context.cacheDir)
                            extractedFiles.add(tempFile)
                            
                            tempFile.outputStream().use { output ->
                                val buffer = ByteArray(4096)
                                var len = zipInputStream.read(buffer)
                                var entrySize = 0L
                                while (len > 0) {
                                    entrySize += len
                                    totalSize += len
                                    if (totalSize > 100 * 1024 * 1024) {
                                        throw SecurityException("Excedido el límite máximo de descompresión (100MB). Posible ZIP bomb detectada.")
                                    }
                                    if (entry.size > 0 && entrySize > entry.size * 100) {
                                        throw SecurityException("Relación de compresión excesiva. Posible ZIP bomb detectada.")
                                    }
                                    output.write(buffer, 0, len)
                                    len = zipInputStream.read(buffer)
                                }
                            }
                            entryIndex++
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                        
                        if (entryIndex > 200) {
                            throw SecurityException("Excedido el límite máximo de archivos BRep extraídos (200). Posible ZIP bomb.")
                        }
                    }
                }

                if (extractedFiles.isEmpty()) {
                    onStateChanged(ImportState.Error("NO_BREP_GEOMETRY", "No se encontró geometría BRep (.brp/.brep) válida dentro de este archivo FCStd."))
                    return@withContext null
                }

                onStateChanged(ImportState.Importing)
                // Semicolon-separated path string for all extracted shapes
                val semicolonPaths = extractedFiles.joinToString(";") { it.absolutePath }
                
                // Perform provisional native import
                val result = FreeCadNative.importBrep(fileName, semicolonPaths)
                
                if (result != null && result.success) {
                    onStateChanged(ImportState.Triangulating)
                    val mesh = FreeCadNative.getSceneMesh(result.documentId)
                    if (mesh != null && mesh.vertexBuffer != null && mesh.vertexCount > 0) {
                        // Success! Return a completed transaction
                        onStateChanged(ImportState.Success(
                            documentId = result.documentId,
                            objectCount = result.objects.size,
                            vertexCount = result.vertexCount,
                            triangleCount = result.triangleCount
                        ))
                        return@withContext ImportTransaction(result.documentId, result.objects)
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
            } finally {
                // Clean up extracted files immediately under all conditions
                extractedFiles.forEach { if (it.exists()) { it.delete() } }
            }

        } else if (lowerName.endsWith(".step") || lowerName.endsWith(".stp")) {
            // Process STEP / STP
            var tempFile: File? = null
            try {
                tempFile = File.createTempFile("step_import_", ".step", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile!!.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val tempCadFile = TemporaryCadFile(tempFile!!)
                onStateChanged(ImportState.Importing)

                val result = FreeCadNative.importStep(fileName, tempFile!!.absolutePath)
                tempCadFile.delete() // Clean up local temporary step file

                if (result != null && result.success) {
                    onStateChanged(ImportState.Triangulating)
                    val mesh = FreeCadNative.getSceneMesh(result.documentId)
                    if (mesh != null && mesh.vertexBuffer != null && mesh.vertexCount > 0) {
                        onStateChanged(ImportState.Success(
                            documentId = result.documentId,
                            objectCount = result.objects.size,
                            vertexCount = result.vertexCount,
                            triangleCount = result.triangleCount
                        ))
                        return@withContext ImportTransaction(result.documentId, result.objects)
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
            } finally {
                tempFile?.let { if (it.exists()) { it.delete() } }
            }
        } else {
            onStateChanged(ImportState.Error("UNSUPPORTED_FORMAT", "Formato de archivo no soportado. Seleccione un archivo .STEP, .STP o .FCStd."))
            return@withContext null
        }
    }
}
