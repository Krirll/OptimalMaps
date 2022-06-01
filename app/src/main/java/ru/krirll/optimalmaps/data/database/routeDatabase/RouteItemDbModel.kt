package ru.krirll.optimalmaps.data.database.routeDatabase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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

    @ColumnInfo(name = "start_point")
    val startPoint: PointItem,

    @ColumnInfo(name = "additional_points")
    var additionalPoints: List<PointItem>? = null,

    @ColumnInfo(name = "finish_point")
    var finishPoint: PointItem? = null,

    @ColumnInfo(name = "points")
    val points: String
) {
    @PrimaryKey(autoGenerate = false)
    var id: Int? = null
}