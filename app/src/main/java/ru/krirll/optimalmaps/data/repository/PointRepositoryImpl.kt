package ru.krirll.optimalmaps.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import org.osmdroid.bonuspack.routing.Road
import ru.krirll.optimalmaps.data.database.routeDatabase.RouteHistoryDao
import ru.krirll.optimalmaps.data.database.routeDatabase.RouteHistoryDatabase
import ru.krirll.optimalmaps.data.database.searchDatabase.SearchHistoryDao
import ru.krirll.optimalmaps.data.database.searchDatabase.SearchHistoryDatabase
import ru.krirll.optimalmaps.data.mapper.PointMapper
import ru.krirll.optimalmaps.data.network.ApiFactory
import ru.krirll.optimalmaps.data.network.SearchApiService
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.model.RouteItem
import ru.krirll.optimalmaps.domain.repository.PointRepository
import ru.krirll.optimalmaps.presentation.enums.RouteError
import ru.krirll.optimalmaps.utils.OptimalRouteSearchUtil

class PointRepositoryImpl(
    private val application: Application,
    private val mapper: PointMapper = PointMapper(),
    private val searchHistoryDao: SearchHistoryDao = SearchHistoryDatabase.getInstance(application).searchDao(),
    private val routeHistoryDao: RouteHistoryDao = RouteHistoryDatabase.getInstance(application).routeDao(),
    private val apiService: SearchApiService = ApiFactory.searchApiService,
    private val optimalRouteSearchUtil: OptimalRouteSearchUtil = OptimalRouteSearchUtil(application.applicationContext)
) : PointRepository {

    override suspend fun getSearchResult(
        query: String,
        locale: String,
        emptyResultEventListener: () -> Unit, //when result is empty
        noInternetEventListener: () -> Unit   //when was exception
    ): List<PointItem> {
        var result: List<PointItem> = listOf()
        try {
            //get result from api and map result items to PointItem
            result = apiService.getSearchResult(query, locale).map { mapper.mapPointDtoToPointEntity(it) }
            if (result.isEmpty())
                emptyResultEventListener.invoke()
        } catch (t: Throwable) {
            noInternetEventListener.invoke()
        }
        return result
    }

    override fun loadSearchHistory(): LiveData<List<PointItem>> =
        //Transformations is needed for editing LiveData
        Transformations.map(searchHistoryDao.getSearchHistory()) { it ->
            it.map {
                mapper.mapPointDbModelToPointEntity(it)
            }
        }

    override suspend fun savePointItem(item: PointItem) {
        searchHistoryDao.apply {
            if (!checkExist(item.text)) {
                if (getCount() == 20)
                    deleteEarliestPointItem()
                insertPointItem(mapper.mapPointEntityToPointDbModel(item))
            }
        }
    }

    override fun loadRouteHistory(): LiveData<List<RouteItem>> =
        Transformations.map(routeHistoryDao.getRouteHistory()) { it ->
            it.map {
                mapper.mapRouteDbModelToRouteEntity(it)
            }
        }

    override suspend fun saveRoute(route: Road, points: String, list: List<PointItem>) {
        routeHistoryDao.apply {
            if (!checkExist(list)) {
                if (getCount() == 10)
                    deleteEarliestRoute()
                saveRoute(mapper.mapRouteEntityToRouteDbModel(RouteItem(route, points, list)))
            }
        }
    }

    override suspend fun createRoute(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (RouteError) -> Unit
    ) = optimalRouteSearchUtil.getRoute(points, withEndPoint, onErrorEventListener)
}