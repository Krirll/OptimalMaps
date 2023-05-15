package ru.krirll.optimalmaps.domain.useCases

import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.repository.LocalRepository
import ru.krirll.optimalmaps.presentation.enums.RouteError

class GetOptimalRouteUseCase(
    private val repository: LocalRepository
) {

    suspend operator fun invoke(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (RouteError) -> Unit
    ) = repository.createRoute(points, withEndPoint, onErrorEventListener)
}