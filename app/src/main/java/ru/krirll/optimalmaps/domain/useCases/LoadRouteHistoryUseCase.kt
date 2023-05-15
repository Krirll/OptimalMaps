package ru.krirll.optimalmaps.domain.useCases

import ru.krirll.optimalmaps.domain.repository.LocalRepository

class LoadRouteHistoryUseCase(
    private val repository: LocalRepository
) {

    operator fun invoke() = repository.loadRouteHistory()
}