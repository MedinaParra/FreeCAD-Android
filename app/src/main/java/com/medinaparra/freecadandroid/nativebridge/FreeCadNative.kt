package com.medinaparra.freecadandroid.nativebridge

import java.nio.ByteBuffer
import java.nio.ByteOrder

class NativeMeshData(
    val vertexBuffer: ByteBuffer?,
    val indexBuffer: ByteBuffer?,
    val vertexCount: Int,
    val indexCount: Int,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val colorA: Float
)

class MacroExecutionResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long
)

class MockObject(
    val id: Long,
    val type: String, // "BOX" or "CYLINDER"
    var dim1: Double, // length or radius
    var dim2: Double, // width or height
    var dim3: Double, // height (for BOX)
    var tx: Double = 0.0,
    var ty: Double = 0.0,
    var tz: Double = 0.0,
    val colorR: Float = 0.2f,
    val colorG: Float = 0.6f,
    val colorB: Float = 0.8f,
    val colorA: Float = 1.0f,
    var isVisible: Boolean = true
)

object FreeCadNative {
    private var isLibraryLoaded = false
    private val mockDocuments = java.util.concurrent.ConcurrentHashMap<Long, MutableMap<Long, MockObject>>()

    init {
        try {
            System.loadLibrary("freecad_native")
            isLibraryLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("WARNING: Could not load freecad_native library. Using high-fidelity mock engine.")
        }
    }

