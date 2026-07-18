package com.medinaparra.freecadandroid.nativebridge

import androidx.annotation.Keep

@Keep
data class NativeCapabilities(
    val nativeLibraryLoaded: Boolean,
    val occtAvailable: Boolean,
    val freeCadBaseAvailable: Boolean,
    val freeCadAppAvailable: Boolean,
    val partModuleAvailable: Boolean,
    val pythonAvailable: Boolean,
    val stepImportAvailable: Boolean,
    val fcStdImportAvailable: Boolean
)
