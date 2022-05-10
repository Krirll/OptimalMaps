package ru.krirll.optimalmaps.presentation.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.entities.GetPointsByQueryUseCase
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.NetworkError

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

    private var _point = MutableLiveData<PointItem?>()
    val point: MutableLiveData<PointItem?>
        get() = _point

    private var locale: String = ""

    fun setPoint(point: PointItem) {
        _point.value = point
    }

    fun removePoint() {
        _point.postValue(null)
    }

    fun setLocale(str: String) {
        locale = str
    }

    private fun onError(error: NetworkError) {
        viewModelScope.launch {
            _networkError.send(error)
        }
    }

    fun getPointByLatLon(lat: Double, lon: Double, isCurrentLocation: Boolean) {
        viewModelScope.launch {
            val result = getSearchByQueryUseCase.invoke(
                "$lat $lon",
                locale,
                { onError(NetworkError.NO_INFO_ABOUT_POINT) },
                { onError(NetworkError.NO_INTERNET) }
            )
            if (result.isNotEmpty())
                if (!isCurrentLocation)
                    point.value = result.first()
                else
                    currentLocationPointTitle.value = result.first().text
        }
    }
}