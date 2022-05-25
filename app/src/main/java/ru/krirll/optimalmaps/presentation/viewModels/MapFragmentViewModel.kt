package ru.krirll.optimalmaps.presentation.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.entities.GetPointsByQueryUseCase
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.NetworkError
import ru.krirll.optimalmaps.presentation.enums.PointMode

class MapFragmentViewModel(app: Application) : AndroidViewModel(app) {

    //init repository and use case
    private val repository: PointRepositoryImpl = PointRepositoryImpl(app)
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

    private var _route = MutableLiveData<Road?>()
    val route: MutableLiveData<Road?>
        get() = _route

    private var _listPoints = MutableLiveData<List<PointItem>?>()
    val listPoints: MutableLiveData<List<PointItem>?>
        get() = _listPoints

    private var locale: String = ""

    fun setRoute(route: Road) {
        _route.value = route
    }

    fun removeRoute() {
        _route.value = null
    }

    fun setListPoints(list: List<PointItem>) {
        _listPoints.value = list
    }

    fun removeListPoints() {
        _listPoints.value = null
    }

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