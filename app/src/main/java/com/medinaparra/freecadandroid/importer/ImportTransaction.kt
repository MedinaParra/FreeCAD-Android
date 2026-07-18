package com.medinaparra.freecadandroid.importer

import com.medinaparra.freecadandroid.nativebridge.FreeCadNative
import com.medinaparra.freecadandroid.nativebridge.ImportedObjectInfo

class ImportTransaction(
    val tempDocId: Long,
    val importedObjects: Array<ImportedObjectInfo> = emptyArray(),
    val tempFile: TemporaryCadFile? = null
) {
    private var isCompleted = false

    fun commit(onCommit: (Long, Array<ImportedObjectInfo>) -> Unit) {
        if (isCompleted) return
        isCompleted = true
        onCommit(tempDocId, importedObjects)
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
