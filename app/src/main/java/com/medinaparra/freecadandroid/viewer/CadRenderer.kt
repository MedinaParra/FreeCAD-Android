package com.medinaparra.freecadandroid.viewer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.medinaparra.freecadandroid.nativebridge.FreeCadNative
import com.medinaparra.freecadandroid.nativebridge.NativeSceneMesh
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CadRenderer(val cameraController: CameraController) : GLSurfaceView.Renderer {

    private val TAG = "CadRenderer"

    // Shader program handle
    private var shaderProgram = 0

    // Uniform locations
    private var mvpMatrixLoc = -1
    private var mvMatrixLoc = -1
    private var colorLoc = -1
    private var flatShadingLoc = -1

    // Model-View-Projection matrices
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Grid and axes buffers
    private var gridVbo = 0
    private var gridVertexCount = 0
    private var axesVbo = 0

    // CAD Mesh buffers
    private var meshVbo = 0
    private var meshIbo = 0
    private var meshIndexCount = 0
    private var meshColor = floatArrayOf(0.2f, 0.6f, 0.8f, 1.0f)

    @Volatile
    private var activeDocumentId: Long = 0
    @Volatile
    private var meshNeedsUpdate = false

    // Bounding Box state
    private var modelMinX = -20f
    private var modelMinY = -20f
    private var modelMinZ = -20f
    private var modelMaxX = 20f
    private var modelMaxY = 20f
    private var modelMaxZ = 20f

    private var aspect = 1.0f

    fun setActiveDocument(documentId: Long) {
        activeDocumentId = documentId
        meshNeedsUpdate = true
    }

    fun requestMeshUpdate() {
        meshNeedsUpdate = true
    }

    fun fitAll() {
        cameraController.fitBounds(
            modelMinX, modelMinY, modelMinZ,
            modelMaxX, modelMaxY, modelMaxZ,
            45f, aspect
        )
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f, 0.15f, 0.18f, 1.0f) // Sleek dark slate industrial background
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)

        // Reset all handles to zero because context was recreated
        shaderProgram = 0
        gridVbo = 0
        axesVbo = 0
        meshVbo = 0
        meshIbo = 0
        meshIndexCount = 0

        // Compile and link shader program
        shaderProgram = createProgram(vertexShaderSource, fragmentShaderSource)
        if (shaderProgram == 0) {
            Log.e(TAG, "Shader compilation failed!")
            return
        }

        mvpMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        mvMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "uMVMatrix")
        colorLoc = GLES30.glGetUniformLocation(shaderProgram, "uColor")
        flatShadingLoc = GLES30.glGetUniformLocation(shaderProgram, "uFlatShading")

        setupGridAndAxes()

        // Active mesh needs to be reloaded on context recreation
        meshNeedsUpdate = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspect = if (height == 0) 1.0f else width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (shaderProgram == 0) return

        // 1. Calculate dynamic near/far planes based on distance and model radius
        val nearPlane = (cameraController.distance - cameraController.modelRadius * 1.5f).coerceAtLeast(0.1f)
        val farPlane = (cameraController.distance + cameraController.modelRadius * 2.5f).coerceAtLeast(10f)
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, aspect, nearPlane, farPlane)

        // 2. Calculate eye position using yaw, pitch, distance, and target
        val yawRad = Math.toRadians(cameraController.yawDegrees.toDouble())
        val pitchRad = Math.toRadians(cameraController.pitchDegrees.toDouble())

        val cosPitch = kotlin.math.cos(pitchRad)
        val sinPitch = kotlin.math.sin(pitchRad)
        val cosYaw = kotlin.math.cos(yawRad)
        val sinYaw = kotlin.math.sin(yawRad)

        val eyeX = (cameraController.targetX + cameraController.distance * cosPitch * cosYaw).toFloat()
        val eyeY = (cameraController.targetY + cameraController.distance * cosPitch * sinYaw).toFloat()
        val eyeZ = (cameraController.targetZ + cameraController.distance * sinPitch).toFloat()

        // 3. Build view matrix looking at target, with Z as vertical axis
        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            cameraController.targetX, cameraController.targetY, cameraController.targetZ,
            0f, 0f, 1f
        )

        GLES30.glUseProgram(shaderProgram)

        // 4. Query and upload CAD Mesh if updated
        if (meshNeedsUpdate && activeDocumentId != 0L) {
            uploadCadMesh()
            meshNeedsUpdate = false
        }

        // 5. Draw Grid and Reference Axes (with flat shading)
        GLES30.glUniform1i(flatShadingLoc, 1) // Enable flat shading
        drawGrid()
        drawAxes()

        // 6. Draw CAD Geometries (with Phong shading)
        if (meshIndexCount > 0) {
            GLES30.glUniform1i(flatShadingLoc, 0) // Enable Phong lighting
            drawCadMesh()
        }
    }

    private fun uploadCadMesh() {
        try {
            val meshData: NativeSceneMesh? = FreeCadNative.getSceneMesh(activeDocumentId)
            if (meshData != null && meshData.vertexCount > 0 && meshData.indexCount > 0) {
                meshIndexCount = meshData.indexCount
                
                modelMinX = meshData.minX
                modelMinY = meshData.minY
                modelMinZ = meshData.minZ
                modelMaxX = meshData.maxX
                modelMaxY = meshData.maxY
                modelMaxZ = meshData.maxZ

                // Upload Vertex Buffer
                if (meshVbo == 0) {
                    val buffers = IntArray(1)
                    GLES30.glGenBuffers(1, buffers, 0)
                    meshVbo = buffers[0]
                }
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVbo)
                val vertexData: ByteBuffer = meshData.vertexBuffer ?: return
                vertexData.position(0)
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.remaining(), vertexData, GLES30.GL_DYNAMIC_DRAW)

                // Upload Index Buffer
                if (meshIbo == 0) {
                    val buffers = IntArray(1)
                    GLES30.glGenBuffers(1, buffers, 0)
                    meshIbo = buffers[0]
                }
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshIbo)
                val indexData: ByteBuffer = meshData.indexBuffer ?: return
                indexData.position(0)
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexData.remaining(), indexData, GLES30.GL_DYNAMIC_DRAW)

                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)

                // Automatically fit bounds once after successfully loading a valid mesh
                cameraController.fitBounds(
                    modelMinX, modelMinY, modelMinZ,
                    modelMaxX, modelMaxY, modelMaxZ,
                    45f, aspect
                )
            } else {
                clearMeshBuffers()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading CAD mesh: ${e.message}")
        }
    }

    private fun drawCadMesh() {
        if (meshVbo == 0 || meshIbo == 0) return

        // Maintain modelMatrix as identity (keeps world coordinates aligned with grid and axes)
        Matrix.setIdentityM(modelMatrix, 0)
        
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES30.glUniformMatrix4fv(mvMatrixLoc, 1, false, mvMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4fv(colorLoc, 1, meshColor, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshVbo)

        // Vertex layout: x, y, z (3 floats), nx, ny, nz (3 floats)
        val stride = 6 * 4 // 6 floats * 4 bytes each
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshIbo)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, meshIndexCount, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun clearSceneOnGlThread() {
        clearMeshBuffers()
    }

    private fun clearMeshBuffers() {
        if (meshVbo != 0) {
            val buffers = intArrayOf(meshVbo)
            GLES30.glDeleteBuffers(1, buffers, 0)
            meshVbo = 0
        }
        if (meshIbo != 0) {
            val buffers = intArrayOf(meshIbo)
            GLES30.glDeleteBuffers(1, buffers, 0)
            meshIbo = 0
        }
        meshIndexCount = 0
    }

    private fun clearGlResources() {
        if (gridVbo != 0) {
            val buffers = intArrayOf(gridVbo)
            GLES30.glDeleteBuffers(1, buffers, 0)
            gridVbo = 0
        }
        if (axesVbo != 0) {
            val buffers = intArrayOf(axesVbo)
            GLES30.glDeleteBuffers(1, buffers, 0)
            axesVbo = 0
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
    }

    private fun setupGridAndAxes() {
        // Build reference grid in X-Y plane at Z=0
        val gridLines = ArrayList<Float>()
        val range = 200
        val step = 10
        for (i in -range..range step step) {
            // Line parallel to Y-axis
            gridLines.add(i.toFloat()); gridLines.add(-range.toFloat()); gridLines.add(0f)
            gridLines.add(0f); gridLines.add(0f); gridLines.add(0f) // Dummy normal

            gridLines.add(i.toFloat()); gridLines.add(range.toFloat()); gridLines.add(0f)
            gridLines.add(0f); gridLines.add(0f); gridLines.add(0f)

            // Line parallel to X-axis
            gridLines.add(-range.toFloat()); gridLines.add(i.toFloat()); gridLines.add(0f)
            gridLines.add(0f); gridLines.add(0f); gridLines.add(0f)

            gridLines.add(range.toFloat()); gridLines.add(i.toFloat()); gridLines.add(0f)
            gridLines.add(0f); gridLines.add(0f); gridLines.add(0f)
        }
        gridVertexCount = gridLines.size / 6

        val gridFloatBuf = ByteBuffer.allocateDirect(gridLines.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (valInList in gridLines) {
            gridFloatBuf.put(valInList)
        }
        gridFloatBuf.position(0)

        val gridBufs = IntArray(1)
        GLES30.glGenBuffers(1, gridBufs, 0)
        gridVbo = gridBufs[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, gridLines.size * 4, gridFloatBuf, GLES30.GL_STATIC_DRAW)

        // Build Cartesian Coordinate Axes: X (Red), Y (Green), Z (Blue)
        val axisLen = 150f
        val axesData = floatArrayOf(
            // X-axis (Red)
            0f, 0f, 0f,   0f, 0f, 0f,
            axisLen, 0f, 0f,   0f, 0f, 0f,
            // Y-axis (Green)
            0f, 0f, 0f,   0f, 0f, 0f,
            0f, axisLen, 0f,   0f, 0f, 0f,
            // Z-axis (Blue)
            0f, 0f, 0f,   0f, 0f, 0f,
            0f, 0f, axisLen,   0f, 0f, 0f
        )
        val axesFloatBuf = ByteBuffer.allocateDirect(axesData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        axesFloatBuf.put(axesData)
        axesFloatBuf.position(0)

        val axesBufs = IntArray(1)
        GLES30.glGenBuffers(1, axesBufs, 0)
        axesVbo = axesBufs[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, axesVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, axesData.size * 4, axesFloatBuf, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun drawGrid() {
        if (gridVbo == 0) return

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES30.glUniformMatrix4fv(mvMatrixLoc, 1, false, mvMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(colorLoc, 0.4f, 0.45f, 0.5f, 0.35f)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, gridVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 6 * 4, 0)

        GLES30.glDrawArrays(GLES30.GL_LINES, 0, gridVertexCount)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun drawAxes() {
        if (axesVbo == 0) return

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES30.glUniformMatrix4fv(mvMatrixLoc, 1, false, mvMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, axesVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 6 * 4, 0)

        GLES30.glLineWidth(3.0f)

        // Draw X-axis (Red)
        GLES30.glUniform4f(colorLoc, 0.95f, 0.25f, 0.25f, 1.0f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2)

        // Draw Y-axis (Green)
        GLES30.glUniform4f(colorLoc, 0.25f, 0.85f, 0.25f, 1.0f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 2, 2)

        // Draw Z-axis (Blue)
        GLES30.glUniform4f(colorLoc, 0.25f, 0.45f, 0.95f, 1.0f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 4, 2)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            Log.e(TAG, "Error compiling shader: $error")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        if (vertexShader == 0) return 0

        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)
        if (fragmentShader == 0) return 0

        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking program: " + GLES30.glGetProgramInfoLog(program))
            GLES30.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private val vertexShaderSource = """
        #version 300 es
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;

        uniform mat4 uMVPMatrix;
        uniform mat4 uMVMatrix;

        out vec3 vNormal;
        out vec3 vPosition;

        void main() {
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            vPosition = vec3(uMVMatrix * vec4(aPosition, 1.0));
            vNormal = vec3(uMVMatrix * vec4(aNormal, 0.0));
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #version 300 es
        precision mediump float;

        in vec3 vNormal;
        in vec3 vPosition;

        uniform vec4 uColor;
        uniform bool uFlatShading;

        out vec4 fragColor;

        void main() {
            if (uFlatShading) {
                fragColor = uColor;
            } else {
                vec3 normal = normalize(vNormal);
                vec3 lightDir = normalize(vec3(80.0, 80.0, 150.0) - vPosition);
                
                float ambient = 0.35;
                float diffuse = max(dot(normal, lightDir), 0.0) * 0.65;
                
                vec3 viewDir = normalize(-vPosition);
                vec3 halfDir = normalize(lightDir + viewDir);
                float specular = pow(max(dot(normal, halfDir), 0.0), 32.0) * 0.35;
                
                vec3 finalColor = uColor.rgb * (ambient + diffuse) + vec3(specular);
                fragColor = vec4(finalColor, uColor.a);
            }
        }
    """.trimIndent()
}
