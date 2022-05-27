package ru.krirll.optimalmaps.data.database.routeDatabase.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint

class PolylineConverter {

    @TypeConverter
    fun restorePolyline(list: String): List<GeoPoint> {
        return Gson().fromJson(list, object : TypeToken<List<GeoPoint>>() {}.type)
    }

    @TypeConverter
    fun savePolyline(list: List<GeoPoint>): String {
        return Gson().toJson(list)
    }
}