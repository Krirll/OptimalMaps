package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.RouteError

class GetOptimalRouteUseCase(
    private val repository: PointRepositoryImpl
) {

    suspend operator fun invoke(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (RouteError) -> Unit
    ) = repository.createRoute(points, withEndPoint, onErrorEventListener)
}