package ru.krirll.optimalmaps.presentation.infoWindow

import android.view.View
import android.widget.Button
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.presentation.fragments.MapFragment

class DefaultInfoWindow private constructor(
    layoutResId: Int,
    mapView: MapView
) : MarkerInfoWindow(layoutResId, mapView) {

    private var idMarker: String = MapFragment.DEFAULT_ID

    private fun setOnDeleteListener(func: (String) -> Unit) {
        val button = view.findViewById<Button>(R.id.deleteButton)
        if (idMarker != MapFragment.CURRENT_LOCATION_MARKER)
            button.setOnClickListener {
                func(idMarker)
            }
        else
            button.visibility = View.GONE
    }

    private fun setOnHideListener(func: (String) -> Unit) {
        view.findViewById<Button>(R.id.hideButton).setOnClickListener {
            func(idMarker)
        }
    }

    companion object {
        fun create(
            layoutResId: Int,
            mapView: MapView,
            id: String,
            onDeleteListener: (String) -> Unit,
            onHideListener: (String) -> Unit
        ) =
            DefaultInfoWindow(layoutResId, mapView).apply {
                idMarker = id
                setOnDeleteListener(onDeleteListener)
                setOnHideListener(onHideListener)
            }
    }
}