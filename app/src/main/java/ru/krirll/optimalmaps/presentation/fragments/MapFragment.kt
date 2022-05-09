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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentMapBinding
import ru.krirll.optimalmaps.presentation.other.PointZoom
import ru.krirll.optimalmaps.presentation.other.TitleAlertDialog
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
        initSearchButton()
        initLocationManager()
        initCurrentLocationButton()
        observeViewModel()
    }

    override fun onResume() {
        tryUpdateLocationManager()
        super.onResume()
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
                savedInstanceState.getDoubleArray(CURRENT_MAP_CENTER)?.let { GeoPoint(it[0], it[1]) }
            currentMapZoom = savedInstanceState.getDouble(CURRENT_MAP_ZOOM)
        }
    }

    private fun startShowCurrentLocationProgress() {
        viewBinding.currentLocationProgress.visibility = View.VISIBLE
    }

    private fun stopShowCurrentLocationProgress() {
        viewBinding.currentLocationProgress.visibility = View.GONE
    }

    private fun isCurrentLocationProgressShowing() = viewBinding.currentLocationProgress.isShown

    private fun initLocationManager() {
        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onLocationChanged(location: Location) {
        if (isCurrentLocationProgressShowing())
            stopShowCurrentLocationProgress()
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val currentPositionOverlay = getMarkerById(CURRENT_LOCATION_MARKER)
        if (currentPositionOverlay != null)
            viewBinding.map.overlays.remove(currentPositionOverlay)
        if (isFixedCurrentLocation == true)
            setColor(R.color.purple_500, viewBinding.currentLocationButton.icon)
        addMarkerOnMap(
            geoPoint,
            PointZoom.SMALL_2.zoom,
            CURRENT_LOCATION_MARKER,
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

    private fun initCurrentLocationButton() {
        viewBinding.currentLocationButton.setOnClickListener {
            checkLocationPermissionAndRequestUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryUpdateLocationManager() {
        if (checkSelfPermissionLocation() && isNetworkProvideEnabled() && isGpsProvideEnabled()) {
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
            if (isFixedCurrentLocation == true && getMarkerById(EMPTY_ID) == null)
                startShowCurrentLocationProgress()
        }
    }

    //get overlay (marker) by id
    private fun getMarkerById(id: String) =
        viewBinding.map.overlays.firstOrNull {
            it is Marker && it.id == id
        }?.let { it as Marker }

    private fun changeCurrentLocationMarkerColor(color: Int) {
        getMarkerById(CURRENT_LOCATION_MARKER)?.let {
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

    private fun isGpsProvideEnabled() =
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == ENABLED

    private fun isNetworkProvideEnabled() =
        locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == ENABLED

    private fun checkLocationPermissionAndRequestUpdates() {
        if (checkPermission()) {
            if (isGpsProvideEnabled()) {
                if (isNetworkProvideEnabled()) {
                    alertDialogTitle = null
                    val marker = getMarkerById(CURRENT_LOCATION_MARKER)
                    if (marker != null) {
                        viewBinding.map.controller.apply {
                            setZoom(PointZoom.SMALL_2.zoom)
                            animateTo(GeoPoint(marker.position))
                        }
                        setColor(R.color.purple_500, viewBinding.currentLocationButton.icon)
                    }
                    else {
                        tryUpdateLocationManager()
                        startShowCurrentLocationProgress()
                    }
                    isFixedCurrentLocation = true
                } else
                    createAlertDialogNoInternet()
            } else
                createAlertDialogGeoPositionDisabled()
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
                TitleAlertDialog.INTERNET -> createAlertDialogNoInternet()
                else -> throw RuntimeException("Unknown title $alertDialogTitle")
            }
    }

    private fun createAlertDialogNoInternet() {
        createAlertDialog(
            TitleAlertDialog.INTERNET,
            getString(R.string.no_internet)
        )
    }

    private fun createAlertDialogLocationPermissionDenied() {
        createAlertDialog(
            TitleAlertDialog.LOCATION_PERMISSION_DENIED,
            getString(R.string.location_rationale)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireActivity().packageName, null) //uri of this application settings
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
        shouldAnimate: Boolean = false
    ) {
        //create marker
        val marker = Marker(viewBinding.map).apply {
            position = geoPoint
            //set icon by id
            val drawable =
                when (idString) {
                    CURRENT_LOCATION_MARKER -> {
                        getDrawable(requireContext(), R.drawable.icon_current_location).apply {
                            setColor(R.color.purple_500, this)
                        }
                    }
                    EMPTY_ID -> null
                    else -> null
                }
            icon = drawable
            id = idString
        }
        showPointOnMap(geoPoint, marker, pointZoom, shouldAnimate)
    }

    //observe MapFragmentViewModel LiveData
    private fun observeViewModel() {
        //observe points
        mapViewModel.point.observe(viewLifecycleOwner) { point ->
            //set point on map
            viewBinding.map.apply {
                addMarkerOnMap(
                    GeoPoint(point.lat, point.lon),
                    point.zoom,
                    EMPTY_ID,
                    true
                )
                isFixedCurrentLocation = false
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
            //set min and max zoom
            minZoomLevel = MIN_ZOOM
            maxZoomLevel = MAX_ZOOM
            //set current zoom
            controller.setZoom(currentMapZoom ?: DEFAULT_ZOOM)
            //set current map center
            if (currentMapCenter != null) setExpectedCenter(currentMapCenter)
            //set on touch listener
            setOnTouchListener { view, _ ->
                val currentMapState = view as MapView
                if (!currentMapState.isAnimating) {
                    if (currentMapState.mapCenter.latitude != currentMapCenter?.latitude
                        && currentMapState.mapCenter.longitude != currentMapCenter?.longitude
                    ) {
                        currentMapCenter = GeoPoint(
                            currentMapState.mapCenter.latitude,
                            currentMapState.mapCenter.longitude
                        )
                        if (isFixedCurrentLocation == true) {
                            setColor(R.color.black, viewBinding.currentLocationButton.icon)
                            isFixedCurrentLocation = false
                            stopShowCurrentLocationProgress()
                        }
                    } else
                        if (currentMapState.zoomLevelDouble != currentMapZoom)
                            currentMapZoom = currentMapState.zoomLevelDouble
                }
                false
            }
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
        outState.putBoolean(IS_FIXED_CURRENT_LOCATION, isFixedCurrentLocation ?: false) //save current location state
        outState.putDoubleArray(
            CURRENT_MAP_CENTER,
            doubleArrayOf(currentMapCenter?.latitude ?: 0.0, currentMapCenter?.longitude ?: 0.0) //save current map center
        )
        outState.putDouble(CURRENT_MAP_ZOOM, currentMapZoom ?: DEFAULT_ZOOM) //save current map zoom
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val DEFAULT_ZOOM = 5.0
        private const val MAX_ZOOM = 20.0
        private const val MIN_ZOOM = 4.0
        private const val TITLE = "TITLE"
        private const val ENABLED = true
        private const val DISABLED = false
        private const val CURRENT_LOCATION_MARKER = "CURRENT_LOCATION_MARKER"
        private const val EMPTY_ID = ""
        private const val IS_FIXED_CURRENT_LOCATION = "IS_FIXED_CURRENT_LOCATION"
        private const val CURRENT_MAP_CENTER = "CURRENT_MAP_CENTER"
        private const val CURRENT_MAP_ZOOM = "CURRENT_MAP_ZOOM"
    }
}