package ru.krirll.optimalmaps.presentation.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.krirll.optimalmaps.domain.model.PointItem

class MapFragmentViewModel : ViewModel() {

    private var _point = MutableLiveData<PointItem?>()
    val point : MutableLiveData<PointItem?>
        get() = _point

    fun setPoint(point : PointItem) {
        _point.value = point
    }

    fun removePoint() {
        _point.postValue(null)
    }
}