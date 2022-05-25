package ru.krirll.optimalmaps.presentation.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentRouteConstructorBinding
import ru.krirll.optimalmaps.presentation.dialogFragment.RoutePointDialog
import ru.krirll.optimalmaps.presentation.enums.*
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel
import ru.krirll.optimalmaps.presentation.viewModels.RouteConstructorViewModel

class RouteConstructorFragment : Fragment(), LocationListener {

    private var _viewBinding: FragmentRouteConstructorBinding? = null
    private val viewBinding: FragmentRouteConstructorBinding
        get() = _viewBinding ?: throw RuntimeException("FragmentRouteConstructorBinding == null")

    private val routeConstructorViewModel by lazy {
        ViewModelProvider(requireActivity())[RouteConstructorViewModel::class.java]
    }

    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted)
                createAlertDialogLocationPermissionDenied()
        }

    private var dialog: RoutePointDialog? = null
    private var dialogMode: PointMode? = null

    private var alertDialog: AlertDialog? = null
    private var alertDialogTitle: TitleAlertDialog? = null

    private val currentLocale by lazy {
        Locale.getLocale(LocaleListCompat.getDefault()[0].toLanguageTag())
    }

    private var locationManager: LocationManager? = null
    private var isCurrentLocationEvent: Boolean = false
    private var isShowingProgress: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentRouteConstructorBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getSavedValues(savedInstanceState)
        initLocationManager()
        initBackButton()
        initStartButton()
        initShowOnMapButton()
        initShowSavedRoutesButton()
        initStartPointLayout()
        initAdditionalPointsLayout()
        initFinishPointLayout()
        setMapViewModelLocale()
        setSearchViewModelLocale()
        observeMapViewModel()
        observeRouteConstructorViewModel()
    }

    private fun setMapViewModelLocale() {
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    private fun setSearchViewModelLocale() {
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    override fun onResume() {
        super.onResume()
        locationManager?.removeUpdates(this)
        tryUpdateLocationManager()
    }

    private fun startProgress() {
        viewBinding.startLayout.progressStart.visibility = View.VISIBLE
        isShowingProgress = true
    }

    private fun stopProgress() {
        viewBinding.startLayout.progressStart.visibility = View.GONE
        isShowingProgress = false
    }

    @SuppressLint("MissingPermission")
    private fun tryUpdateLocationManager() {
        if (checkSelfPermissionLocation() && getLocationMode() == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
            alertDialogTitle = null
            locationManager?.removeUpdates(this)
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.5F,
                this
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0.5F,
                this
            )
            if (isCurrentLocationEvent)
                startProgress()
        }
    }

    override fun onLocationChanged(location: Location) {
        if (isCurrentLocationEvent) {
            mapViewModel.getPointByLatLon(
                location.latitude,
                location.longitude,
                PointMode.CURRENT_LOCATION_IN_CONSTRUCTOR
            )
        }
        isCurrentLocationEvent = false
    }

    override fun onProviderEnabled(provider: String) {
        //this method must be override
    }

    override fun onProviderDisabled(provider: String) {
        if (routeConstructorViewModel.startPoint.value?.second == true)
            routeConstructorViewModel.removeStartPoint()
        stopProgress()
        isCurrentLocationEvent = false
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        //this method must be override because minSDK is 22
    }

    private fun initLocationManager() {
        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun checkSelfPermissionLocation() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun getLocationMode(): Int =
        Settings.Secure.getInt(requireActivity().contentResolver, Settings.Secure.LOCATION_MODE)

    private fun checkLocationPermissionAndRequestUpdates() {
        if (checkPermission()) {
            when (getLocationMode()) {
                Settings.Secure.LOCATION_MODE_OFF -> createAlertDialogGeoPositionDisabled()
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY -> tryUpdateLocationManager()
                else -> createAlertDialogChangeLocationMode()
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
            clearDialog()
        }
    }

    private fun createAlertDialogGeoPositionDisabled() {
        createAlertDialog(
            TitleAlertDialog.GEO_POSITION_DISABLED,
            getString(R.string.location_disabled)
        ) {
            //open location settings
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            clearDialog()
        }
    }

    private fun createAlertDialogChangeLocationMode() {
        createAlertDialog(
            TitleAlertDialog.CHANGE_LOCATION_MODE,
            getString(R.string.change_location_mode)
        ) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            clearDialog()
        }
    }

    private fun createAlertDialogByTitle(savedTitle: TitleAlertDialog?) {
        if (savedTitle != null)
            when (alertDialogTitle) {
                TitleAlertDialog.GEO_POSITION_DISABLED -> createAlertDialogGeoPositionDisabled()
                TitleAlertDialog.LOCATION_PERMISSION_DENIED -> createAlertDialogLocationPermissionDenied()
                TitleAlertDialog.CHANGE_LOCATION_MODE -> createAlertDialogChangeLocationMode()
                else -> throw RuntimeException("Unknown title $alertDialogTitle")
            }
    }

    private fun createAlertDialog(
        title: TitleAlertDialog,
        message: String,
        positiveFunction: (() -> Unit)? = null
    ) {
        if (alertDialog?.isShowing == true)
            alertDialog?.dismiss()
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

    private fun initBackButton() {
        viewBinding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initStartButton() {
        viewBinding.startButton.setOnClickListener {
            routeConstructorViewModel.createRoute(RouteMode.NAVIGATION_ON_MAP_MODE)
        }
    }

    private fun initShowOnMapButton() {
        viewBinding.showButton.setOnClickListener {
            routeConstructorViewModel.createRoute(RouteMode.SHOW_ON_MAP_MODE)
        }
    }

    private fun initShowSavedRoutesButton() {
        viewBinding.savedRoutesButton.setOnClickListener {
            if ((it as Button).text == getString(R.string.show_saved_routes)) {
                viewBinding.savedRoutesRecyclerView.visibility = View.VISIBLE
                it.text = getString(R.string.hide_saved_routes)
            } else {
                it.text = getString(R.string.show_saved_routes)
                viewBinding.savedRoutesRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun initStartPointLayout() {
        viewBinding.startLayout.editStart.setOnClickListener {
            dialogMode = PointMode.START_POINT
            dialogMode?.let { createDialogByMode(it) }
        }
    }

    private fun initAdditionalPointsLayout() {
        viewBinding.additionalLayout.editAdd.setOnClickListener {
            dialogMode = PointMode.ADDITIONAL_POINT_IN_CONSTRUCTOR
            dialogMode?.let { createDialogByMode(it) }
        }
    }

    private fun initFinishPointLayout() {
        viewBinding.finishLayout.editFinish.setOnClickListener {
            dialogMode = PointMode.FINISH_POINT
            dialogMode?.let { createDialogByMode(it) }
        }
    }

    private fun observeRouteConstructorViewModel() {
        //observe start point
        routeConstructorViewModel.startPoint.observe(viewLifecycleOwner) {
            if (it != null)
                viewBinding.startLayout.startText.setText(it.first.text)
            else
                viewBinding.startLayout.startText.setText("")
        }
        //observe additional points list
        routeConstructorViewModel.additionalPoints.observe(viewLifecycleOwner) {
            if (it.size > 0)
                viewBinding.additionalLayout.addText.setText(
                    getString(
                        R.string.points_count,
                        it.size
                    )
                )
            else
                viewBinding.additionalLayout.addText.setText("")
        }
        //observe finish point
        routeConstructorViewModel.finishPoint.observe(viewLifecycleOwner) {
            if (it != null)
                viewBinding.finishLayout.finishText.setText(it.text)
            else
                viewBinding.finishLayout.finishText.setText("")
        }
        routeConstructorViewModel.route.observe(viewLifecycleOwner) {
            if (it.first != null) {
                when (it.second) { //route mode
                    RouteMode.SHOW_ON_MAP_MODE -> {
                        mapViewModel.setRoute(it.first!!)
                        mapViewModel.setListPoints(routeConstructorViewModel.getCurrentListOfPoints())
                        findNavController().popBackStack()
                    }
                    RouteMode.NAVIGATION_ON_MAP_MODE -> {
                        //navigator
                    }
                    else -> {
                        /*nothing*/
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                routeConstructorViewModel.pointError.collect {
                    createSnackbar(
                        when (it) {
                            PointError.START_POINT_CONTAINS -> getString(R.string.double_start_point)
                            PointError.ADDITIONAL_POINT_CONTAINS -> getString(R.string.double_additional_point)
                            PointError.FINISH_POINT_CONTAINS -> getString(R.string.double_finish_point)
                            PointError.NO_START_POINT -> getString(R.string.no_start_point)
                            PointError.NO_ADDITIONAL_AND_FINISH_POINTS -> getString(R.string.no_additional_finish_points)
                        }
                    )
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            routeConstructorViewModel.routeError.collect {
                createSnackbar(
                    when (it) {
                        Road.STATUS_INVALID -> getString(R.string.no_route)
                        Road.STATUS_TECHNICAL_ISSUE -> getString(R.string.technical_route_error)
                        else -> ""
                    }
                )
            }
        }
    }

    private fun observeMapViewModel() {
        mapViewModel.point.observe(viewLifecycleOwner) {
            if (it.first != null && it.second != null) {
                when (it.first) /*it.first is a mode of point*/ {
                    PointMode.START_POINT -> {
                        routeConstructorViewModel.setStartPoint(it.second!!, false)
                        mapViewModel.removePoint()
                    }
                    PointMode.CURRENT_LOCATION_IN_CONSTRUCTOR -> {
                        routeConstructorViewModel.setStartPoint(it.second!!, true)
                        stopProgress()
                        mapViewModel.removePoint()
                    }
                    PointMode.FINISH_POINT -> {
                        routeConstructorViewModel.setFinishPoint(it.second!!)
                        mapViewModel.removePoint()
                    }
                    else -> {
                        /*this observe is only for start and finish points*/
                    }
                }
            }
        }
        mapViewModel.route.observe(viewLifecycleOwner) {
            if (it == null)
                routeConstructorViewModel.route.value =
                    Pair(routeConstructorViewModel.route.value?.first, null)
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

    private fun createDialogByMode(mode: PointMode) {
        when (mode) {
            PointMode.START_POINT -> {
                showDialog(
                    {
                        clearDialog()
                        isCurrentLocationEvent = true
                        checkLocationPermissionAndRequestUpdates()
                    },
                    {
                        clearDialog()
                        mapViewModel.removePoint()
                        routeConstructorViewModel.setPointMode(mode)
                        findNavController().navigate(R.id.action_routeConstructorFragment_to_mapPointChoiceFragment)
                    },
                    {
                        clearDialog()
                        routeConstructorViewModel.setPointMode(mode)
                        findNavController().navigate(R.id.action_routeConstructorFragment_to_searchFragment)
                    },
                    {
                        routeConstructorViewModel.removeStartPoint()
                        clearDialog()
                    }
                )
            }
            PointMode.ADDITIONAL_POINT_IN_CONSTRUCTOR -> {
                clearDialog()
                findNavController().navigate(R.id.action_routeConstructorFragment_to_additionalPointsListFragment)
            }
            PointMode.FINISH_POINT -> {
                showDialog(
                    null /*without "My location" button*/,
                    {
                        clearDialog()
                        mapViewModel.removePoint()
                        routeConstructorViewModel.setPointMode(mode)
                        findNavController().navigate(R.id.action_routeConstructorFragment_to_mapPointChoiceFragment)
                    },
                    {
                        clearDialog()
                        routeConstructorViewModel.setPointMode(mode)
                        findNavController().navigate(R.id.action_routeConstructorFragment_to_searchFragment)
                    },
                    {
                        routeConstructorViewModel.removeFinishPoint()
                        clearDialog()
                    }
                )
            }
            else -> {
                /*ADDITIONAL_POINT_IN_LIST mode is needed only in AdditionalPointsListFragment*/
            }
        }
    }

    private fun clearDialog() {
        dialog?.dismiss()
        dialogMode = null
    }

    private fun showDialog(
        onCurrentLocationClickListener: (() -> Unit)? = null,
        onChooseOnMapClickListener: () -> Unit,
        onSearchClickListener: () -> Unit,
        onDeleteClickListener: (() -> Unit)? = null
    ) {
        dialog = RoutePointDialog(
            onCurrentLocationClickListener,
            onChooseOnMapClickListener,
            onSearchClickListener,
            onDeleteClickListener
        )
        dialog?.isCancelable = false
        dialog?.show(requireActivity().supportFragmentManager, DIALOG)
    }

    override fun onPause() {
        super.onPause()
        if (dialog?.dialog?.isShowing == true)
            dialog?.dismiss()
        else
            dialogMode = null
        alertDialog?.dismiss()
        locationManager?.removeUpdates(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    private fun getSavedValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            //get saved title of alert dialog
            alertDialogTitle = savedInstanceState.getParcelable(TITLE)
            createAlertDialogByTitle(alertDialogTitle)
            //get saved mode of dialog fragment
            dialogMode = savedInstanceState.getParcelable(ROUTE_POINT_DIALOG_MODE)
            dialogMode?.let { createDialogByMode(it) }
            //get saved event state
            isCurrentLocationEvent = savedInstanceState.getBoolean(IS_CURRENT_LOCATION_EVENT)
            //get progress state
            isShowingProgress = savedInstanceState.getBoolean(IS_SHOWING_PROGRESS)
            if (isShowingProgress)
                startProgress()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(
            ROUTE_POINT_DIALOG_MODE,
            dialogMode
        )
        outState.putParcelable(TITLE, alertDialogTitle)
        outState.putBoolean(IS_CURRENT_LOCATION_EVENT, isCurrentLocationEvent)
        outState.putBoolean(IS_SHOWING_PROGRESS, isShowingProgress)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val DIALOG = "DIALOG"
        private const val ROUTE_POINT_DIALOG_MODE = "ROUTE_POINT_DIALOG_MODE"
        private const val TITLE = "TITLE"
        private const val IS_CURRENT_LOCATION_EVENT = "IS_CURRENT_LOCATION_EVENT"
        private const val IS_SHOWING_PROGRESS = "IS_SHOWING_PROGRESS"
    }
}