package ru.krirll.optimalmaps.data.database.routeDatabase

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import ru.krirll.optimalmaps.domain.model.PointItem

@Entity
data class RouteItemDbModel(

    @ColumnInfo(name = "length")
    val length: Double,

    @ColumnInfo(name = "duration")
    val duration: Double,

    @ColumnInfo(name = "polyline")
    val polyline: List<GeoPoint>,

    @ColumnInfo(name = "nodes")
    val nodes: List<RoadNode>,

    @ColumnInfo(name = "points_list")
    val list: List<PointItem>,

    @ColumnInfo(name = "points")
    val points: String
) {
    @PrimaryKey(autoGenerate = false)
    var id: Int? = null
}