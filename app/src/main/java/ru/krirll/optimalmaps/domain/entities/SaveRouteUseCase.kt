package ru.krirll.optimalmaps.domain.entities

import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.model.PointItem

class SaveRouteUseCase(
    private val repository: PointRepositoryImpl
) {

    suspend operator fun invoke(
        route: Road, points: String, startPoint: PointItem,
        additionalPoints: List<PointItem>?,
        finishPoint: PointItem?
    ) =
        repository.saveRoute(route, points, startPoint, additionalPoints, finishPoint)
}