package ru.krirll.optimalmaps.data.mapper

import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import ru.krirll.optimalmaps.data.database.routeDatabase.RouteItemDbModel
import ru.krirll.optimalmaps.domain.model.RouteItem
import java.util.ArrayList

class RouteMapper {

    fun mapRouteEntityToRouteDbModel(entity: RouteItem) = RouteItemDbModel(
        duration = entity.route.mDuration,
        length = entity.route.mLength,
        polyline = entity.route.mRouteHigh,
        nodes = entity.route.mNodes,
        points = entity.points,
        startPoint = entity.startPoint,
        additionalPoints = entity.additionalPoints,
        finishPoint = entity.finishPoint
    )

    fun mapRouteDbModelToRouteEntity(dbModel: RouteItemDbModel) = RouteItem(
        Road().apply {
            mDuration = dbModel.duration
            mLength = dbModel.length
            mNodes = dbModel.nodes as ArrayList<RoadNode>
            mRouteHigh = dbModel.polyline as ArrayList<GeoPoint>
        },
        points = dbModel.points,
        startPoint = dbModel.startPoint,
        additionalPoints = dbModel.additionalPoints,
        finishPoint = dbModel.finishPoint
    )
}