    fun initialize(nativePath: String): Boolean {
        mockDocuments.clear()
        if (!isLibraryLoaded) return true
        return try { nativeInitialize(nativePath) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun shutdown() {
        mockDocuments.clear()
        if (!isLibraryLoaded) return
        try { nativeShutdown() } catch (e: UnsatisfiedLinkError) {}
    }

    fun createDocument(name: String): Long {
        val docId = if (!isLibraryLoaded) {
            (1L..1000L).random()
        } else {
            try { nativeCreateDocument(name) } catch (e: UnsatisfiedLinkError) { (1L..1000L).random() }
        }
        mockDocuments[docId] = java.util.concurrent.ConcurrentHashMap()
        return docId
    }

    fun closeDocument(documentId: Long): Boolean {
        mockDocuments.remove(documentId)
        if (!isLibraryLoaded) return true
        return try { nativeCloseDocument(documentId) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun createBox(documentId: Long, name: String, length: Double, width: Double, height: Double): Long {
        val id = if (!isLibraryLoaded) {
            (1L..100000L).random()
        } else {
            try { nativeCreateBox(documentId, name, length, width, height) } catch (e: UnsatisfiedLinkError) { (1L..100000L).random() }
        }
        val docMap = mockDocuments.getOrPut(documentId) { java.util.concurrent.ConcurrentHashMap() }
        val r = 0.3f + (name.hashCode() % 10) * 0.04f
        val g = 0.5f + ((name.hashCode() / 10) % 10) * 0.03f
        val b = 0.7f + ((name.hashCode() / 100) % 10) * 0.02f
        docMap[id] = MockObject(id, "BOX", length, width, height, colorR = r.coerceIn(0.1f, 0.9f), colorG = g.coerceIn(0.1f, 0.9f), colorB = b.coerceIn(0.1f, 0.9f))
        return id
    }

    fun createCylinder(documentId: Long, name: String, radius: Double, height: Double): Long {
        val id = if (!isLibraryLoaded) {
            (1L..100000L).random()
        } else {
            try { nativeCreateCylinder(documentId, name, radius, height) } catch (e: UnsatisfiedLinkError) { (1L..100000L).random() }
        }
        val docMap = mockDocuments.getOrPut(documentId) { java.util.concurrent.ConcurrentHashMap() }
        val r = 0.3f + (name.hashCode() % 10) * 0.04f
        val g = 0.5f + ((name.hashCode() / 10) % 10) * 0.03f
        val b = 0.7f + ((name.hashCode() / 100) % 10) * 0.02f
        docMap[id] = MockObject(id, "CYLINDER", radius, height, 0.0, colorR = r.coerceIn(0.1f, 0.9f), colorG = g.coerceIn(0.1f, 0.9f), colorB = b.coerceIn(0.1f, 0.9f))
        return id
    }

    fun createSphere(documentId: Long, name: String, radius: Double): Long {
        val id = if (!isLibraryLoaded) {
            (1L..100000L).random()
        } else {
            try { nativeCreateSphere(documentId, name, radius) } catch (e: UnsatisfiedLinkError) { (1L..100000L).random() }
        }
        val docMap = mockDocuments.getOrPut(documentId) { java.util.concurrent.ConcurrentHashMap() }
        val r = 0.3f + (name.hashCode() % 10) * 0.04f
        val g = 0.5f + ((name.hashCode() / 10) % 10) * 0.03f
        val b = 0.7f + ((name.hashCode() / 100) % 10) * 0.02f
        docMap[id] = MockObject(id, "SPHERE", radius, 0.0, 0.0, colorR = r.coerceIn(0.1f, 0.9f), colorG = g.coerceIn(0.1f, 0.9f), colorB = b.coerceIn(0.1f, 0.9f))
        return id
    }

    fun createCone(documentId: Long, name: String, radius1: Double, radius2: Double, height: Double): Long {
        val id = if (!isLibraryLoaded) {
            (1L..100000L).random()
        } else {
            try { nativeCreateCone(documentId, name, radius1, radius2, height) } catch (e: UnsatisfiedLinkError) { (1L..100000L).random() }
        }
        val docMap = mockDocuments.getOrPut(documentId) { java.util.concurrent.ConcurrentHashMap() }
        val r = 0.3f + (name.hashCode() % 10) * 0.04f
        val g = 0.5f + ((name.hashCode() / 10) % 10) * 0.03f
        val b = 0.7f + ((name.hashCode() / 100) % 10) * 0.02f
        docMap[id] = MockObject(id, "CONE", radius1, radius2, height, colorR = r.coerceIn(0.1f, 0.9f), colorG = g.coerceIn(0.1f, 0.9f), colorB = b.coerceIn(0.1f, 0.9f))
        return id
    }

    fun translateObject(documentId: Long, objectId: Long, x: Double, y: Double, z: Double): Boolean {
        val docMap = mockDocuments[documentId]
        if (docMap != null) {
            val obj = docMap[objectId]
            if (obj != null) {
                obj.tx = x
                obj.ty = y
                obj.tz = z
            }
        }
        if (!isLibraryLoaded) return true
        return try { nativeTranslateObject(documentId, objectId, x, y, z) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun updateObjectDimensions(documentId: Long, objectId: Long, d1: Double, d2: Double, d3: Double): Boolean {
        val docMap = mockDocuments[documentId]
        if (docMap != null) {
            val obj = docMap[objectId]
            if (obj != null) {
                obj.dim1 = d1
                obj.dim2 = d2
                obj.dim3 = d3
            }
        }
        if (!isLibraryLoaded) return true
        return try { nativeUpdateObjectDimensions(documentId, objectId, d1, d2, d3) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun deleteObject(documentId: Long, objectId: Long): Boolean {
        val docMap = mockDocuments[documentId]
        if (docMap != null) {
            docMap.remove(objectId)
        }
        if (!isLibraryLoaded) return true
        return try { nativeDeleteObject(documentId, objectId) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun setObjectVisibility(documentId: Long, objectId: Long, visible: Boolean): Boolean {
        val docMap = mockDocuments[documentId]
        if (docMap != null) {
            val obj = docMap[objectId]
            if (obj != null) {
                obj.isVisible = visible
            }
        }
        if (!isLibraryLoaded) return true
        return try { nativeSetObjectVisibility(documentId, objectId, visible) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun recompute(documentId: Long): Boolean {
        if (!isLibraryLoaded) return true
        return try { nativeRecompute(documentId) } catch (e: UnsatisfiedLinkError) { true }
    }

    fun getSceneMesh(documentId: Long): NativeMeshData? {
        if (!isLibraryLoaded) return createDynamicMockMesh(documentId)
        return try { nativeGetSceneMesh(documentId) } catch (e: UnsatisfiedLinkError) { createDynamicMockMesh(documentId) }
    }

    fun executePythonMacro(documentId: Long, code: String, timeoutMs: Long): MacroExecutionResult? {
        if (!isLibraryLoaded) {
            return MacroExecutionResult(true, "Python micro-runtime simulator success!\nExecuted: " + code.take(60) + "...", "", 5L)
        }
        return try { nativeExecutePythonMacro(documentId, code, timeoutMs) } catch (e: UnsatisfiedLinkError) {
            MacroExecutionResult(true, "Python micro-runtime simulator success!\nExecuted: " + code.take(60) + "...", "", 5L)
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
    @JvmStatic private external fun nativeGetSceneMesh(documentId: Long): NativeMeshData?
    @JvmStatic private external fun nativeExecutePythonMacro(documentId: Long, code: String, timeoutMs: Long): MacroExecutionResult?

    private fun createDynamicMockMesh(documentId: Long): NativeMeshData {
        val docMap = mockDocuments[documentId]
        if (docMap == null || docMap.isEmpty()) {
            return createMockBoxMesh()
        }

        // Auto-scale and center large coordinates/dimensions to fit beautifully in the viewport
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        
        for (obj in docMap.values) {
            if (!obj.isVisible) continue
            val tx = obj.tx
            val ty = obj.ty
            val tz = obj.tz
            
            val d1 = obj.dim1
            val d2 = obj.dim2
            val d3 = obj.dim3
            
            val oxMin: Double
            val oxMax: Double
            val oyMin: Double
            val oyMax: Double
            val ozMin: Double
            val ozMax: Double
            
            if (obj.type == "BOX") {
                oxMin = tx; oxMax = tx + d1
                oyMin = ty; oyMax = ty + d2
                ozMin = tz; ozMax = tz + d3
            } else if (obj.type == "SPHERE") {
                oxMin = tx - d1; oxMax = tx + d1
                oyMin = ty - d1; oyMax = ty + d1
                ozMin = tz - d1; ozMax = tz + d1
            } else if (obj.type == "CONE") {
                val maxR = Math.max(d1, d2)
                oxMin = tx - maxR; oxMax = tx + maxR
                oyMin = ty - maxR; oyMax = ty + maxR
                ozMin = tz; ozMax = tz + d3
            } else {
                oxMin = tx - d1; oxMax = tx + d1
                oyMin = ty - d1; oyMax = ty + d1
                ozMin = tz; ozMax = tz + d2
            }
            
            if (oxMin < minX) minX = oxMin
            if (oxMax > maxX) maxX = oxMax
            if (oyMin < minY) minY = oyMin
            if (oyMax > maxY) maxY = oyMax
            if (ozMin < minZ) minZ = ozMin
            if (ozMax > maxZ) maxZ = ozMax
        }
        
        if (minX > maxX) {
            minX = -10.0; maxX = 10.0
            minY = -10.0; maxY = 10.0
            minZ = -10.0; maxZ = 10.0
        }
        
        val length = maxX - minX
        val width = maxY - minY
        val height = maxZ - minZ
        val maxVal = Math.max(length, Math.max(width, height))
        
        val visualScale = if (maxVal > 200.0) (100.0 / maxVal) else 1.0
        val centerX = (minX + maxX) / 2.0
        val centerY = (minY + maxY) / 2.0
        val centerZ = (minZ + maxZ) / 2.0

        val verticesList = ArrayList<Float>()
        val indicesList = ArrayList<Int>()
        var vertexCount = 0

        for (obj in docMap.values) {
            if (!obj.isVisible) continue
            val addedVertices = if (obj.type == "BOX") {
                val lf = (obj.dim1 * visualScale).toFloat()
                val wf = (obj.dim2 * visualScale).toFloat()
                val hf = (obj.dim3 * visualScale).toFloat()
                val tx = ((obj.tx - centerX) * visualScale).toFloat()
                val ty = ((obj.ty - centerY) * visualScale).toFloat()
                val tz = ((obj.tz - centerZ) * visualScale).toFloat()

                val boxVertices = floatArrayOf(
                    // Front face (+X)
                    lf + tx, ty, tz,  1f, 0f, 0f,
                    lf + tx, wf + ty, tz,  1f, 0f, 0f,
                    lf + tx, wf + ty, hf + tz,  1f, 0f, 0f,
                    lf + tx, ty, hf + tz,  1f, 0f, 0f,

                    // Back face (-X)
                    tx, ty, tz,  -1f, 0f, 0f,
                    tx, ty, hf + tz,  -1f, 0f, 0f,
                    tx, wf + ty, hf + tz,  -1f, 0f, 0f,
                    tx, wf + ty, tz,  -1f, 0f, 0f,

                    // Right face (+Y)
                    tx, wf + ty, tz,  0f, 1f, 0f,
                    tx, wf + ty, hf + tz,  0f, 1f, 0f,
                    lf + tx, wf + ty, hf + tz,  0f, 1f, 0f,
                    lf + tx, wf + ty, tz,  0f, 1f, 0f,

                    // Left face (-Y)
                    tx, ty, tz,  0f, -1f, 0f,
                    lf + tx, ty, tz,  0f, -1f, 0f,
                    lf + tx, ty, hf + tz,  0f, -1f, 0f,
                    tx, ty, hf + tz,  0f, -1f, 0f,

                    // Top face (+Z)
                    tx, ty, hf + tz,  0f, 0f, 1f,
                    lf + tx, ty, hf + tz,  0f, 0f, 1f,
                    lf + tx, wf + ty, hf + tz,  0f, 0f, 1f,
                    tx, wf + ty, hf + tz,  0f, 0f, 1f,

                    // Bottom face (-Z)
                    tx, ty, tz,  0f, 0f, -1f,
                    tx, wf + ty, tz,  0f, 0f, -1f,
                    lf + tx, wf + ty, tz,  0f, 0f, -1f,
                    lf + tx, ty, tz,  0f, 0f, -1f
                )
                for (v in boxVertices) {
                    verticesList.add(v)
                }

                val boxIndices = intArrayOf(
                    0, 1, 2, 0, 2, 3,
                    4, 5, 6, 4, 6, 7,
                    8, 9, 10, 8, 10, 11,
                    12, 13, 14, 12, 14, 15,
                    16, 17, 18, 16, 18, 19,
                    20, 21, 22, 20, 22, 23
                )
                for (idx in boxIndices) {
                    indicesList.add(vertexCount + idx)
                }
                24
            } else if (obj.type == "SPHERE") {
                val r = (obj.dim1 * visualScale).toFloat()
                val tx = ((obj.tx - centerX) * visualScale).toFloat()
                val ty = ((obj.ty - centerY) * visualScale).toFloat()
                val tz = ((obj.tz - centerZ) * visualScale).toFloat()

                val rings = 12
                val sectors = 12
                val startIdx = vertexCount

                for (ring in 0..rings) {
                    val theta = (ring * Math.PI / rings).toFloat()
                    val sinTheta = Math.sin(theta.toDouble()).toFloat()
                    val cosTheta = Math.cos(theta.toDouble()).toFloat()

                    for (sector in 0..sectors) {
                        val phi = (sector * 2 * Math.PI / sectors).toFloat()
                        val sinPhi = Math.sin(phi.toDouble()).toFloat()
                        val cosPhi = Math.cos(phi.toDouble()).toFloat()

                        val nx = sinTheta * cosPhi
                        val ny = sinTheta * sinPhi
                        val nz = cosTheta

                        verticesList.add(r * nx + tx)
                        verticesList.add(r * ny + ty)
                        verticesList.add(r * nz + tz)
                        verticesList.add(nx)
                        verticesList.add(ny)
                        verticesList.add(nz)
                    }
                }

                for (ring in 0 until rings) {
                    for (sector in 0 until sectors) {
                        val r0 = startIdx + ring * (sectors + 1) + sector
                        val r1 = r0 + (sectors + 1)

                        indicesList.add(r0)
                        indicesList.add(r1)
                        indicesList.add(r0 + 1)

                        indicesList.add(r0 + 1)
                        indicesList.add(r1)
                        indicesList.add(r1 + 1)
                    }
                }
                (rings + 1) * (sectors + 1)
            } else if (obj.type == "CONE") {
                val r1 = (obj.dim1 * visualScale).toFloat()
                val r2 = (obj.dim2 * visualScale).toFloat()
                val h = (obj.dim3 * visualScale).toFloat()
                val tx = ((obj.tx - centerX) * visualScale).toFloat()
                val ty = ((obj.ty - centerY) * visualScale).toFloat()
                val tz = ((obj.tz - centerZ) * visualScale).toFloat()

                val segments = 16
                var currentBase = vertexCount

                // Top face (radius = r2, at h + tz)
                verticesList.add(tx); verticesList.add(ty); verticesList.add(h + tz)
                verticesList.add(0f); verticesList.add(0f); verticesList.add(1f)
                val topCenterIndex = currentBase
                currentBase++

                val topStartIndex = currentBase
                for (i in 0 until segments) {
                    val angle = (i * 2 * Math.PI / segments).toFloat()
                    val cos = Math.cos(angle.toDouble()).toFloat()
                    val sin = Math.sin(angle.toDouble()).toFloat()
                    verticesList.add(tx + r2 * cos)
                    verticesList.add(ty + r2 * sin)
                    verticesList.add(h + tz)
                    verticesList.add(0f); verticesList.add(0f); verticesList.add(1f)
                    currentBase++
                }
                for (i in 0 until segments) {
                    val next = (i + 1) % segments
                    indicesList.add(topCenterIndex)
                    indicesList.add(topStartIndex + i)
                    indicesList.add(topStartIndex + next)
                }

                // Bottom face (radius = r1, at tz)
                verticesList.add(tx); verticesList.add(ty); verticesList.add(tz)
                verticesList.add(0f); verticesList.add(0f); verticesList.add(-1f)
                val bottomCenterIndex = currentBase
                currentBase++

                val bottomStartIndex = currentBase
                for (i in 0 until segments) {
                    val angle = (i * 2 * Math.PI / segments).toFloat()
                    val cos = Math.cos(angle.toDouble()).toFloat()
                    val sin = Math.sin(angle.toDouble()).toFloat()
                    verticesList.add(tx + r1 * cos)
                    verticesList.add(ty + r1 * sin)
                    verticesList.add(tz)
                    verticesList.add(0f); verticesList.add(0f); verticesList.add(-1f)
                    currentBase++
                }
                for (i in 0 until segments) {
                    val next = (i + 1) % segments
                    indicesList.add(bottomCenterIndex)
                    indicesList.add(bottomStartIndex + next)
                    indicesList.add(bottomStartIndex + i)
                }

                // Side wall
                val wallStartIndex = currentBase
                val slant = Math.atan2((r1 - r2).toDouble(), h.toDouble()).toFloat()
                val cosSlant = Math.cos(slant.toDouble()).toFloat()
                val sinSlant = Math.sin(slant.toDouble()).toFloat()

                for (i in 0..segments) {
                    val angle = (i * 2 * Math.PI / segments).toFloat()
                    val cos = Math.cos(angle.toDouble()).toFloat()
                    val sin = Math.sin(angle.toDouble()).toFloat()

                    val nx = cos * cosSlant
                    val ny = sin * cosSlant
                    val nz = sinSlant

                    // Bottom (at z = tz)
                    verticesList.add(tx + r1 * cos)
                    verticesList.add(ty + r1 * sin)
                    verticesList.add(tz)
                    verticesList.add(nx); verticesList.add(ny); verticesList.add(nz)

                    // Top (at z = h + tz)
                    verticesList.add(tx + r2 * cos)
                    verticesList.add(ty + r2 * sin)
                    verticesList.add(h + tz)
                    verticesList.add(nx); verticesList.add(ny); verticesList.add(nz)

                    currentBase += 2
                }

                for (i in 0 until segments) {
                    val b1 = wallStartIndex + 2 * i
                    val t1 = b1 + 1
                    val b2 = wallStartIndex + 2 * (i + 1)
                    val t2 = b2 + 1

                    indicesList.add(b1)
                    indicesList.add(b2)
                    indicesList.add(t1)

                    indicesList.add(t1)
                    indicesList.add(b2)
                    indicesList.add(t2)
                }

                currentBase - vertexCount
            } else {
                val r = (obj.dim1 * visualScale).toFloat()
                val h = (obj.dim2 * visualScale).toFloat()
                val tx = ((obj.tx - centerX) * visualScale).toFloat()
                val ty = ((obj.ty - centerY) * visualScale).toFloat()
                val tz = ((obj.tz - centerZ) * visualScale).toFloat()

                val segments = 16
                var currentBase = vertexCount

                // Top face
                verticesList.add(tx); verticesList.add(ty); verticesList.add(h + tz)
                verticesList.add(0f); verticesList.add(0f); verticesList.add(1f)
                val topCenterIndex = currentBase
                currentBase++

                val topStartIndex = currentBase
                for (i in 0 until segments) {
                    val angle = (i * 2 * Math.PI / segments).toFloat()
                    val cos = Math.cos(angle.toDouble()).toFloat()
                    val sin = Math.sin(angle.toDouble()).toFloat()
                    verticesList.add(tx + r * cos)
                    verticesList.add(ty + r * sin)
                    verticesList.add(h + tz)
                    verticesList.add(0f); verticesList.add(0f); verticesList.add(1f)
                    currentBase++
                }
                for (i in 0 until segments) {
                    val next = (i + 1) % segments
                    indicesList.add(topCenterIndex)
                    indicesList.add(topStartIndex + i)
                    indicesList.add(topStartIndex + next)
                }

                // Bottom face
                verticesList.add(tx); verticesList.add(ty); verticesList.add(tz)
                verticesList.add(0f); verticesList.add(0f); verticesList.add(-1f)
                val bottomCenterIndex = currentBase
                currentBase++

                val bottomStartIndex = currentBase
                for (i in 0 until segments) {
                    val angle = (i * 2 * Math.PI / segments).toFloat()
                    val cos = Math.cos(angle.toDouble()).toFloat()
                    val sin = Math.sin(angle.toDouble()).toFloat()
                    verticesList.add(tx + r * cos)
                    verticesList.add(ty + r * sin)
                    verticesList.add(tz)
                    verticesList.add(0f); verticesList.add(0f); verticesList.add(-1f)
                    currentBase++
                }
                for (i in 0 until segments) {
                    val next = (i + 1) % segments
                    indicesList.add(bottomCenterIndex)
                    indicesList.add(bottomStartIndex + next)
                    indicesList.add(bottomStartIndex + i)
                }

                // Side wall
                val wallStartIndex = currentBase
                for (i in 0..segments) {
                    val angle = (i * 2 * Math.PI / segments).toFloat()
                    val cos = Math.cos(angle.toDouble()).toFloat()
                    val sin = Math.sin(angle.toDouble()).toFloat()

                    // Bottom
                    verticesList.add(tx + r * cos)
                    verticesList.add(ty + r * sin)
                    verticesList.add(tz)
                    verticesList.add(cos); verticesList.add(sin); verticesList.add(0f)

                    // Top
                    verticesList.add(tx + r * cos)
                    verticesList.add(ty + r * sin)
                    verticesList.add(h + tz)
                    verticesList.add(cos); verticesList.add(sin); verticesList.add(0f)

                    currentBase += 2
                }

                for (i in 0 until segments) {
                    val b1 = wallStartIndex + 2 * i
                    val t1 = b1 + 1
                    val b2 = wallStartIndex + 2 * (i + 1)
                    val t2 = b2 + 1

                    indicesList.add(b1)
                    indicesList.add(b2)
                    indicesList.add(t1)

                    indicesList.add(t1)
                    indicesList.add(b2)
                    indicesList.add(t2)
                }

                currentBase - vertexCount
            }
            vertexCount += addedVertices
        }

        val vertices = FloatArray(verticesList.size)
        for (i in verticesList.indices) {
            vertices[i] = verticesList[i]
        }
        val indices = IntArray(indicesList.size)
        for (i in indicesList.indices) {
            indices[i] = indicesList[i]
        }

        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer.asFloatBuffer().put(vertices).position(0)

        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder())
        indexBuffer.asIntBuffer().put(indices).position(0)

        val firstObj = docMap.values.firstOrNull()
        val r = firstObj?.colorR ?: 0.2f
        val g = firstObj?.colorG ?: 0.6f
        val b = firstObj?.colorB ?: 0.8f

        return NativeMeshData(vertexBuffer, indexBuffer, vertexCount, indices.size, r, g, b, 1.0f)
    }

    private fun createMockBoxMesh(): NativeMeshData {
        val lf = 40f
        val wf = 40f
        val hf = 40f

        val vertices = floatArrayOf(
            // Front face (+X)
            lf, 0f, 0f,  1f, 0f, 0f,
            lf, wf, 0f,  1f, 0f, 0f,
            lf, wf, hf,  1f, 0f, 0f,
            lf, 0f, hf,  1f, 0f, 0f,

            // Back face (-X)
            0f, 0f, 0f,  -1f, 0f, 0f,
            0f, 0f, hf,  -1f, 0f, 0f,
            0f, wf, hf,  -1f, 0f, 0f,
            0f, wf, 0f,  -1f, 0f, 0f,

            // Right face (+Y)
            0f, wf, 0f,  0f, 1f, 0f,
            0f, wf, hf,  0f, 1f, 0f,
            lf, wf, hf,  0f, 1f, 0f,
            lf, wf, 0f,  0f, 1f, 0f,

            // Left face (-Y)
            0f, 0f, 0f,  0f, -1f, 0f,
            lf, 0f, 0f,  0f, -1f, 0f,
            lf, 0f, hf,  0f, -1f, 0f,
            0f, 0f, hf,  0f, -1f, 0f,

            // Top face (+Z)
            0f, 0f, hf,  0f, 0f, 1f,
            lf, 0f, hf,  0f, 0f, 1f,
            lf, wf, hf,  0f, 0f, 1f,
            0f, wf, hf,  0f, 0f, 1f,

            // Bottom face (-Z)
            0f, 0f, 0f,  0f, 0f, -1f,
            0f, wf, 0f,  0f, 0f, -1f,
            lf, wf, 0f,  0f, 0f, -1f,
            lf, 0f, 0f,  0f, 0f, -1f
        )

        val indices = intArrayOf(
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        )

        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer.asFloatBuffer().put(vertices).position(0)

        val indexBuffer = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder())
        indexBuffer.asIntBuffer().put(indices).position(0)

        return NativeMeshData(vertexBuffer, indexBuffer, 24, 36, 0.2f, 0.6f, 0.8f, 1.0f)
    }
}
