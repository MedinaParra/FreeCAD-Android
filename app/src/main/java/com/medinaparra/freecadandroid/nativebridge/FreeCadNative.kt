package com.medinaparra.freecadandroid.nativebridge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.medinaparra.freecadandroid.nativebridge.MacroExecutionResult

object FreeCadNative {
    var isLibraryLoaded = false
        private set
    
    private var lastNativeError: String? = null

    init {
        try {
            System.loadLibrary("freecad_native")
            isLibraryLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            lastNativeError = "No se pudo cargar freecad_native. Verifique las bibliotecas ABI y sus dependencias: " + e.localizedMessage
            System.err.println("ERROR: " + lastNativeError)
        }
    }

    fun isNativeAvailable(): Boolean = isLibraryLoaded

    fun getLoadError(): String? = lastNativeError

    fun initialize(nativePath: String): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeInitialize(nativePath)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun shutdown() {
        if (!isLibraryLoaded) return
        try {
            nativeShutdown()
        } catch (e: UnsatisfiedLinkError) {}
    }

    fun createDocument(name: String): Long {
        if (!isLibraryLoaded) return 0L
        return try {
            nativeCreateDocument(name)
        } catch (e: UnsatisfiedLinkError) {
            0L
        }
    }

    fun closeDocument(documentId: Long): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeCloseDocument(documentId)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun createBox(documentId: Long, name: String, length: Double, width: Double, height: Double): Long {
        if (!isLibraryLoaded) return 0L
        return try {
            nativeCreateBox(documentId, name, length, width, height)
        } catch (e: UnsatisfiedLinkError) {
            0L
        }
    }

    fun createCylinder(documentId: Long, name: String, radius: Double, height: Double): Long {
        if (!isLibraryLoaded) return 0L
        return try {
            nativeCreateCylinder(documentId, name, radius, height)
        } catch (e: UnsatisfiedLinkError) {
            0L
        }
    }

    fun createSphere(documentId: Long, name: String, radius: Double): Long {
        if (!isLibraryLoaded) return 0L
        return try {
            nativeCreateSphere(documentId, name, radius)
        } catch (e: UnsatisfiedLinkError) {
            0L
        }
    }

    fun createCone(documentId: Long, name: String, radius1: Double, radius2: Double, height: Double): Long {
        if (!isLibraryLoaded) return 0L
        return try {
            nativeCreateCone(documentId, name, radius1, radius2, height)
        } catch (e: UnsatisfiedLinkError) {
            0L
        }
    }

    fun translateObject(documentId: Long, objectId: Long, x: Double, y: Double, z: Double): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeTranslateObject(documentId, objectId, x, y, z)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun updateObjectDimensions(documentId: Long, objectId: Long, d1: Double, d2: Double, d3: Double): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeUpdateObjectDimensions(documentId, objectId, d1, d2, d3)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun deleteObject(documentId: Long, objectId: Long): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeDeleteObject(documentId, objectId)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun setObjectVisibility(documentId: Long, objectId: Long, visible: Boolean): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeSetObjectVisibility(documentId, objectId, visible)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun recompute(documentId: Long): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeRecompute(documentId)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun importStep(documentName: String, filePath: String): CadImportResult {
        if (!isLibraryLoaded) {
            return CadImportResult(
                success = false,
                documentId = 0,
                objects = emptyArray(),
                vertexCount = 0,
                triangleCount = 0,
                errorCode = "NATIVE_LIB_NOT_LOADED",
                errorMessage = "No se pudo cargar freecad_native."
            )
        }
        return try {
            nativeImportStep(documentName, filePath)
        } catch (e: UnsatisfiedLinkError) {
            CadImportResult(
                success = false,
                documentId = 0,
                objects = emptyArray(),
                vertexCount = 0,
                triangleCount = 0,
                errorCode = "UNSATISFIED_LINK",
                errorMessage = "Función nativa no enlazada: " + e.localizedMessage
            )
        }
    }

    fun importBrep(documentName: String, filePath: String): CadImportResult {
        if (!isLibraryLoaded) {
            return CadImportResult(
                success = false,
                documentId = 0,
                objects = emptyArray(),
                vertexCount = 0,
                triangleCount = 0,
                errorCode = "NATIVE_LIB_NOT_LOADED",
                errorMessage = "No se pudo cargar freecad_native."
            )
        }
        return try {
            nativeImportBrep(documentName, filePath)
        } catch (e: UnsatisfiedLinkError) {
            CadImportResult(
                success = false,
                documentId = 0,
                objects = emptyArray(),
                vertexCount = 0,
                triangleCount = 0,
                errorCode = "UNSATISFIED_LINK",
                errorMessage = "Función nativa no enlazada: " + e.localizedMessage
            )
        }
    }

