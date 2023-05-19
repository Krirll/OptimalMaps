package ru.krirll.optimalmaps.presentation.fragments

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.R
import ru.krirll.optimalmaps.databinding.FragmentRouteConstructorBinding
import ru.krirll.optimalmaps.presentation.adapters.routeAdapter.RouteListAdapter
import ru.krirll.optimalmaps.presentation.dialogFragment.RoutePointDialog
import ru.krirll.optimalmaps.presentation.enums.Locale
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointError
import ru.krirll.optimalmaps.presentation.enums.PointMode
import ru.krirll.optimalmaps.presentation.enums.RouteError
import ru.krirll.optimalmaps.presentation.enums.RouteMode
import ru.krirll.optimalmaps.presentation.enums.TitleAlertDialog
import ru.krirll.optimalmaps.presentation.viewModels.MapFragmentViewModel
import ru.krirll.optimalmaps.presentation.viewModels.RouteConstructorViewModel

class RouteConstructorFragment: BaseFragmentLocationSupport() {

    private var _viewBinding: FragmentRouteConstructorBinding? = null
    private val viewBinding: FragmentRouteConstructorBinding
        get() = _viewBinding ?: throw RuntimeException("FragmentRouteConstructorBinding == null")

    private val routeConstructorViewModel by lazy {
        ViewModelProvider(requireActivity())[RouteConstructorViewModel::class.java]
    }

    private val mapViewModel by lazy {
        ViewModelProvider(requireActivity())[MapFragmentViewModel::class.java]
    }

    private var listAdapter: RouteListAdapter? = null
    private var dialog: RoutePointDialog? = null

    private var dialogMode: PointMode? = null

    private val currentLocale by lazy {
        Locale.getLocale(LocaleListCompat.getDefault()[0]!!.toLanguageTag())
    }

