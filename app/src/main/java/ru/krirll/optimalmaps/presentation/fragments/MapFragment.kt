package ru.krirll.optimalmaps.presentation.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentMapBinding
import ru.krirll.optimalmaps.presentation.other.TitleAlertDialog
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel

class MapFragment : Fragment() {

    // create/get MapFragmentViewModel in AppActivity context
    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted)
                initLocationButton()
            else {
                createAlertDialogLocationPermissionDenied()
            }
        }

    private var alertDialog: AlertDialog? = null
    private var alertDialogTitle: TitleAlertDialog? = null

    private var currentMapCenter: IGeoPoint? = null //for saving center
    private var currentZoom: Double? = null         //for saving zoom

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
        alertDialogTitle = savedInstanceState?.getParcelable(TITLE)
        createAlertDialogByTitle(alertDialogTitle)
        initMap()
        initSearchButton()
        initLocationButton()
        observeViewModel()
    }

    private fun createAlertDialogByTitle(savedTitle: TitleAlertDialog?) {
        if (savedTitle != null)
            when (alertDialogTitle) {
                TitleAlertDialog.GEO_POSITION_DISABLED -> createAlertDialogGeoPositionDisabled()
                TitleAlertDialog.LOCATION_PERMISSION_DENIED -> createAlertDialogLocationPermissionDenied()
                else -> throw RuntimeException("Unknown title $alertDialogTitle")
            }
    }

    private fun initLocationButton() {
        viewBinding.currentLocationButton.setOnClickListener {
            if (checkSelfPermissionLocation()) {
                if (isLocationEnabled()) {
                    //update current user location on map  and change location button icon
                } else
                    createAlertDialogGeoPositionDisabled()
            } else
                checkPermission()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //check location permission
    private fun checkPermission() {
        when {
            //if granted
             checkSelfPermissionLocation() -> {
                //update current user location on map and change location button icon
            }
            //if was denied and should show rationale
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                createAlertDialogLocationPermissionDenied()
            }
            //if was denied
            else ->
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkSelfPermissionLocation() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun createAlertDialogLocationPermissionDenied() {
        createAlertDialog(
            TitleAlertDialog.LOCATION_PERMISSION_DENIED,
            getString(R.string.location_rationale)
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireActivity().packageName, null)
                )
            )
        }
    }

    private fun createAlertDialogGeoPositionDisabled() {
        createAlertDialog(
            TitleAlertDialog.GEO_POSITION_DISABLED,
            getString(R.string.location_disabled)
        ) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun createAlertDialog(
        title: TitleAlertDialog,
        message: String,
        positiveFunction: () -> Unit
    ) {
        alertDialog =
            AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setTitle(title.stringRes)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    positiveFunction()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .create()
        alertDialog?.show()
        alertDialogTitle = title
    }

    //observe MapFragmentViewModel LiveData
    private fun observeViewModel() {
        //observe point
        mapViewModel.point.observe(viewLifecycleOwner) { point ->
            //set point on map
            viewBinding.map.apply {
                val geoPoint = GeoPoint(point.lat, point.lon)
                controller.setZoom(point.zoom)
                controller.animateTo(geoPoint)
                val startMarker = Marker(this).apply {
                    position = geoPoint
                    //TODO Дополнить информацией диалоговое окно
                }
                overlays.add(startMarker)
                //refresh MapView
                invalidate()
                refreshDrawableState()
            }
        }
    }

    private fun initSearchButton() {
        viewBinding.searchButton.setOnClickListener {
            //navigate to SearchFragment
            findNavController().navigate(R.id.action_mapFragment_to_searchFragment)
        }
    }

    //map settings
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
            controller.setZoom(currentZoom ?: DEFAULT_ZOOM)
            //set current map center
            if (currentMapCenter != null) setExpectedCenter(currentMapCenter)
        }
    }

    override fun onPause() {
        super.onPause()
        viewBinding.map.apply {
            //save current state of map
            currentMapCenter = mapCenter
            currentZoom = zoomLevelDouble
            onPause()
        }
        alertDialog?.dismiss()
    }

    //clean view binding
    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(TITLE, alertDialogTitle) //save title of current alert dialog
        super.onSaveInstanceState(outState)
    }

    companion object {

        private const val DEFAULT_ZOOM = 5.0
        private const val MAX_ZOOM = 20.0
        private const val MIN_ZOOM = 4.0
        private const val TITLE = "TITLE"
    }

}