package ru.krirll.optimalmaps.data.database.routeDatabase.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.bonuspack.routing.RoadNode

class RoadNodeConverter {

    @TypeConverter
    fun restoreRoadNode(node: String): List<RoadNode> {
        return Gson().fromJson(node, object : TypeToken<List<RoadNode>>() {}.type)
    }

    @TypeConverter
    fun saveRoadNode(node: List<RoadNode>): String {
        return Gson().toJson(node)
    }
}