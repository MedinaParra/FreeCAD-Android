package com.medinaparra.freecadandroid.viewer

class CameraController {
    var angleX = -45f
    var angleY = 45f
    var distance = 150f
    var panX = 0f
    var panY = 0f

    fun handleRotate(dx: Float, dy: Float) {
        angleX += dy * 0.5f
        angleY += dx * 0.5f
    }

    fun handleZoom(scale: Float) {
        // Clamp scale multiplier to reasonable ranges
        val s = scale.coerceIn(0.9f, 1.1f)
        distance /= s
        distance = distance.coerceIn(10f, 1000f)
    }

    fun handlePan(dx: Float, dy: Float) {
        val factor = distance / 1000f
        panX += dx * factor
        panY -= dy * factor
    }

    fun reset() {
        angleX = -45f
        angleY = 45f
        distance = 150f
        panX = 0f
        panY = 0f
    }
}
