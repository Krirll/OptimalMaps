package ru.krirll.optimalmaps.domain.repository

import ru.krirll.optimalmaps.domain.model.PointItem

interface RemoteRepository {

    suspend fun getSearchResult(
        query: String,
        locale: String,
        emptyResultEventListener: () -> Unit,
        noInternetEventListener: () -> Unit,
    ): List<PointItem>

}