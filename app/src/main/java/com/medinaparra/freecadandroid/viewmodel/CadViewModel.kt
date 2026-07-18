package com.medinaparra.freecadandroid.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medinaparra.freecadandroid.importer.CadFileImporter
import com.medinaparra.freecadandroid.importer.ImportState
import com.medinaparra.freecadandroid.ui.CadObjectState
import com.medinaparra.freecadandroid.nativebridge.FreeCadNative
import com.medinaparra.freecadandroid.nativebridge.NativeCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CadViewModel : ViewModel() {

    private val _activeDocId = MutableStateFlow(0L)
    val activeDocId: StateFlow<Long> = _activeDocId.asStateFlow()

    private val _activeDocName = MutableStateFlow("Modelo_Activo")
    val activeDocName: StateFlow<String> = _activeDocName.asStateFlow()

    private val _objectsList = MutableStateFlow<List<CadObjectState>>(emptyList())
    val objectsList: StateFlow<List<CadObjectState>> = _objectsList.asStateFlow()

    private val _selectedObjectId = MutableStateFlow<Long?>(null)
    val selectedObjectId: StateFlow<Long?> = _selectedObjectId.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _pythonConsoleOutput = MutableStateFlow("Backend CAD: comprobando capacidades nativas...\n")
    val pythonConsoleOutput: StateFlow<String> = _pythonConsoleOutput.asStateFlow()

    private val _nativeCapabilities = MutableStateFlow(
        NativeCapabilities(
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
    )
    val nativeCapabilities: StateFlow<NativeCapabilities> = _nativeCapabilities.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        // Load initial capabilities from native layer
        viewModelScope.launch {
            val caps = FreeCadNative.getNativeCapabilities()
            _nativeCapabilities.value = caps
            
            // Log status of the backend to the user console with real honest information
            var msg = "Biblioteca JNI: " + (if (caps.nativeLibraryLoaded) "CARGADA" else "NO CARGADA") + "\n"
            msg += "OpenCASCADE: " + (if (caps.occtAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            msg += "FreeCAD Base: " + (if (caps.freeCadBaseAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            msg += "FreeCAD App: " + (if (caps.freeCadAppAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            msg += "Part: " + (if (caps.partModuleAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            msg += "Python: " + (if (caps.pythonAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            msg += "STEP: " + (if (caps.stepImportAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            msg += "FCStd completo: " + (if (caps.fcStdCoreAvailable) "DISPONIBLE" else "NO DISPONIBLE") + "\n"
            _pythonConsoleOutput.value = msg
            
            // Create default active document
            val defaultId = FreeCadNative.createDocument("Modelo_Activo")
            if (defaultId != 0L) {
                _activeDocId.value = defaultId
            }
        }
    }

    fun setTheme(dark: Boolean) {
        _isDarkTheme.value = dark
    }

    fun selectObject(id: Long?) {
        _selectedObjectId.value = id
    }

    fun clearConsole() {
        _pythonConsoleOutput.value = ""
    }

    fun appendConsoleLog(text: String) {
        _pythonConsoleOutput.value += text
    }

    fun importCadFile(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Idle
            val transaction = CadFileImporter.importFile(context, uri, fileName) { state ->
                _importState.value = state
                when (state) {
                    is ImportState.Copying -> {
                        _pythonConsoleOutput.value += ">>> Copiando archivo: $fileName...\n"
                    }
                    is ImportState.Importing -> {
                        _pythonConsoleOutput.value += ">>> Procesando importación con OpenCASCADE...\n"
                    }
                    is ImportState.Triangulating -> {
                        _pythonConsoleOutput.value += ">>> Generando malla de triángulos 3D (Triangulación OCCT)...\n"
                    }
                    is ImportState.Success -> {
                        _pythonConsoleOutput.value += ">>> ¡Importación completada con éxito!\n"
                        _pythonConsoleOutput.value += ">>> Sólidos: ${state.objectCount}, Vértices: ${state.vertexCount}, Triángulos: ${state.triangleCount}\n"
                        if (fileName.lowercase().endsWith(".fcstd") || fileName.lowercase().endsWith(".fcstd1")) {
                            _pythonConsoleOutput.value += ">>> FCStd abierto parcialmente mediante las shapes BRep internas.\n>>> No se reconstruyó el historial paramétrico.\n>>> Los placements y dependencias pueden no estar completos.\n"
                        }
                    }
                    is ImportState.Error -> {
                        _pythonConsoleOutput.value += ">>> ERROR EN IMPORTACIÓN [${state.code}]: ${state.message}\n"
                    }
                    else -> {}
                }
            }

            if (transaction != null) {
                // Transactional commit!
                val oldDocId = _activeDocId.value
                transaction.commit { tempDocId, importedObjects ->
                    _activeDocId.value = tempDocId
                    _activeDocName.value = fileName
                    
                    // Populate objects tree with real native-imported objects
                    val objects = importedObjects.map { obj ->
                        CadObjectState(
                            id = obj.objectId,
                            name = obj.name,
                            type = obj.type,
                            dim1 = 0f,
                            dim2 = 0f,
                            dim3 = 0f,
                            isVisible = obj.visible
                        )
                    }
                    _objectsList.value = objects
                    _selectedObjectId.value = objects.firstOrNull()?.id
                }

                // Close previous document after switching safely
                if (oldDocId != 0L && oldDocId != transaction.tempDocId) {
                    FreeCadNative.closeDocument(oldDocId)
                }
            } else {
                _pythonConsoleOutput.value += ">>> Importación abortada de forma segura. El documento actual no ha sufrido cambios.\n"
            }
        }
    }

    fun deleteActiveObject() {
        val docId = _activeDocId.value
        val objId = _selectedObjectId.value
        if (docId != 0L && objId != null) {
            deleteObject(objId)
        }
    }

    fun deleteObject(objectId: Long) {
        val docId = _activeDocId.value
        if (docId != 0L) {
            FreeCadNative.deleteObject(docId, objectId)
            _objectsList.value = _objectsList.value.filter { it.id != objectId }
            if (_selectedObjectId.value == objectId) {
                _selectedObjectId.value = _objectsList.value.firstOrNull()?.id
            }
            FreeCadNative.recompute(docId)
        }
    }

    fun addNewBox() {
        val docId = _activeDocId.value
        if (docId == 0L) return
        val count = _objectsList.value.count { it.type == "BOX" } + 1
        val name = "Caja_$count"
        val boxId = FreeCadNative.createBox(docId, name, 40.0, 40.0, 40.0)
        if (boxId != 0L) {
            val newObj = CadObjectState(boxId, name, "BOX")
            _objectsList.value = _objectsList.value + newObj
            _selectedObjectId.value = boxId
            FreeCadNative.recompute(docId)
        }
    }

    fun addNewCylinder() {
        val docId = _activeDocId.value
        if (docId == 0L) return
        val count = _objectsList.value.count { it.type == "CYLINDER" } + 1
        val name = "Cilindro_$count"
        val cylId = FreeCadNative.createCylinder(docId, name, 20.0, 50.0)
        if (cylId != 0L) {
            val newObj = CadObjectState(cylId, name, "CYLINDER", dim1 = 20f, dim2 = 50f)
            _objectsList.value = _objectsList.value + newObj
            _selectedObjectId.value = cylId
            FreeCadNative.recompute(docId)
        }
    }

    fun addNewSphere() {
        val docId = _activeDocId.value
        if (docId == 0L) return
        val count = _objectsList.value.count { it.type == "SPHERE" } + 1
        val name = "Esfera_$count"
        val sphereId = FreeCadNative.createSphere(docId, name, 20.0)
        if (sphereId != 0L) {
            val newObj = CadObjectState(sphereId, name, "SPHERE", dim1 = 20f)
            _objectsList.value = _objectsList.value + newObj
            _selectedObjectId.value = sphereId
            FreeCadNative.recompute(docId)
        }
    }

    fun addNewCone() {
        val docId = _activeDocId.value
        if (docId == 0L) return
        val count = _objectsList.value.count { it.type == "CONE" } + 1
        val name = "Cono_$count"
        val coneId = FreeCadNative.createCone(docId, name, 20.0, 5.0, 50.0)
        if (coneId != 0L) {
            val newObj = CadObjectState(coneId, name, "CONE", dim1 = 20f, dim2 = 5f, dim3 = 50f)
            _objectsList.value = _objectsList.value + newObj
            _selectedObjectId.value = coneId
            FreeCadNative.recompute(docId)
        }
    }

    fun updateObjectPosition(objectId: Long, tx: Float, ty: Float, tz: Float) {
        val docId = _activeDocId.value
        if (docId == 0L) return
        FreeCadNative.translateObject(docId, objectId, tx.toDouble(), ty.toDouble(), tz.toDouble())
        _objectsList.value = _objectsList.value.map {
            if (it.id == objectId) it.copy(tx = tx, ty = ty, tz = tz) else it
        }
        FreeCadNative.recompute(docId)
    }

    fun updateObjectDimensions(objectId: Long, d1: Float, d2: Float, d3: Float) {
        val docId = _activeDocId.value
        if (docId == 0L) return
        FreeCadNative.updateObjectDimensions(docId, objectId, d1.toDouble(), d2.toDouble(), d3.toDouble())
        _objectsList.value = _objectsList.value.map {
            if (it.id == objectId) it.copy(dim1 = d1, dim2 = d2, dim3 = d3) else it
        }
        FreeCadNative.recompute(docId)
    }

    fun toggleObjectVisibility(objectId: Long) {
        val docId = _activeDocId.value
        if (docId == 0L) return
        val obj = _objectsList.value.find { it.id == objectId } ?: return
        val nextVisible = !obj.isVisible
        FreeCadNative.setObjectVisibility(docId, objectId, nextVisible)
        _objectsList.value = _objectsList.value.map {
            if (it.id == objectId) it.copy(isVisible = nextVisible) else it
        }
        FreeCadNative.recompute(docId)
    }

    override fun onCleared() {
        super.onCleared()
        // No-op or native resource clean-up if needed (MainActivity handles shutdown)
    }
}
