package ru.krirll.optimalmaps.data.database.routeDatabase.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.krirll.optimalmaps.domain.model.PointItem

class PointItemConverter {

    @TypeConverter
    fun restorePointItemList(list: String): List<PointItem>? {
        return Gson().fromJson(list, object : TypeToken<List<PointItem>>() {}.type)
    }

    @TypeConverter
    fun savePointItemList(list: List<PointItem>?): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun restorePointItem(item: String): PointItem? {
        return Gson().fromJson(item, object : TypeToken<PointItem>() {}.type)
    }

    @TypeConverter
    fun savePointItem(item: PointItem?): String {
        return Gson().toJson(item)
    }
}