package com.medinaparra.freecadandroid.importer

sealed interface ImportState {
    data object Idle : ImportState
    data class Copying(val progress: Float?) : ImportState
    data object Importing : ImportState
    data object Triangulating : ImportState

    data class Success(
        val documentId: Long,
        val objectCount: Int,
        val vertexCount: Int,
        val triangleCount: Int
    ) : ImportState

    data class Error(
        val code: String,
        val message: String
    ) : ImportState
}
