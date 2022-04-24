package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.domain.repository.PointRepository

class LoadSearchHistoryUseCase(
    private val repository: PointRepository
) {

    operator fun invoke() = repository.loadSearchHistory()
}