    fun getSceneMesh(documentId: Long): NativeSceneMesh? {
        if (!isLibraryLoaded) return null
        return try {
            nativeGetSceneMesh(documentId)
        } catch (e: UnsatisfiedLinkError) {
            null
        }
    }

    fun executePythonMacro(documentId: Long, code: String, timeoutMs: Long): MacroExecutionResult? {
        if (!isLibraryLoaded) {
            return MacroExecutionResult(false, "", "No se pudo cargar la biblioteca nativa freecad_native.", 0L)
        }
        return try {
            nativeExecutePythonMacro(documentId, code, timeoutMs)
        } catch (e: UnsatisfiedLinkError) {
            MacroExecutionResult(false, "", "Runtime Python no disponible en esta compilación.", 0L)
        }
    }

    fun getLastNativeError(): String? {
        if (!isLibraryLoaded) return lastNativeError
        return try {
            nativeGetLastNativeError()
        } catch (e: UnsatisfiedLinkError) {
            lastNativeError
        }
    }

    fun getNativeCapabilities(): NativeCapabilities {
        if (!isLibraryLoaded) {
            return NativeCapabilities(
                nativeLibraryLoaded = false,
                occtAvailable = false,
                freeCadBaseAvailable = false,
                freeCadAppAvailable = false,
                partModuleAvailable = false,
                pythonAvailable = false,
                stepImportAvailable = false,
                fcStdBrepExtractionAvailable = false,
                fcStdCoreAvailable = false
            )
        }
        return try {
            nativeGetNativeCapabilities()
        } catch (e: UnsatisfiedLinkError) {
            NativeCapabilities(
                nativeLibraryLoaded = true,
                occtAvailable = false,
                freeCadBaseAvailable = false,
                freeCadAppAvailable = false,
                partModuleAvailable = false,
                pythonAvailable = false,
                stepImportAvailable = false,
                fcStdBrepExtractionAvailable = false,
                fcStdCoreAvailable = false
            )
        }
    }

    // Native JNI mappings
    @JvmStatic private external fun nativeInitialize(nativePath: String): Boolean
    @JvmStatic private external fun nativeShutdown()
    @JvmStatic private external fun nativeCreateDocument(name: String): Long
    @JvmStatic private external fun nativeCloseDocument(documentId: Long): Boolean
    @JvmStatic private external fun nativeCreateBox(documentId: Long, name: String, length: Double, width: Double, height: Double): Long
    @JvmStatic private external fun nativeCreateCylinder(documentId: Long, name: String, radius: Double, height: Double): Long
    @JvmStatic private external fun nativeCreateSphere(documentId: Long, name: String, radius: Double): Long
    @JvmStatic private external fun nativeCreateCone(documentId: Long, name: String, radius1: Double, radius2: Double, height: Double): Long
    @JvmStatic private external fun nativeTranslateObject(documentId: Long, objectId: Long, x: Double, y: Double, z: Double): Boolean
    @JvmStatic private external fun nativeUpdateObjectDimensions(documentId: Long, objectId: Long, d1: Double, d2: Double, d3: Double): Boolean
    @JvmStatic private external fun nativeDeleteObject(documentId: Long, objectId: Long): Boolean
    @JvmStatic private external fun nativeSetObjectVisibility(documentId: Long, objectId: Long, visible: Boolean): Boolean
    @JvmStatic private external fun nativeRecompute(documentId: Long): Boolean
    @JvmStatic private external fun nativeImportStep(documentName: String, filePath: String): CadImportResult
    @JvmStatic private external fun nativeImportBrep(documentName: String, filePath: String): CadImportResult
    @JvmStatic private external fun nativeGetSceneMesh(documentId: Long): NativeSceneMesh?
    @JvmStatic private external fun nativeExecutePythonMacro(documentId: Long, code: String, timeoutMs: Long): MacroExecutionResult?
    @JvmStatic private external fun nativeGetLastNativeError(): String?
    @JvmStatic private external fun nativeGetNativeCapabilities(): NativeCapabilities
}
