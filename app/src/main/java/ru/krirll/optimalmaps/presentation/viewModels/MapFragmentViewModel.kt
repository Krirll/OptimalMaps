package ru.krirll.optimalmaps.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.krirll.optimalmaps.domain.model.PointItem

class MapFragmentViewModel : ViewModel() {

    private var _point = MutableLiveData<PointItem>()
    val point : LiveData<PointItem>
        get() = _point

    fun setMapPoint(point : PointItem) {
        _point.value = point
    }
}