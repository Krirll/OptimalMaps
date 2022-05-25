package ru.krirll.optimalmaps.presentation.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.entities.GetOptimalRouteUseCase
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.PointError
import ru.krirll.optimalmaps.presentation.enums.PointMode
import ru.krirll.optimalmaps.presentation.enums.RouteMode

class RouteConstructorViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = PointRepositoryImpl(app)
    private val getOptimalRouteUseCase = GetOptimalRouteUseCase(repository)

    private var _startPoint = MutableLiveData<Pair<PointItem, Boolean>?>()
    val startPoint: MutableLiveData<Pair<PointItem, Boolean>?>
        get() = _startPoint

    private var _additionalPoints = MutableLiveData<MutableList<PointItem>>()
    val additionalPoints: MutableLiveData<MutableList<PointItem>>
        get() = _additionalPoints

    private var _finishPoint = MutableLiveData<PointItem?>()
    val finishPoint: MutableLiveData<PointItem?>
        get() = _finishPoint

    private var _route = MutableLiveData<Pair<Road?, RouteMode?>>()
    val route: MutableLiveData<Pair<Road?, RouteMode?>>
        get() = _route

    private var currentListOfPoints: MutableList<PointItem> = mutableListOf()

    private var _pointError = Channel<PointError>(CONFLATED)
    val pointError
        get() = _pointError.receiveAsFlow()

    private var _routeError = Channel<Int>(CONFLATED)
    val routeError
        get() = _routeError.receiveAsFlow()

    private var pointMode: PointMode? = null

    private var currentItem: PointItem? = null

    fun setPointMode(mode: PointMode) {
        pointMode = mode
    }

    fun getPointMode() = pointMode

    fun removePointMode() {
        pointMode = null
    }

    fun setCurrentItemIndex(point: PointItem) {
        currentItem = point
    }

    fun getCurrentItem() = currentItem

    fun removeCurrentItemIndex() {
        currentItem = null
    }

    fun setStartPoint(point: PointItem, isCurrentLocation: Boolean) {
        if (!contains(point))
            _startPoint.value = Pair(point, isCurrentLocation)
    }

    fun removeStartPoint() {
        _startPoint.value = null
    }

    fun addAdditionalPoint(point: PointItem) {
        if (!contains(point)) {
            if (additionalPoints.value == null)
                _additionalPoints.value = mutableListOf()
            _additionalPoints.value?.add(point)
        }
    }

    fun removeAdditionalPoint(point: PointItem) {
        _additionalPoints.value = _additionalPoints.value?.apply { remove(point) }
    }

    fun editAdditionalPoint(point: PointItem) {
        if (!contains(point))
            currentItem?.let { p ->
                _additionalPoints.value =
                    _additionalPoints.value?.apply { set(indexOf(p), point) }
            }
    }

    fun setFinishPoint(point: PointItem) {
        if (!contains(point))
            _finishPoint.value = point
    }

    fun removeFinishPoint() {
        _finishPoint.value = null
    }

    fun createRoute(mode: RouteMode) {
        if (startPoint.value != null) {
            if (additionalPoints.value != null && additionalPoints.value?.size != 0) {
                val list = createListOfPoints()
                if (currentListOfPoints != list) {
                    currentListOfPoints = list
                    CoroutineScope(Dispatchers.IO).launch {
                        clearRoute(mode)
                        _route.postValue(
                            Pair(
                                getOptimalRouteUseCase.invoke(
                                    currentListOfPoints,
                                    finishPoint.value != null
                                ) { viewModelScope.launch { _routeError.send(it) } },
                                mode
                            )
                        )
                    }
                }
                else {
                    _route.postValue(Pair(_route.value?.first, mode))
                }
            } else {
                if (finishPoint.value != null) {
                    val list = createListOfPoints()
                    if (currentListOfPoints != list) {
                        currentListOfPoints = list
                        CoroutineScope(Dispatchers.IO).launch {
                            clearRoute(mode)
                            _route.postValue(
                                Pair(
                                    getOptimalRouteUseCase.invoke(
                                        currentListOfPoints,
                                        true
                                    ) { viewModelScope.launch { _routeError.send(it) } },
                                    mode
                                )
                            )
                        }
                    }
                    else {
                        _route.postValue(Pair(_route.value?.first, mode))
                    }
                } else {
                    sendError(PointError.NO_ADDITIONAL_AND_FINISH_POINTS)
                }
            }
        } else {
            sendError(PointError.NO_START_POINT)
        }
    }

    private fun clearRoute(mode: RouteMode) {
        _route.postValue(Pair(null, mode))
    }

    private fun createListOfPoints() =
        mutableListOf<PointItem>().apply {
            startPoint.value?.first?.let { add(it) }
            additionalPoints.value?.let { addAll(it) }
            finishPoint.value?.let { add(it) }
        }

    private fun contains(point: PointItem): Boolean {
        var result = true
        if (point.lat != startPoint.value?.first?.lat && point.lat != startPoint.value?.first?.lat) {
            val a =
                additionalPoints.value?.firstOrNull { it.lat == point.lat && it.lon == point.lon }
            if (a == null) {
                if (point.lat != finishPoint.value?.lat && point.lat != finishPoint.value?.lat) {
                    result = false
                } else {
                    sendError(PointError.FINISH_POINT_CONTAINS)
                }
            } else {
                sendError(PointError.ADDITIONAL_POINT_CONTAINS)
            }
        } else {
            sendError(PointError.START_POINT_CONTAINS)
        }
        return result
    }

    private fun sendError(error: PointError) {
        viewModelScope.launch { _pointError.send(error) }
    }
}