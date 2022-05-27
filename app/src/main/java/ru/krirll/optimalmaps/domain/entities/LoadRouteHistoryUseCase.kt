package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.data.repository.PointRepositoryImpl

class LoadRouteHistoryUseCase(
    private val repository: PointRepositoryImpl
) {

    operator fun invoke() = repository.loadRouteHistory()
}