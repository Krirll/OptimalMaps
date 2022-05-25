package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl
import ru.krirll.optimalmaps.domain.model.PointItem

class GetOptimalRouteUseCase(
    private val repository: PointRepositoryImpl
) {

    suspend operator fun invoke(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (Int) -> Unit
    ) = repository.createRoute(points, withEndPoint, onErrorEventListener)
}