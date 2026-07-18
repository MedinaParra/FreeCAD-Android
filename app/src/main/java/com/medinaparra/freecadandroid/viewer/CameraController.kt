package com.medinaparra.freecadandroid.viewer

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.sqrt
import kotlin.math.atan

class CameraController {
    var targetX = 0f
    var targetY = 0f
    var targetZ = 0f
    var yawDegrees = 45f
    var pitchDegrees = -45f
    var distance = 150f
    var modelRadius = 10f

    fun handleRotate(dx: Float, dy: Float) {
        yawDegrees += dx * 0.5f
        pitchDegrees += dy * 0.5f
        // Limit pitchDegrees to prevent camera from flipping
        pitchDegrees = pitchDegrees.coerceIn(-85f, 85f)
    }

    fun handleZoom(scale: Float) {
        val s = scale.coerceIn(0.9f, 1.1f)
        distance /= s
        // Dynamic zoom limits based on model size
        val minD = (modelRadius * 0.2f).coerceAtLeast(0.1f)
        val maxD = (modelRadius * 20f).coerceAtLeast(10f)
        distance = distance.coerceIn(minD, maxD)
    }

    fun handlePan(dx: Float, dy: Float) {
        val yawRad = Math.toRadians(yawDegrees.toDouble())
        val pitchRad = Math.toRadians(pitchDegrees.toDouble())

        val rx = -sin(yawRad).toFloat()
        val ry = cos(yawRad).toFloat()
        val rz = 0f

        val ux = -(sin(pitchRad) * cos(yawRad)).toFloat()
        val uy = -(sin(pitchRad) * sin(yawRad)).toFloat()
        val uz = cos(pitchRad).toFloat()

        val factor = distance / 1000f

        targetX -= (dx * rx + dy * ux) * factor
        targetY -= (dx * ry + dy * uy) * factor
        targetZ -= (dx * rz + dy * uz) * factor
    }

    fun fitBounds(
        minX: Float,
        minY: Float,
        minZ: Float,
        maxX: Float,
        maxY: Float,
        maxZ: Float,
        fovYDegrees: Float,
        aspect: Float
    ) {
        targetX = (minX + maxX) / 2f
        targetY = (minY + maxY) / 2f
        targetZ = (minZ + maxZ) / 2f

        val dx = maxX - minX
        val dy = maxY - minY
        val dz = maxZ - minZ
        modelRadius = (sqrt((dx * dx + dy * dy + dz * dz).toDouble()) / 2.0).toFloat().coerceAtLeast(1f)

        val fovRad = Math.toRadians(fovYDegrees.toDouble())
        val halfFovY = fovRad / 2.0
        // Adjust for aspect ratio in landscape or portrait
        val halfFovX = atan(tan(halfFovY) * aspect)
        val halfFov = if (aspect < 1.0f) halfFovX else halfFovY

        distance = (modelRadius / sin(halfFov)).toFloat().coerceIn(1f, 5000f)
    }

    fun reset() {
        yawDegrees = 45f
        pitchDegrees = -45f
        targetX = 0f
        targetY = 0f
        targetZ = 0f
        distance = (modelRadius * 2.5f).coerceIn(10f, 1000f)
    }
}
