package ru.krirll.optimalmaps.presentation.infoWindow

import android.view.View
import android.widget.Button
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.presentation.enums.PointId

class DefaultInfoWindow private constructor(
    layoutResId: Int,
    mapView: MapView
) : MarkerInfoWindow(layoutResId, mapView) {

    private var idMarker: String = PointId.DEFAULT_ID.value

    private fun setOnDeleteListener(func: (String) -> Unit) {
        val button = view.findViewById<Button>(R.id.deleteButton)
        if (idMarker != PointId.CURRENT_LOCATION_ID.value)
            button.setOnClickListener {
                func(idMarker)
            }
        else
            button.visibility = View.GONE
    }

    private fun setOnHideListener(func: ((String) -> Unit)?) {
        val button = view.findViewById<Button>(R.id.hideButton)
        if (func != null)
            button.setOnClickListener {
                func(idMarker)
            }
        else
            button.visibility = View.GONE
    }

    private fun setOnChooseListener(func: ((String) -> Unit)?) {
        val button = view.findViewById<Button>(R.id.chooseButton)
        if (func != null)
            button.setOnClickListener {
                func(idMarker)
            }
        else
            button.visibility = View.GONE
    }

    companion object {
        fun create(
            layoutResId: Int,
            mapView: MapView,
            id: String,
            onDeleteListener: (String) -> Unit,
            onHideListener: ((String) -> Unit)?,
            onChooseListener: ((String) -> Unit)?
        ) =
            DefaultInfoWindow(layoutResId, mapView).apply {
                idMarker = id
                setOnDeleteListener(onDeleteListener)
                setOnHideListener(onHideListener)
                setOnChooseListener(onChooseListener)
            }
    }
}