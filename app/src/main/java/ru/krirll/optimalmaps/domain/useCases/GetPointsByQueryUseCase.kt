package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.domain.repository.RemoteRepository

class GetPointsByQueryUseCase(
    private val repository: RemoteRepository
) {
    
    suspend operator fun invoke(
        query: String,
        locale: String,
        emptyResultEventListener: () -> Unit,
        noInternetEventListener: () -> Unit
    ) = repository.getSearchResult(query, locale, emptyResultEventListener, noInternetEventListener)
}