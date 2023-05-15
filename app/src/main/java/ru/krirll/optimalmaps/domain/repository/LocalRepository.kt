package ru.krirll.optimalmaps.domain.repository

import androidx.lifecycle.LiveData
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.model.RouteItem
import ru.krirll.optimalmaps.presentation.enums.RouteError

interface LocalRepository {

    fun loadSearchHistory(): LiveData<List<PointItem>>

    suspend fun savePointItem(item: PointItem)

    suspend fun createRoute(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (RouteError) -> Unit
    ): Road?

    fun loadRouteHistory(): LiveData<List<RouteItem>>

    suspend fun saveRoute(
        route: Road,
        points: String,
        startPoint: PointItem,
        additionalPoints: List<PointItem>?,
        finishPoint: PointItem?
    )

}