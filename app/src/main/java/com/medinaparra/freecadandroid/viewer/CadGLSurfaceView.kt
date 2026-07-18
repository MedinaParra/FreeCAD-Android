package com.medinaparra.freecadandroid.viewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.sqrt

class CadGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val cameraController = CameraController()
    val renderer = CadRenderer(cameraController)

    private var previousX = 0f
    private var previousY = 0f
    private var previousDist = 1f
    private var isPinching = false

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousX = x
                previousY = y
                isPinching = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    previousDist = getSpacing(event)
                    isPinching = true
                    previousX = (event.getX(0) + event.getX(1)) / 2f
                    previousY = (event.getY(0) + event.getY(1)) / 2f
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !isPinching) {
                    // Rotate camera
                    val dx = x - previousX
                    val dy = y - previousY
                    cameraController.handleRotate(dx, dy)
                    requestRender()
                } else if (event.pointerCount == 2) {
                    // Zoom camera
                    val newDist = getSpacing(event)
                    if (newDist > 10f) {
                        val scale = newDist / previousDist
                        cameraController.handleZoom(scale)
                        previousDist = newDist
                    }

                    // Pan camera
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val dx = midX - previousX
                    val dy = midY - previousY
                    cameraController.handlePan(dx, dy)
                    previousX = midX
                    previousY = midY

                    requestRender()
                }
                if (event.pointerCount == 1) {
                    previousX = x
                    previousY = y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                isPinching = false
            }
        }
        return true
    }

    private fun getSpacing(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
