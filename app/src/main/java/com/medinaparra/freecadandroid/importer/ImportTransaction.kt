package com.medinaparra.freecadandroid.importer

import com.medinaparra.freecadandroid.nativebridge.FreeCadNative

class ImportTransaction(
    val tempDocId: Long,
    val tempFile: TemporaryCadFile? = null
) {
    private var isCompleted = false

    fun commit(onCommit: (Long) -> Unit) {
        if (isCompleted) return
        isCompleted = true
        onCommit(tempDocId)
        tempFile?.delete()
    }

    fun rollback() {
        if (isCompleted) return
        isCompleted = true
        if (tempDocId != 0L) {
            FreeCadNative.closeDocument(tempDocId)
        }
        tempFile?.delete()
    }
}
