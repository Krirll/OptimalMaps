package ru.krirll.optimalmaps.domain.repository

import androidx.lifecycle.LiveData
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.domain.model.PointItem

interface PointRepository {

    suspend fun getSearchResult(
        query: String,
        locale: String,
        emptyResultEventListener: () -> Unit,
        noInternetEventListener: () -> Unit,
    ): List<PointItem>

    fun loadSearchHistory(): LiveData<List<PointItem>>

    suspend fun savePointItem(item: PointItem)

    suspend fun createRoute(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (Int) -> Unit
    ): Road?

}