package ru.krirll.optimalmaps.data.database.routeDatabase.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint

class GeoPointConverter {

    @TypeConverter
    fun restoreGeoPoint(geoPoint: String): GeoPoint {
        return Gson().fromJson(geoPoint, object : TypeToken<GeoPoint>() {}.type)
    }

    @TypeConverter
    fun saveGeoPoint(geoPoint: GeoPoint): String {
        return Gson().toJson(geoPoint)
    }
}