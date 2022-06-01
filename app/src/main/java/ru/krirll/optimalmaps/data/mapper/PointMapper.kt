package ru.krirll.optimalmaps.data.mapper

import ru.krirll.optimalmaps.data.database.searchDatabase.PointItemDbModel
import ru.krirll.optimalmaps.data.network.model.PointItemDto
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.PointZoom

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
}