package ru.krirll.optimalmaps.presentation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentMapPointChoiceBinding
import ru.krirll.optimalmaps.presentation.enums.Locale
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointId
import ru.krirll.optimalmaps.presentation.infoWindow.DefaultInfoWindow
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel
import ru.krirll.optimalmaps.presentation.viewModels.RouteConstructorViewModel

class MapPointChoiceFragment : Fragment() {

    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private val routeConstructorViewModel by lazy {
        ViewModelProvider(requireActivity())[RouteConstructorViewModel::class.java]
    }

    private var _viewBinding: FragmentMapPointChoiceBinding? = null
    private val viewBinding: FragmentMapPointChoiceBinding
        get() = _viewBinding ?: throw RuntimeException("FragmentMapPointChoiceBinding == null")

    private var currentMapCenter: IGeoPoint? = null //for saving center
    private var currentMapZoom: Double? = null         //for saving zoom

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentMapPointChoiceBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getSavedValues(savedInstanceState)
        initMap()
        observeMapViewModel()
        setMapViewModelLocale()
    }

    private fun setMapViewModelLocale() {
        val currentLocale = Locale.getLocale(LocaleListCompat.getDefault()[0].toLanguageTag())
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    //get overlay (marker) by id
    private fun getMarkerById(id: String) =
        viewBinding.mapViewChoice.overlays.firstOrNull {
            it is Marker && it.id == id
        }?.let { it as Marker }

    private fun showPointOnMap(
        geoPoint: GeoPoint,
        marker: Marker,
        pointZoom: Double
    ) {
        //add point on map
        viewBinding.mapViewChoice.apply {
            controller.animateTo(geoPoint)
            controller.setZoom(pointZoom)
            currentMapZoom = pointZoom
            currentMapCenter = geoPoint
            overlays.add(marker)
            invalidate()
        }
    }

    private fun addMarkerOnMap(
        geoPoint: GeoPoint,
        pointZoom: Double,
        text: String,
    ) {
        //create marker
        val marker = Marker(viewBinding.mapViewChoice).apply {
            id = PointId.DEFAULT_ID.value
            position = geoPoint
            title = text
            infoWindow = DefaultInfoWindow.create(
                R.layout.point_info_window,
                viewBinding.mapViewChoice,
                id,
                //On delete event
                { id ->
                    viewBinding.mapViewChoice.overlays.apply {
                        remove(getMarkerById(id).apply { closeInfoWindow() })
                    }
                    mapViewModel.removePoint()
                },
                null,
                //on choose event
                {
                    routeConstructorViewModel.removePointMode()
                    findNavController().popBackStack()
                }
            )
            setOnMarkerClickListener { marker, _ ->
                if (marker.title != "" && !marker.isInfoWindowShown)
                    marker.showInfoWindow()
                else {
                    startProgress()
                    mapViewModel.getPointByLatLon(
                        geoPoint.latitude,
                        geoPoint.longitude,
                        routeConstructorViewModel.getPointMode()
                    )
                }
                viewBinding.mapViewChoice.controller.animateTo(GeoPoint(marker.position))
                true
            }
            showInfoWindow()
        }
        showPointOnMap(geoPoint, marker, pointZoom)
    }

    private fun startProgress() {
        viewBinding.progress.visibility = View.VISIBLE
    }

    private fun stopProgress() {
        viewBinding.progress.visibility = View.GONE
    }

    //observe MapFragmentViewModel
    private fun observeMapViewModel() {
        //observe point
        mapViewModel.point.observe(viewLifecycleOwner) { it ->
            val point = it.second
            //set point on map
            viewBinding.mapViewChoice.apply {
                if (point != null) {
                    getMarkerById(PointId.DEFAULT_ID.value)?.let {
                        it.closeInfoWindow()
                        it.remove(this)
                    }
                    addMarkerOnMap(
                        GeoPoint(point.lat, point.lon),
                        point.zoom,
                        point.text
                    )
                    stopProgress()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //collect errors
                mapViewModel.networkError.collect {
                    val message = when (it) {
                        NetworkError.NO_INTERNET -> getString(R.string.no_internet)
                        NetworkError.NO_INFO_ABOUT_POINT -> getString(R.string.no_info_about_point)
                        else -> ""
                    }
                    createSnackbar(message)
                }
            }
        }
    }

    private fun createSnackbar(message: String) {
        view?.let {
            Snackbar.make(
                it,
                message,
                Snackbar.LENGTH_LONG
            ).apply {
                animationMode = Snackbar.ANIMATION_MODE_SLIDE
            }.show()
        }
    }

    //set map settings
    @SuppressLint("ClickableViewAccessibility")
    private fun initMap() {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        viewBinding.mapViewChoice.apply {
            //set standard kind of map
            setTileSource(TileSourceFactory.MAPNIK)
            //hide zoom buttons
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            //access zooming by fingers
            setMultiTouchControls(true)
            //set min and max zoom
            minZoomLevel = MIN_ZOOM
            maxZoomLevel = MAX_ZOOM
            //set current zoom
            controller.setZoom(currentMapZoom ?: DEFAULT_ZOOM)
            //set current map center
            if (currentMapCenter != null) setExpectedCenter(currentMapCenter)
            //set on touch listener
            setOnTouchListener { view, _ ->
                val currentMap = view as MapView
                if (!currentMap.isAnimating && currentMap.isInTouchMode) {
                    currentMapCenter = GeoPoint(
                        currentMap.mapCenter.latitude,
                        currentMap.mapCenter.longitude
                    )
                    currentMapZoom = currentMap.zoomLevelDouble
                }
                false
            }
            //set on long click listener with search
            val eventReceiver =
                object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint?) = false
                    override fun longPressHelper(point: GeoPoint?): Boolean {
                        point?.let {
                            mapViewModel.getPointByLatLon(
                                it.latitude,
                                it.longitude,
                                routeConstructorViewModel.getPointMode()
                            )
                        }
                        return false
                    }
                }
            overlays.add(object : MapEventsOverlay(eventReceiver) { /*body is empty*/ })
        }
    }

    override fun onPause() {
        viewBinding.mapViewChoice.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    private fun getSavedValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            //get current map center and zoom
            currentMapCenter =
                savedInstanceState.getDoubleArray(CURRENT_MAP_CENTER)
                    ?.let { GeoPoint(it[0], it[1]) }
            currentMapZoom = savedInstanceState.getDouble(CURRENT_MAP_ZOOM)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putDoubleArray(
            CURRENT_MAP_CENTER,
            doubleArrayOf(
                currentMapCenter?.latitude ?: 0.0,
                currentMapCenter?.longitude ?: 0.0
            ) //save current map center
        )
        outState.putDouble(CURRENT_MAP_ZOOM, currentMapZoom ?: DEFAULT_ZOOM) //save current map zoom
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val DEFAULT_ZOOM = 5.0
        private const val MAX_ZOOM = 20.0
        private const val MIN_ZOOM = 4.0
        private const val CURRENT_MAP_CENTER = "CURRENT_MAP_CENTER"
        private const val CURRENT_MAP_ZOOM = "CURRENT_MAP_ZOOM"
    }
}