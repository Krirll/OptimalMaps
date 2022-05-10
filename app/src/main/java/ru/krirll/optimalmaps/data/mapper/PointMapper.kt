package ru.krirll.optimalmaps.data.mapper

import ru.krirll.optimalmaps.data.database.model.PointItemDbModel
import ru.krirll.optimalmaps.data.network.model.PointItemDto
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.PointZoom

class PointMapper {

    fun mapDbModelToEntity(dbModel: PointItemDbModel) = PointItem(
        zoom = dbModel.zoom,
        text = dbModel.text,
        isHistorySearch = true,
        lat = dbModel.lat,
        lon = dbModel.lon
    )

    fun mapDtoToEntity(dto: PointItemDto) = PointItem(
        zoom = PointZoom.getZoomByImportance(dto.importance),
        text = dto.text,
        isHistorySearch = false,
        lat = dto.lat.toDouble(),
        lon = dto.lon.toDouble()
    )

    fun mapEntityToDbModel(entity: PointItem) = PointItemDbModel(
        zoom = entity.zoom,
        text = entity.text,
        isHistorySearch = true,
        lat = entity.lat,
        lon = entity.lon
    )
}