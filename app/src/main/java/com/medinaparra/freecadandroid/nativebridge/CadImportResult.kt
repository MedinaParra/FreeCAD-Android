package com.medinaparra.freecadandroid.nativebridge

data class CadImportResult(
    val success: Boolean,
    val documentId: Long,
    val objectCount: Int,
    val vertexCount: Int,
    val triangleCount: Int,
    val errorCode: String?,
    val errorMessage: String?
)
