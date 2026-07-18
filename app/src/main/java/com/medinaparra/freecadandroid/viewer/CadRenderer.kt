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

    // Bounding Box and dynamic centering state (preserving original coordinates)
    private var modelMinX = -20f
    private var modelMinY = -20f
    private var modelMinZ = -20f
    private var modelMaxX = 20f
    private var modelMaxY = 20f
    private var modelMaxZ = 20f

    private var modelCenterX = 0f
    private var modelCenterY = 0f
    private var modelCenterZ = 0f
    private var modelSize = 40f
    private var aspect = 1.0f

    fun setActiveDocument(documentId: Long) {
        activeDocumentId = documentId
        meshNeedsUpdate = true
    }

    fun requestMeshUpdate() {
        meshNeedsUpdate = true
    }

    // Centering and zoom visual fitting (fitAll)
    fun fitAll() {
        cameraController.panX = 0f
        cameraController.panY = 0f
        cameraController.distance = (modelSize * 1.5f).coerceIn(10f, 5000f)
        cameraController.angleX = -45f
        cameraController.angleY = 45f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.12f, 0.15f, 0.18f, 1.0f) // Sleek dark slate industrial background
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)

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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (shaderProgram == 0) return

        // 1. Setup dynamic clipping planes proportional to model size to prevent z-fighting or geometric clipping
        val nearPlane = (cameraController.distance - modelSize * 1.2f).coerceAtLeast(0.1f)
        val farPlane = (cameraController.distance + modelSize * 2.5f).coerceAtLeast(100f)
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, aspect, nearPlane, farPlane)

        // 2. Calculate camera transform matrices
        Matrix.setIdentityM(viewMatrix, 0)
        // Translate view based on distance and pan
        Matrix.translateM(viewMatrix, 0, cameraController.panX, cameraController.panY, -cameraController.distance)
        // Rotate camera based on orbit angles
        Matrix.rotateM(viewMatrix, 0, cameraController.angleX, 1.0f, 0.0f, 0.0f)
        Matrix.rotateM(viewMatrix, 0, cameraController.angleY, 0.0f, 1.0f, 0.0f)

        GLES30.glUseProgram(shaderProgram)

        // 3. Query and upload CAD Mesh if updated
        if (meshNeedsUpdate && activeDocumentId != 0L) {
            uploadCadMesh()
            meshNeedsUpdate = false
        }

        // 4. Draw Grid and Reference Axes (with flat shading)
        GLES30.glUniform1i(flatShadingLoc, 1) // Enable flat shading
        drawGrid()
        drawAxes()

        // 5. Draw CAD Geometries (with Phong shading)
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
                
                // Track absolute bounding box bounds to compute dynamic centering matrix offsets and near/far plane depth
                modelMinX = meshData.minX
                modelMinY = meshData.minY
                modelMinZ = meshData.minZ
                modelMaxX = meshData.maxX
                modelMaxY = meshData.maxY
                modelMaxZ = meshData.maxZ

                modelCenterX = (modelMinX + modelMaxX) / 2f
                modelCenterY = (modelMinY + modelMaxY) / 2f
                modelCenterZ = (modelMinZ + modelMaxZ) / 2f

                val dx = modelMaxX - modelMinX
                val dy = modelMaxY - modelMinY
                val dz = modelMaxZ - modelMinZ
                modelSize = kotlin.math.sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat().coerceAtLeast(1f)

                // Color defaults to high-fidelity slate/teal blue
                meshColor[0] = 0.2f
                meshColor[1] = 0.6f
                meshColor[2] = 0.8f
                meshColor[3] = 1.0f

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

                // Automatically trigger fitAll to view the model centered on first import
                fitAll()
            } else {
                meshIndexCount = 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading CAD mesh: ${e.message}")
        }
    }

    private fun drawCadMesh() {
        if (meshVbo == 0 || meshIbo == 0) return

        // Dynamic visual centering using model-matrix translation while keeping underlying coordinates clean and original!
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -modelCenterX, -modelCenterY, -modelCenterZ)
        
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
