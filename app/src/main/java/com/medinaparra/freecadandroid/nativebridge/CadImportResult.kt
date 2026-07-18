package com.medinaparra.freecadandroid.nativebridge

import androidx.annotation.Keep

@Keep
data class ImportedObjectInfo(
    val objectId: Long,
    val name: String,
    val type: String,
    val visible: Boolean,
    val indexOffset: Int,
    val indexCount: Int
)

@Keep
data class CadImportResult(
    val success: Boolean,
    val documentId: Long,
    val objects: Array<ImportedObjectInfo>,
    val vertexCount: Int,
    val triangleCount: Int,
    val errorCode: String?,
    val errorMessage: String?
)
