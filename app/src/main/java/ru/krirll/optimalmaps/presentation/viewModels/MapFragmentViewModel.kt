package ru.krirll.optimalmaps.presentation.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

class MapFragmentViewModel : ViewModel() {

    private val _point = MutableLiveData<GeoPoint>()
    val point : LiveData<GeoPoint>
        get() = _point

    private val _listPoint = MutableLiveData<MutableList<GeoPoint>>()
    val listPoint : LiveData<MutableList<GeoPoint>>
        get() = _listPoint

    fun setSingleMapPoint(point : GeoPoint) {
        //  set point on map
    }

    fun createDirection() {
        //  get list of points and create direction on map
    }

}