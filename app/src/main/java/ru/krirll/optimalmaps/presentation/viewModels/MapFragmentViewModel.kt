package ru.krirll.optimalmaps.presentation.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.data.repository.RemoteRepositoryImpl
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.useCases.GetPointsByQueryUseCase
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointMode
import ru.krirll.optimalmaps.presentation.enums.RouteMode

class MapFragmentViewModel : ViewModel() {

    //init repository and use case
    private val repository: RemoteRepositoryImpl = RemoteRepositoryImpl()
    private val getSearchByQueryUseCase: GetPointsByQueryUseCase = GetPointsByQueryUseCase(repository)

    //channel for sending errors
    private var _networkError = Channel<NetworkError>()
    val networkError
        get() = _networkError.receiveAsFlow()

    private var _currentLocationPointTitle = MutableLiveData<String?>()
    val currentLocationPointTitle: MutableLiveData<String?>
        get() = _currentLocationPointTitle

    private var _point = MutableLiveData<Pair<PointMode?, PointItem?>>()
    val point: MutableLiveData<Pair<PointMode?, PointItem?>>
        get() = _point

    private var _route = MutableLiveData<Pair<Road?, RouteMode?>>()
    val route: MutableLiveData<Pair<Road?, RouteMode?>>
        get() = _route

    private var currentIndexNode = 1

    private var locale: String = ""

    fun setRoute(route: Road, mode: RouteMode) {
        _route.value = Pair(route.apply { mRouteHigh = ArrayList(route.mRouteHigh) }, mode)
    }

    fun removeRoute() {
        _route.value = Pair(null, null)
        currentIndexNode = 1
    }

    fun removeLastNode() {
        var containsPoint = _route.value?.first?.mRouteHigh?.indexOf(
            route.value?.first?.mNodes?.get(currentIndexNode)?.mLocation
        )
        containsPoint?.let {
            while(containsPoint-- != 0 && containsPoint >= 0) {
                _route.value?.first?.mRouteHigh?.removeFirst()
            }
        }
        currentIndexNode++
    }

    fun getCurrentIndexNode() = currentIndexNode

    fun setPoint(point: PointItem, mode: PointMode) {
        _point.value = Pair(mode, point)
    }

    fun removePoint() {
        _point.postValue(Pair(null, null))
    }

    fun setLocale(str: String) {
        locale = str
    }

    private fun onError(error: NetworkError) {
        viewModelScope.launch {
            _networkError.send(error)
        }
    }

    fun getPointByLatLon(lat: Double, lon: Double, mode: PointMode?) {
        viewModelScope.launch {
            val result = getSearchByQueryUseCase.invoke(
                "$lat $lon",
                locale,
                { onError(NetworkError.NO_INFO_ABOUT_POINT) },
                { onError(NetworkError.NO_INTERNET) }
            )
            if (result.isNotEmpty())
                when (mode) {
                    PointMode.CURRENT_LOCATION_POINT ->
                        currentLocationPointTitle.value = result.first().text
                    else -> {
                        point.value =
                            Pair(
                                mode,
                                result.map { PointItem(it.zoom, it.text, false, lat, lon) }[0]
                            )
                    }
                }
        }
    }
}