package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.domain.repository.PointRepository

class GetPointByQueryUseCase(
    private val repository: PointRepository
) {

    suspend operator fun invoke(
        query: String,
        emptyResultEventListener: () -> Unit,
        noInternetEventListener: () -> Unit
    ) = repository.getSearchResult(query, emptyResultEventListener, noInternetEventListener)
}