package ru.krirll.optimalmaps.presentation.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
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
import ru.krirll.optimalmaps.presentation.enums.*
import ru.krirll.optimalmaps.presentation.infoWindow.DefaultInfoWindow
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel

class MapFragment : Fragment(), LocationListener {

    // create/get MapFragmentViewModel in AppActivity context
    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted)
                createAlertDialogLocationPermissionDenied()
            else
                tryUpdateLocationManager()
        }

    private var alertDialog: AlertDialog? = null
    private var alertDialogTitle: TitleAlertDialog? = null

    private var locationManager: LocationManager? = null
    private var isCurrentLocationInfoWindowOpened = false

    private var currentMapCenter: IGeoPoint? = null //for saving center
    private var currentMapZoom: Double? = null         //for saving zoom
    private var isFixedCurrentLocation: Boolean? = null //for saving current location state

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
        initLocationManager()
        initCurrentLocationButton()
        observeViewModel()
    }

    override fun onResume() {
        tryUpdateLocationManager()
        super.onResume()
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
        val currentLocale = Locale.getLocale(LocaleListCompat.getDefault()[0].toLanguageTag())
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    private fun getSavedValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            //get saved title of alert dialog
            alertDialogTitle = savedInstanceState.getParcelable(TITLE)
            createAlertDialogByTitle(alertDialogTitle)
            //get current location state
            isFixedCurrentLocation = savedInstanceState.getBoolean(IS_FIXED_CURRENT_LOCATION)
            //get current map center and zoom
            currentMapCenter =
                savedInstanceState.getDoubleArray(CURRENT_MAP_CENTER)
                    ?.let { GeoPoint(it[0], it[1]) }
            currentMapZoom = savedInstanceState.getDouble(CURRENT_MAP_ZOOM)
            //get current location info window state
            isCurrentLocationInfoWindowOpened = savedInstanceState.getBoolean(IS_OPENED, false)
        }
    }

    private fun startProgress() {
        viewBinding.progress.visibility = View.VISIBLE
    }

    private fun stopProgress() {
        viewBinding.progress.visibility = View.GONE
    }

    private fun initLocationManager() {
        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onLocationChanged(location: Location) {
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
            PointZoom.SMALL_1.zoom,
            PointId.CURRENT_LOCATION_ID.value,
            "",
            isFixedCurrentLocation ?: false
        )
    }

    override fun onProviderDisabled(provider: String) {
        changeCurrentLocationMarkerColor(R.color.black)
        changeLocationButtonIcon(DISABLED)
    }

    override fun onProviderEnabled(provider: String) {
        changeCurrentLocationMarkerColor(R.color.purple_500)
        changeLocationButtonIcon(ENABLED)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //this method must me override because minSDK is 22
    }

    private fun initCurrentLocationButton() {
        viewBinding.currentLocationButton.setOnClickListener {
            checkLocationPermissionAndRequestUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryUpdateLocationManager() {
        if (checkSelfPermissionLocation() && getLocationMode() == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
            alertDialogTitle = null
            locationManager?.removeUpdates(this)
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                0.5F,
                this
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                500L,
                0.5F,
                this
            )
            changeLocationButtonIcon(ENABLED)
            if (isFixedCurrentLocation == true && getMarkerById(PointId.DEFAULT_ID.value) == null)
                startProgress()
        }
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

    private fun getLocationMode(): Int =
        Settings.Secure.getInt(requireActivity().contentResolver, Settings.Secure.LOCATION_MODE)

    private fun checkLocationPermissionAndRequestUpdates() {
        if (checkPermission()) {
            when (getLocationMode()) {
                Settings.Secure.LOCATION_MODE_OFF -> createAlertDialogGeoPositionDisabled()
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY -> {
                    alertDialogTitle = null
                    val marker = getMarkerById(PointId.CURRENT_LOCATION_ID.value)
                    if (marker != null) {
                        viewBinding.map.controller.apply {
                            setZoom(PointZoom.SMALL_1.zoom)
                            animateTo(GeoPoint(marker.position))
                        }
                        setColor(R.color.purple_500, viewBinding.currentLocationButton.icon)
                    } else {
                        tryUpdateLocationManager()
                        startProgress()
                    }
                    isFixedCurrentLocation = true
                }
                else -> {
                    createAlertDialogChangeLocationMode()
                }
            }
        }
    }

    //check location permission
    private fun checkPermission(): Boolean {
        var result = false
        when {
            //if granted
            checkSelfPermissionLocation() -> {
                result = true
            }
            //if was denied and should show rationale
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                createAlertDialogLocationPermissionDenied()
            }
            //if was denied
            else ->
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return result
    }

    private fun checkSelfPermissionLocation() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun createAlertDialogByTitle(savedTitle: TitleAlertDialog?) {
        if (savedTitle != null)
            when (alertDialogTitle) {
                TitleAlertDialog.GEO_POSITION_DISABLED -> createAlertDialogGeoPositionDisabled()
                TitleAlertDialog.LOCATION_PERMISSION_DENIED -> createAlertDialogLocationPermissionDenied()
                TitleAlertDialog.CHANGE_LOCATION_MODE -> createAlertDialogChangeLocationMode()
                else -> throw RuntimeException("Unknown title $alertDialogTitle")
            }
    }

    private fun createAlertDialogChangeLocationMode() {
        createAlertDialog(
            TitleAlertDialog.CHANGE_LOCATION_MODE,
            getString(R.string.change_location_mode)
        ) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun createAlertDialogLocationPermissionDenied() {
        createAlertDialog(
            TitleAlertDialog.LOCATION_PERMISSION_DENIED,
            getString(R.string.location_rationale)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts(
                        "package",
                        requireActivity().packageName,
                        null
                    ) //uri of this application settings
                )
            )
        }
    }

    private fun createAlertDialogGeoPositionDisabled() {
        createAlertDialog(
            TitleAlertDialog.GEO_POSITION_DISABLED,
            getString(R.string.location_disabled)
        ) {
            //open location settings
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun createAlertDialog(
        title: TitleAlertDialog,
        message: String,
        positiveFunction: (() -> Unit)? = null
    ) {
        alertDialog =
            AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setTitle(title.stringRes)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    alertDialogTitle = null
                    positiveFunction?.invoke()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    alertDialogTitle = null
                }
                .create()
        alertDialog?.show()
        alertDialogTitle = title
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
                startProgress()
                mapViewModel.getPointByLatLon(
                    geoPoint.latitude,
                    geoPoint.longitude,
                    PointMode.CURRENT_LOCATION_POINT
                )
            }
            if (idString != PointId.CURRENT_LOCATION_ID.value)
                showInfoWindow()
            else {
                setOnMarkerClickListener { marker, _ ->
                    if (marker.title != "" && !marker.isInfoWindowShown)
                        marker.showInfoWindow()
                    else {
                        startProgress()
                        mapViewModel.getPointByLatLon(
                            geoPoint.latitude,
                            geoPoint.longitude,
                            PointMode.CURRENT_LOCATION_POINT
                        )
                    }
                    viewBinding.map.controller.animateTo(GeoPoint(marker.position))
                    isCurrentLocationInfoWindowOpened = true
                    true
                }
            }
        }
        showPointOnMap(geoPoint, marker, pointZoom, shouldAnimate)
    }

    private fun setVisibility(visibility: Int) {
        viewBinding.apply {
            searchButton.visibility = visibility
            routeButton.visibility = visibility
            currentLocationButton.visibility = visibility
            cancelButton.visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
            lengthDurationShow.visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun showRoute(route: Road) {
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
            }
        }
        val over = RoadManager.buildRoadOverlay(route).apply { isGeodesic = true }
        viewBinding.map.apply {
            currentMapCenter = GeoPoint(over.bounds.centerLatitude, over.bounds.centerLongitude)
            currentMapZoom = PointZoom.SMALL_3.zoom
            controller.setCenter(currentMapCenter)
            controller.setZoom(currentMapZoom!!)
            overlays.add(over)
            invalidate()
        }
        viewBinding.lengthDurationShow.text = getString(
            R.string.duration_length,
            Road.getLengthDurationText(requireContext(), route.mLength, route.mDuration)
        )
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
                    when (route.second) { //route mode
                        RouteMode.SHOW_ON_MAP_MODE -> {
                            setVisibility(View.GONE)
                            showRoute(route.first!!)
                        }
                        RouteMode.NAVIGATION_ON_MAP_MODE -> {
                            //навигатор
                        }
                        else -> {
                            /*nothing*/
                        }
                    }
                }
            } else {
                setVisibility(View.VISIBLE)
                //delete all points from map
                viewBinding.map.overlays.removeAll(
                    viewBinding.map.overlays.filter { it is Marker && it.id == null }
                )
                viewBinding.map.invalidate()
                //delete route line from map
                viewBinding.map.overlays.apply {
                    remove(firstOrNull { it is Polyline })
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
                    stopProgress()
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
        viewBinding.map.onPause()
        locationManager?.removeUpdates(this)
        alertDialog?.dismiss()
        super.onPause()
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
    }
}