package ru.krirll.optimalmaps.domain.repository

import androidx.lifecycle.LiveData
import ru.krirll.optimalmaps.domain.model.PointItem

interface PointRepository {

    suspend fun getSearchResult(
        query: String,
        emptyResultEventListener: () -> Unit,
        noInternetEventListener: () -> Unit,
    ): List<PointItem>

    fun loadSearchHistory(): LiveData<List<PointItem>>

    suspend fun savePointItem(item: PointItem)

}