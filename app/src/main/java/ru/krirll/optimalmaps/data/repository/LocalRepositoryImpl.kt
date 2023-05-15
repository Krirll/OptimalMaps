package ru.krirll.optimalmaps.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.data.database.routeDatabase.RouteHistoryDao
import ru.krirll.optimalmaps.data.database.routeDatabase.RouteHistoryDatabase
import ru.krirll.optimalmaps.data.database.searchDatabase.SearchHistoryDao
import ru.krirll.optimalmaps.data.database.searchDatabase.SearchHistoryDatabase
import ru.krirll.optimalmaps.data.mapper.PointMapper
import ru.krirll.optimalmaps.data.mapper.RouteMapper
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.model.RouteItem
import ru.krirll.optimalmaps.domain.repository.LocalRepository
import ru.krirll.optimalmaps.presentation.enums.RouteError
import ru.krirll.optimalmaps.utils.OptimalRouteSearchUtil

class LocalRepositoryImpl(
    private val application: Application,
    private val pointMapper: PointMapper = PointMapper(),
    private val routeMapper: RouteMapper = RouteMapper(),
    private val searchHistoryDao: SearchHistoryDao = SearchHistoryDatabase.getInstance(application).searchDao(),
    private val routeHistoryDao: RouteHistoryDao = RouteHistoryDatabase.getInstance(application).routeDao(),
    private val optimalRouteSearchUtil: OptimalRouteSearchUtil = OptimalRouteSearchUtil(application.applicationContext)
) : LocalRepository {

    override fun loadSearchHistory(): LiveData<List<PointItem>> =
        searchHistoryDao.getSearchHistory().map { list ->
            list.map {
                pointMapper.mapPointDbModelToPointEntity(it)
            }
        }

    override suspend fun savePointItem(item: PointItem) {
        searchHistoryDao.apply {
            if (!checkExist(item.text)) {
                if (getCount() == 20)
                    deleteEarliestPointItem()
                insertPointItem(pointMapper.mapPointEntityToPointDbModel(item))
            }
        }
    }

    override fun loadRouteHistory(): LiveData<List<RouteItem>> =
        routeHistoryDao.getRouteHistory().map { list ->
            list.map {
                routeMapper.mapRouteDbModelToRouteEntity(it)
            }
        }

    override suspend fun saveRoute(
        route: Road,
        points: String,
        startPoint: PointItem,
        additionalPoints: List<PointItem>?,
        finishPoint: PointItem?
    ) {
        routeHistoryDao.apply {
            if (!checkExist(route.mRouteHigh)) {
                if (getCount() == 10)
                    deleteEarliestRoute()
                saveRoute(
                    routeMapper.mapRouteEntityToRouteDbModel(
                        RouteItem(
                            route,
                            points,
                            startPoint,
                            additionalPoints,
                            finishPoint
                        )
                    )
                )
            }
        }
    }

    override suspend fun createRoute(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (RouteError) -> Unit
    ) = optimalRouteSearchUtil.getRoute(points, withEndPoint, onErrorEventListener)
}