package ru.krirll.optimalmaps.domain.useCases

import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.repository.LocalRepository

class SaveRouteUseCase(
    private val repository: LocalRepository
) {

    suspend operator fun invoke(
        route: Road, points: String, startPoint: PointItem,
        additionalPoints: List<PointItem>?,
        finishPoint: PointItem?
    ) =
        repository.saveRoute(route, points, startPoint, additionalPoints, finishPoint)
}