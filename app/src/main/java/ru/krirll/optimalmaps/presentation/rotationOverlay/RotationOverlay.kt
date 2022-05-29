package ru.krirll.optimalmaps.presentation.rotationOverlay

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.gestures.RotationGestureDetector

class RotationOverlay(
    private val mapView: MapView,
    private val onRotationMapListener: (Float) -> Unit
) : Overlay(), RotationGestureDetector.RotationListener {

    private val rotationDetector = RotationGestureDetector(this)

    private var currentAngle = 0f
    private var timeLastSet = 0L
    private val deltaTime = 25L

    override fun onRotate(deltaAngle: Float) {
        currentAngle += deltaAngle
        if (System.currentTimeMillis() - deltaTime > timeLastSet) {
            timeLastSet = System.currentTimeMillis()
            mapView.mapOrientation = mapView.mapOrientation + currentAngle
        }
        onRotationMapListener(mapView.mapOrientation)
    }

    override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
        rotationDetector.onTouch(event)
        return super.onTouchEvent(event, mapView)
    }
}