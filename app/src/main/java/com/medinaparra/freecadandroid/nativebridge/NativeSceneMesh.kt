package com.medinaparra.freecadandroid.nativebridge

import java.nio.ByteBuffer

data class NativeSceneMesh(
    val vertexBuffer: ByteBuffer?,
    val indexBuffer: ByteBuffer?,
    val vertexCount: Int,
    val indexCount: Int,
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
)
