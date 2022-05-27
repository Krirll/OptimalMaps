package ru.krirll.optimalmaps.data.mapper

import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import ru.krirll.optimalmaps.data.database.searchDatabase.PointItemDbModel
import ru.krirll.optimalmaps.data.database.routeDatabase.RouteItemDbModel
import ru.krirll.optimalmaps.data.network.model.PointItemDto
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.model.RouteItem
import ru.krirll.optimalmaps.presentation.enums.PointZoom
import java.util.ArrayList

class PointMapper {

    fun mapPointDbModelToPointEntity(dbModel: PointItemDbModel) = PointItem(
        zoom = dbModel.zoom,
        text = dbModel.text,
        isHistorySearch = true,
        lat = dbModel.lat,
        lon = dbModel.lon
    )

    fun mapPointDtoToPointEntity(dto: PointItemDto) = PointItem(
        zoom = PointZoom.getZoomByImportance(dto.importance),
        text = dto.text,
        isHistorySearch = false,
        lat = dto.lat.toDouble(),
        lon = dto.lon.toDouble()
    )

    fun mapPointEntityToPointDbModel(entity: PointItem) = PointItemDbModel(
        zoom = entity.zoom,
        text = entity.text,
        isHistorySearch = true,
        lat = entity.lat,
        lon = entity.lon
    )

    fun mapRouteEntityToRouteDbModel(entity: RouteItem) = RouteItemDbModel(
        duration = entity.route.mDuration,
        length = entity.route.mLength,
        polyline = entity.route.mRouteHigh,
        nodes = entity.route.mNodes,
        points = entity.points,
        list = entity.list
    )

    fun mapRouteDbModelToRouteEntity(dbModel: RouteItemDbModel) = RouteItem(
        Road().apply {
            mDuration = dbModel.duration
            mLength = dbModel.length
            mNodes = dbModel.nodes as ArrayList<RoadNode>
            mRouteHigh = dbModel.polyline as ArrayList<GeoPoint>
        },
        points = dbModel.points,
        list = dbModel.list
    )
}