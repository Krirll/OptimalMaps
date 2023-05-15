package ru.krirll.optimalmaps.domain.entities

import ru.krirll.optimalmaps.domain.repository.LocalRepository

class LoadSearchHistoryUseCase(
    private val repository: LocalRepository
) {

    operator fun invoke() = repository.loadSearchHistory()
}