    private var isCurrentLocationEvent: Boolean = false
    private var isShowingStartPointProgress: Boolean = false
    private var isShowingStartNavProgress: Boolean = false
    private var isShowingShowOnMapProgress: Boolean = false
    private var isShowingSavedRoutes: Boolean = false
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
        initBackButton()
        initStartButton()
        initShowOnMapButton()
        initShowSavedRoutesButton()
        initStartPointLayout()
        initAdditionalPointsLayout()
        initFinishPointLayout()
        initRecyclerView()
        setMapViewModelLocale()
        setSearchViewModelLocale()
        observeMapViewModel()
        observeRouteConstructorViewModel()
    }

    override fun onResume() {
        startService()
        tryUpdateLocation()
        super.onResume()
    }

    private fun setMapViewModelLocale() {
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    private fun setSearchViewModelLocale() {
        mapViewModel.setLocale(Locale.getLocale(currentLocale))
    }

    private fun startPointProgress() {
        viewBinding.startLayout.progressStart.visibility = View.VISIBLE
        isShowingStartPointProgress = true
    }

    private fun stopPointProgress() {
        viewBinding.startLayout.progressStart.visibility = View.GONE
        isShowingStartPointProgress = false
    }

    private fun startShowOnMapProgress() {
        viewBinding.apply {
            progressShowOnMap.visibility = View.VISIBLE
            startButton.isEnabled = false
            setEnabled(false)
        }
        isShowingShowOnMapProgress = true
    }

    private fun stopShowOnMapProgress() {
        viewBinding.apply {
            progressShowOnMap.visibility = View.GONE
            startButton.isEnabled = true
            setEnabled(true)
        }
        isShowingShowOnMapProgress = false
    }

    private fun startNavProgress() {
        viewBinding.apply {
            progressStartNav.visibility = View.VISIBLE
            showButton.isEnabled = false
            setEnabled(false)
        }
        isShowingStartNavProgress = true
    }

    private fun setEnabled(value: Boolean) {
        viewBinding.apply {
            startLayout.editStart.isEnabled = value
            additionalLayout.editAdd.isEnabled = value
            finishLayout.editFinish.isEnabled = value
            if (!value) {
                savedRoutesButton.text = getString(R.string.show_saved_routes)
                savedRoutesRecyclerView.visibility = View.GONE
            }
            savedRoutesButton.isEnabled = value
        }
    }

    private fun stopNavProgress() {
        viewBinding.apply {
            progressStartNav.visibility = View.GONE
            showButton.isEnabled = true
            setEnabled(true)
        }
        isShowingStartNavProgress = false
    }

    override fun updateLocation(location: Location) {
        if (isCurrentLocationEvent) {
            mapViewModel.getPointByLatLon(
                location.latitude,
                location.longitude,
                PointMode.CURRENT_LOCATION_IN_CONSTRUCTOR
            )
        }
        isCurrentLocationEvent = false
    }

    override fun setStateProviderDisabled() {
        if (routeConstructorViewModel.startPoint.value?.second == true)
            routeConstructorViewModel.removeStartPoint()
        stopPointProgress()
        isCurrentLocationEvent = false
    }

    private fun tryUpdateLocation() {
        alertDialogTitle = null
        locationService?.tryUpdateLocation()
        if (isCurrentLocationEvent)
            startPointProgress()
    }

    private fun initBackButton() {
        viewBinding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initStartButton() {
        viewBinding.startButton.setOnClickListener {
            startNavProgress()
            routeConstructorViewModel.createRoute(RouteMode.NAVIGATION_ON_MAP_MODE)
        }
    }

    private fun initShowOnMapButton() {
        viewBinding.showButton.setOnClickListener {
            startShowOnMapProgress()
            routeConstructorViewModel.createRoute(RouteMode.SHOW_ON_MAP_MODE)
        }
    }

    private fun initShowSavedRoutesButton() {
        viewBinding.savedRoutesButton.setOnClickListener {
            if ((it as Button).text == getString(R.string.show_saved_routes)) {
                viewBinding.savedRoutesRecyclerView.visibility = View.VISIBLE
                it.text = getString(R.string.hide_saved_routes)
                isShowingSavedRoutes = true
                if (listAdapter?.currentList == null)
                    routeConstructorViewModel.loadRouteHistory()
            } else {
                isShowingSavedRoutes = false
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

    private fun initRecyclerView() {
        listAdapter = RouteListAdapter().apply {
            setOnRouteItemClickListener {
                routeConstructorViewModel.apply {
                    startPoint.value = Pair(it.startPoint, false)
                    additionalPoints.value = mutableListOf()
                    additionalPoints.value = it.additionalPoints?.toMutableList()
                    finishPoint.value = it.finishPoint
                    updateCurrentList()
                    route.value = Pair(it.route, null)
                }
                viewBinding.scrollView.smoothScrollTo(0, 0, 1200)
            }
        }
        routeConstructorViewModel.loadRouteHistory()
        viewBinding.savedRoutesRecyclerView.adapter = listAdapter!!
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
            if (it != null && it.size > 0)
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
                stopNavProgress()
                stopShowOnMapProgress()
                routeConstructorViewModel.updateNodes()
                val routeCopy = Road().apply {
                    mLength = it.first?.mLength!!
                    mDuration = it.first?.mDuration!!
                    mNodes = it.first?.mNodes!!
                    mRouteHigh = it.first?.mRouteHigh
                }
                when (it.second) { //route mode
                    RouteMode.SHOW_ON_MAP_MODE -> {
                        if (it.first?.mLength!! < 1000)
                            saveRoute()
                        mapViewModel.setRoute(routeCopy, RouteMode.SHOW_ON_MAP_MODE)
                        findNavController().popBackStack()
                    }
                    RouteMode.NAVIGATION_ON_MAP_MODE -> {
                        if (it.first?.mLength!! < 1000)
                            saveRoute()
                        mapViewModel.setRoute(routeCopy, RouteMode.NAVIGATION_ON_MAP_MODE)
                        findNavController().popBackStack()
                    }
                    else -> {
                        /*nothing*/
                    }

                }
            }
        }
        routeConstructorViewModel.routeHistory.observe(viewLifecycleOwner) {
            if (it != null) {
                listAdapter?.submitList(it.reversed())
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
                    stopNavProgress()
                    stopShowOnMapProgress()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                routeConstructorViewModel.routeError.collect {
                    createSnackbar(
                        when (it) {
                            RouteError.ROUTE_INVALID -> getString(R.string.no_route)
                            RouteError.ROUTE_TECHNICAL_ISSUE -> getString(R.string.technical_route_error)
                            RouteError.ROUTE_TOO_BIG -> getString(R.string.too_big_route)
                            RouteError.MAX_COUNT_OF_POINTS -> getString(R.string.max_count_points)
                        }
                    )
                    routeConstructorViewModel.clearCurrentListOfPoints()
                    stopNavProgress()
                    stopShowOnMapProgress()
                }
            }
        }
    }

    private fun saveRoute() {
        routeConstructorViewModel.apply {
            val firstString = startPoint.value?.first?.let {
                getString(R.string.start_point_route, it.text)
            } ?: ""
            val secondString = additionalPoints.value?.let {
                if (it.size != 0)
                    getString(R.string.point_count_route, it.size)
                else
                    ""
            } ?: ""
            val thirdString = finishPoint.value?.let {
                getString(R.string.finish_point_route, it.text)
            } ?: ""
            saveRoute(firstString + secondString + thirdString)
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
                        stopPointProgress()
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
            if (it.second == null)
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
                    stopPointProgress()
                    createSnackbar(message)
                }
            }
        }
    }


    private fun createDialogByMode(mode: PointMode) {
        when (mode) {
            PointMode.START_POINT -> {
                showDialog(
                    {
                        clearDialog()
                        isCurrentLocationEvent = true
                        tryUpdateLocation()
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
            else -> {}
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    private fun getSavedValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            //get saved title of alert dialog
            alertDialogTitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                savedInstanceState.getParcelable(TITLE, TitleAlertDialog::class.java)
            else
                savedInstanceState.getParcelable(TITLE)

            alertDialogTitle?.let { createAlertDialogByTitle(it) }
            //get saved mode of dialog fragment
            dialogMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                savedInstanceState.getParcelable(ROUTE_POINT_DIALOG_MODE, PointMode::class.java)
            else
                savedInstanceState.getParcelable(ROUTE_POINT_DIALOG_MODE)
            dialogMode?.let { createDialogByMode(it) }
            //get saved event state
            isCurrentLocationEvent = savedInstanceState.getBoolean(IS_CURRENT_LOCATION_EVENT)
            //get progress state
            isShowingStartPointProgress = savedInstanceState.getBoolean(IS_SHOWING_START_POINT_PROGRESS)
            if (isShowingStartPointProgress)
                startPointProgress()
            isShowingShowOnMapProgress = savedInstanceState.getBoolean(IS_SHOWING_SHOW_ON_MAP_PROGRESS)
            if (isShowingShowOnMapProgress)
                startShowOnMapProgress()
            isShowingStartNavProgress = savedInstanceState.getBoolean(IS_SHOWING_START_NAV_PROGRESS)
            if (isShowingStartNavProgress)
                startNavProgress()
            isShowingSavedRoutes = savedInstanceState.getBoolean(IS_SHOWING_SAVED_ROUTES)
            if (isShowingSavedRoutes) {
                viewBinding.savedRoutesRecyclerView.visibility = View.VISIBLE
                viewBinding.savedRoutesButton.text = getString(R.string.hide_saved_routes)
                routeConstructorViewModel.loadRouteHistory()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(
            ROUTE_POINT_DIALOG_MODE,
            dialogMode
        )
        outState.putParcelable(TITLE, alertDialogTitle)
        outState.putBoolean(IS_CURRENT_LOCATION_EVENT, isCurrentLocationEvent)
        outState.putBoolean(IS_SHOWING_START_POINT_PROGRESS, isShowingStartPointProgress)
        outState.putBoolean(IS_SHOWING_SHOW_ON_MAP_PROGRESS, isShowingShowOnMapProgress)
        outState.putBoolean(IS_SHOWING_START_NAV_PROGRESS, isShowingStartNavProgress)
        outState.putBoolean(IS_SHOWING_SAVED_ROUTES, isShowingSavedRoutes)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val DIALOG = "DIALOG"
        private const val ROUTE_POINT_DIALOG_MODE = "ROUTE_POINT_DIALOG_MODE"
        private const val TITLE = "TITLE"
        private const val IS_CURRENT_LOCATION_EVENT = "IS_CURRENT_LOCATION_EVENT"
        private const val IS_SHOWING_START_POINT_PROGRESS = "IS_SHOWING_START_POINT_PROGRESS"
        private const val IS_SHOWING_SHOW_ON_MAP_PROGRESS = "IS_SHOWING_SHOW_ON_MAP_PROGRESS"
        private const val IS_SHOWING_START_NAV_PROGRESS = "IS_SHOWING_START_NAV_PROGRESS"
        private const val IS_SHOWING_SAVED_ROUTES = "IS_SHOWING_SAVED_ROUTES"
    }
}