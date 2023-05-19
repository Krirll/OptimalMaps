package ru.krirll.optimalmaps.presentation.fragments

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat.getColor
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentMapBinding
import ru.krirll.optimalmaps.presentation.enums.Locale
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointId
import ru.krirll.optimalmaps.presentation.enums.PointMode
import ru.krirll.optimalmaps.presentation.enums.PointZoom
import ru.krirll.optimalmaps.presentation.enums.RouteMode
import ru.krirll.optimalmaps.presentation.enums.TitleAlertDialog
import ru.krirll.optimalmaps.presentation.infoWindow.DefaultInfoWindow
import ru.krirll.optimalmaps.presentation.rotationOverlay.RotationOverlay
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel

class MapFragment: BaseFragmentLocationSupport() {

    // create/get MapFragmentViewModel in AppActivity context
    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private var isCurrentLocationInfoWindowOpened = false

    private var currentMapCenter: GeoPoint? = null //for saving center
    private var currentMapZoom: Double? = null         //for saving zoom
    private var isFixedCurrentLocation: Boolean? = null //for saving current location state
    private var mapOrientation: Float? = null

    private var _viewBinding: FragmentMapBinding? = null
    private val viewBinding: FragmentMapBinding
        get() = _viewBinding ?: throw RuntimeException("FragmentMapBinding == null")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentMapBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getSavedValues(savedInstanceState)
        initMap()
        setViewModelLocale()
        initSearchButton()
        initRouteButton()
        initCancelButton()
        initCompass()
        initCurrentLocationButton()
        observeViewModel()
    }

    override fun onResume() {
        startService()
        tryUpdateLocation()
        super.onResume()
    }

    private fun initCompass() {
        viewBinding.compass.setOnClickListener {
            viewBinding.map.mapOrientation = 0.0f
            viewBinding.compass.visibility = View.GONE
        }
    }

    private fun initRouteButton() {
        //open route constructor
        viewBinding.routeButton.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_routeConstructorFragment)
        }
    }

    private fun initCancelButton() {
        viewBinding.cancelButton.setOnClickListener {
            mapViewModel.removeRoute()
            findNavController().navigate(R.id.action_mapFragment_to_routeConstructorFragment)
        }
    }

    private fun setViewModelLocale() {
        val currentLocale = Locale.getLocale(LocaleListCompat.getDefault()[0]!!.toLanguageTag())
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    private fun getSavedValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            //get saved title of alert dialog
            alertDialogTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                savedInstanceState.getParcelable(TITLE, TitleAlertDialog::class.java)
            else
                savedInstanceState.getParcelable(TITLE)

            alertDialogTitle?.let { createAlertDialogByTitle(it) }
            //get current location state
            isFixedCurrentLocation = savedInstanceState.getBoolean(IS_FIXED_CURRENT_LOCATION)
            //get current map center and zoom
            currentMapCenter =
                savedInstanceState.getDoubleArray(CURRENT_MAP_CENTER)
                    ?.let { GeoPoint(it[0], it[1]) }
            currentMapZoom = savedInstanceState.getDouble(CURRENT_MAP_ZOOM)
            //get current location info window state
            isCurrentLocationInfoWindowOpened = savedInstanceState.getBoolean(IS_OPENED, false)
            mapOrientation = savedInstanceState.getFloat(MAP_ROTATION, 0.0f)
            if (mapOrientation != 0.0f) {
                viewBinding.compass.visibility = View.VISIBLE
                viewBinding.map.mapOrientation = mapOrientation ?: 0.0f
            }
        }
    }

    private fun startProgress() {
        viewBinding.progress.visibility = View.VISIBLE
    }

    private fun stopProgress() {
        viewBinding.progress.visibility = View.GONE
    }

    override fun updateLocation(location: Location) {
        stopProgress()
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val currentPositionMarker = getMarkerById(PointId.CURRENT_LOCATION_ID.value)
        if (currentPositionMarker != null) {
            if (currentPositionMarker.isInfoWindowShown)
                currentPositionMarker.closeInfoWindow()
            viewBinding.map.overlays.remove(currentPositionMarker)
        }
        if (isFixedCurrentLocation == true)
            setColor(R.color.purple_500, viewBinding.currentLocationButton.icon)
        addMarkerOnMap(
            geoPoint,
            viewBinding.map.zoomLevelDouble,
            PointId.CURRENT_LOCATION_ID.value,
            "",
            isFixedCurrentLocation ?: false
        )
        if (mapViewModel.route.value?.second == RouteMode.NAVIGATION_ON_MAP_MODE) {
            viewBinding.navDescriptionBlock.visibility = View.VISIBLE
            mapViewModel.route.value?.first?.mNodes?.get(mapViewModel.getCurrentIndexNode())?.let {
                updateNavigationInstructions(
                    geoPoint,
                    it
                )
            }
        }
    }

    private fun updateNavigationInstructions(position: GeoPoint, node: RoadNode) {
        val length = getLengthFromPositionToNode(position, node.mLocation)
        val icons = resources.obtainTypedArray(R.array.directions_icons)
        val iconId = icons.getResourceId(node.mManeuverType, R.drawable.ic_empty)
        viewBinding.apply {
            directionTitle.text =
                if (node.mInstructions != null) node.mInstructions else getString(R.string.move_line)
            lengthDirection.text = getFormattedLengthString(length)
            directionImage.setImageDrawable(
                getDrawable(
                    requireContext(),
                    if (iconId == R.drawable.ic_empty) R.drawable.ic_continue else iconId
                )
            )
        }
        icons.recycle()
        if (length.toInt() <= 15) {
            mapViewModel.removeLastNode()
            viewBinding.map.apply {
                overlays.add(mapViewModel.route.value?.first?.mRouteHigh?.let { it ->
                    overlays.remove(overlays.find { it is Polyline })
                    buildPolyline(it)
                })
            }
            if (mapViewModel.getCurrentIndexNode() == mapViewModel.route.value?.first?.mNodes?.size) {
                mapViewModel.removeRoute()
                viewBinding.navDescriptionBlock.visibility = View.GONE
            }
        }
    }

    private fun getFormattedLengthString(length: Float): String =
        if (length >= 1000.0) {
            getString(
                R.string.format_distance_kilometers,
                (length / 1000).toInt().toString()
            )
        } else {
            getString(
                R.string.format_distance_meters,
                length.toInt().toString()
            )
        }

    private fun getLengthFromPositionToNode(position: GeoPoint, node: GeoPoint): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            position.latitude,
            position.longitude,
            node.latitude,
            node.longitude,
            result
        )
        return result[0]
    }

    override fun setStateProviderDisabled() {
        changeCurrentLocationMarkerColor(R.color.black)
        changeLocationButtonIcon(DISABLED)
        createSnackbar(getString(R.string.no_location))
    }

    override fun setStateProviderEnabled() {
        changeCurrentLocationMarkerColor(R.color.purple_500)
        changeLocationButtonIcon(ENABLED)
    }

    private fun initCurrentLocationButton() {
        viewBinding.currentLocationButton.setOnClickListener {
            tryUpdateCurrentLocationMarker()
        }
    }

    private fun tryUpdateLocation() {
        alertDialogTitle = null
        locationService?.tryUpdateLocation()
        changeLocationButtonIcon(ENABLED)
        if (isFixedCurrentLocation == true && getMarkerById(PointId.DEFAULT_ID.value) == null)
            startProgress()
    }

    //get overlay (marker) by id
    private fun getMarkerById(id: String) =
        viewBinding.map.overlays.firstOrNull {
            it is Marker && it.id == id
        }?.let { it as Marker }

    private fun changeCurrentLocationMarkerColor(color: Int) {
        getMarkerById(PointId.CURRENT_LOCATION_ID.value)?.let {
            setColor(color, it.icon)
        }
        viewBinding.map.invalidate()
    }

    private fun changeLocationButtonIcon(currentLocationState: Boolean) {
        viewBinding.currentLocationButton.icon =
            when (currentLocationState) {
                ENABLED -> getDrawable(requireContext(), R.drawable.icon_fix_location)
                DISABLED -> getDrawable(requireContext(), R.drawable.icon_no_location)
                else -> null
            }
        viewBinding.map.invalidate()
    }

    //set color for drawable
    private fun setColor(color: Int, drawable: Drawable?) {
        drawable?.setTint(getColor(requireContext(), color))
    }

    //todo в отдельном классе и абстрактно
    private fun tryUpdateCurrentLocationMarker() {
        alertDialogTitle = null
        val marker = getMarkerById(PointId.CURRENT_LOCATION_ID.value)
        if (marker != null) {
            viewBinding.map.controller.apply {
                setZoom(PointZoom.SMALL_1.zoom)
                animateTo(GeoPoint(marker.position))
            }
            setColor(R.color.purple_500, viewBinding.currentLocationButton.icon)
        }
        else {
            tryUpdateLocation()
            startProgress()
        }
        isFixedCurrentLocation = true
    }

    private fun showPointOnMap(
        geoPoint: GeoPoint,
        marker: Marker,
        pointZoom: Double,
        shouldAnimate: Boolean = false
    ) {
        //add point on map
        viewBinding.map.apply {
            if (shouldAnimate) {
                controller.animateTo(geoPoint)
                controller.setZoom(pointZoom)
                currentMapZoom = pointZoom
                currentMapCenter = geoPoint
            }
            overlays.add(marker)
            invalidate()
        }
    }

    private fun addMarkerOnMap(
        geoPoint: GeoPoint,
        pointZoom: Double,
        idString: String,
        text: String,
        shouldAnimate: Boolean = false
    ) {
        //create marker
        val marker = Marker(viewBinding.map).apply {
            position = geoPoint
            //set icon by id
            val drawable =
                when (idString) {
                    PointId.CURRENT_LOCATION_ID.value -> {
                        getDrawable(requireContext(), R.drawable.icon_current_location).apply {
                            setColor(R.color.purple_500, this)
                        }
                    }
                    PointId.DEFAULT_ID.value -> null
                    else -> null
                }
            title = text
            icon = drawable
            id = idString
            infoWindow = DefaultInfoWindow.create(
                R.layout.point_info_window,
                viewBinding.map,
                idString,
                //On delete event
                { id ->
                    viewBinding.map.overlays.apply {
                        remove(getMarkerById(id).apply { closeInfoWindow() })
                    }
                    mapViewModel.removePoint()
                },
                //On hide event
                {
                    getMarkerById(id)?.closeInfoWindow()
                    if (id == PointId.CURRENT_LOCATION_ID.value)
                        isCurrentLocationInfoWindowOpened = false
                },
                null //no choose listener
            )
            if (isCurrentLocationInfoWindowOpened) {
                getMarkerAddress(geoPoint)
            }
            if (idString != PointId.CURRENT_LOCATION_ID.value)
                showInfoWindow()
            else {
                setOnMarkerClickListener { marker, _ ->
                    if (marker.title != "" && !marker.isInfoWindowShown)
                        marker.showInfoWindow()
                    else {
                        getMarkerAddress(geoPoint)
                    }
                    viewBinding.map.controller.animateTo(GeoPoint(marker.position))
                    isCurrentLocationInfoWindowOpened = true
                    true
                }
            }
        }
        showPointOnMap(geoPoint, marker, pointZoom, shouldAnimate)
    }

    private fun getMarkerAddress(geoPoint: GeoPoint) {
        startProgress()
        mapViewModel.getPointByLatLon(
            geoPoint.latitude,
            geoPoint.longitude,
            PointMode.CURRENT_LOCATION_POINT
        )
    }

    private fun setVisibilityForShowMode(visibility: Int) {
        viewBinding.apply {
            searchButton.visibility = visibility
            routeButton.visibility = visibility
            currentLocationButton.visibility = visibility
            cancelButton.visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
            lengthDurationShow.visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun setVisibilityForNavigationMode(visibility: Int) {
        viewBinding.apply {
            routeButton.visibility = visibility
            searchButton.visibility = visibility
            cancelButton.visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun showRoute(route: Road, mode: RouteMode) {
        for (i in 0 until route.mNodes.size) {
            val node: RoadNode = route.mNodes[i]
            if (node.mManeuverType == 24) {
                val nodeMarker = Marker(viewBinding.map)
                nodeMarker.position = node.mLocation
                nodeMarker.icon = when (i) {
                    0 -> getDrawable(requireContext(), R.drawable.icon_route)
                    route.mNodes.size - 1 -> getDrawable(requireContext(), R.drawable.icon_finish)
                    else -> getDrawable(requireContext(), R.drawable.icon_additional_point)
                }.apply { this?.setTint(getColor(requireContext(), R.color.black)) }
                nodeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                nodeMarker.setOnMarkerClickListener { _, _ -> true }
                viewBinding.map.overlays.add(nodeMarker)
                viewBinding.map.invalidate()
            }
        }
        var over: Polyline
        viewBinding.map.apply {
            if (mode == RouteMode.SHOW_ON_MAP_MODE) {
                over = RoadManager.buildRoadOverlay(route)
                currentMapCenter = GeoPoint(over.bounds.centerLatitude, over.bounds.centerLongitude)
                currentMapZoom = PointZoom.getZoomByRouteLength(route.mLength)
                controller.setCenter(currentMapCenter)
                controller.setZoom(currentMapZoom!!)
                viewBinding.lengthDurationShow.text = getString(
                    R.string.duration_length,
                    Road.getLengthDurationText(requireContext(), route.mLength, route.mDuration)
                )
            } else {
                over = buildPolyline(mapViewModel.route.value?.first?.mRouteHigh!!)
                val index = mapViewModel.getCurrentIndexNode()
                if (index != mapViewModel.route.value?.first?.mNodes?.size)
                    currentMapCenter = mapViewModel.route.value?.first?.mNodes?.get(index - 1)?.mLocation
                currentMapZoom = PointZoom.SMALL_2.zoom
                controller.setCenter(currentMapCenter)
                controller.setZoom(currentMapZoom!!)
            }
            overlays.add(over)
            invalidate()
        }
    }

    private fun buildPolyline(points: List<GeoPoint>): Polyline {
        return Polyline().apply {
            with(outlinePaint) {
                color = getColor(requireContext(), R.color.purple_500)
                outlinePaint.strokeWidth = 6.0f
            }
            setPoints(points)
        }
    }

    //observe MapFragmentViewModel LiveData
    private fun observeViewModel() {
        //observe point
        mapViewModel.point.observe(viewLifecycleOwner) { it ->
            val point = it.second
            stopProgress()
            //set point on map
            viewBinding.map.apply {
                if (point != null) {
                    getMarkerById(PointId.DEFAULT_ID.value)?.let {
                        it.closeInfoWindow()
                        it.remove(this)
                    }
                    addMarkerOnMap(
                        GeoPoint(point.lat, point.lon),
                        point.zoom,
                        PointId.DEFAULT_ID.value,
                        point.text,
                        true
                    )
                    isFixedCurrentLocation = false
                }
            }
        }
        //observe current location title
        mapViewModel.currentLocationPointTitle.observe(viewLifecycleOwner) { title ->
            if (title != null) {
                getMarkerById(PointId.CURRENT_LOCATION_ID.value)?.let {
                    //set title and open info window
                    it.title = title
                    it.showInfoWindow()
                    stopProgress()
                }
            }
        }
        mapViewModel.route.observe(viewLifecycleOwner) { route ->
            if (route.first != null) {
                if (route.second != null) {
                    when (route.second!!) { //route mode
                        RouteMode.SHOW_ON_MAP_MODE -> {
                            setVisibilityForShowMode(View.GONE)
                            showRoute(route.first!!, RouteMode.SHOW_ON_MAP_MODE)
                        }
                        RouteMode.NAVIGATION_ON_MAP_MODE -> {
                            setVisibilityForNavigationMode(View.GONE)
                            showRoute(route.first!!, RouteMode.NAVIGATION_ON_MAP_MODE)
                        }
                    }
                }
            } else {
                if (viewBinding.lengthDurationShow.visibility == View.VISIBLE)
                    setVisibilityForShowMode(View.VISIBLE)
                if (viewBinding.navDescriptionBlock.visibility == View.VISIBLE)
                    setVisibilityForNavigationMode(View.VISIBLE)
                //delete all points from map
                viewBinding.map.overlays.clear()
                viewBinding.map.invalidate()
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
                    stopProgress()
                    createSnackbar(message)
                }
            }
        }
    }

    private fun initSearchButton() {
        viewBinding.searchButton.setOnClickListener {
            //navigate to SearchFragment
            findNavController().navigate(R.id.action_mapFragment_to_searchFragment)
        }
    }

    //set map settings
    @SuppressLint("ClickableViewAccessibility")
    private fun initMap() {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        viewBinding.map.apply {
            //set standard kind of map
            setTileSource(TileSourceFactory.MAPNIK)
            //hide zoom buttons
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            //access zooming by fingers
            setMultiTouchControls(true)
            //add rotation with by fingers
            overlays.add(RotationOverlay(this) {
                if (it > 1f || it < -1f)
                    viewBinding.compass.visibility = View.VISIBLE
                else
                    viewBinding.compass.visibility = View.GONE
            })
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
                    if (isFixedCurrentLocation == true) {
                        setColor(R.color.black, viewBinding.currentLocationButton.icon)
                        isFixedCurrentLocation = false
                        stopProgress()
                    }
                }
                false
            }
            //set on long click listener with search
            val eventReceiver =
                object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint?) = false
                    override fun longPressHelper(point: GeoPoint?): Boolean {
                        point?.let {
                            startProgress()
                            mapViewModel.getPointByLatLon(
                                it.latitude,
                                it.longitude,
                                PointMode.DEFAULT_POINT
                            )
                        }
                        return false
                    }
                }
            overlays.add(object : MapEventsOverlay(eventReceiver) { /*body is empty*/ })
        }
    }

    override fun onPause() {
        super.onPause()
        viewBinding.map.onPause()
        alertDialog?.dismiss()
        mapOrientation = viewBinding.map.mapOrientation
    }

    //clean view binding
    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(TITLE, alertDialogTitle) //save title of current alert dialog
        outState.putBoolean(
            IS_FIXED_CURRENT_LOCATION,
            isFixedCurrentLocation ?: false
        ) //save current location state
        outState.putDoubleArray(
            CURRENT_MAP_CENTER,
            doubleArrayOf(
                currentMapCenter?.latitude ?: 0.0,
                currentMapCenter?.longitude ?: 0.0
            ) //save current map center
        )
        outState.putDouble(CURRENT_MAP_ZOOM, currentMapZoom ?: DEFAULT_ZOOM) //save current map zoom
        outState.putBoolean(IS_OPENED, isCurrentLocationInfoWindowOpened)
        outState.putFloat(MAP_ROTATION, mapOrientation ?: 0.0f)
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val DEFAULT_ZOOM = 5.0
        private const val MAX_ZOOM = 20.0
        private const val MIN_ZOOM = 4.0
        private const val TITLE = "TITLE"
        private const val ENABLED = true
        private const val DISABLED = false
        private const val IS_OPENED = "IS_OPENED"
        private const val IS_FIXED_CURRENT_LOCATION = "IS_FIXED_CURRENT_LOCATION"
        private const val CURRENT_MAP_CENTER = "CURRENT_MAP_CENTER"
        private const val CURRENT_MAP_ZOOM = "CURRENT_MAP_ZOOM"
        private const val MAP_ROTATION = "MAP_ROTATION"
    }
}