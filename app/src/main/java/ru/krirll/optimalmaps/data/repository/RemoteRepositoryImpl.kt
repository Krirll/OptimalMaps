package ru.krirll.optimalmaps.data.repository

import ru.krirll.optimalmaps.data.mapper.PointMapper
import ru.krirll.optimalmaps.data.network.ApiFactory
import ru.krirll.optimalmaps.data.network.SearchApiService
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.domain.repository.RemoteRepository

class RemoteRepositoryImpl(
    private val apiService: SearchApiService = ApiFactory.searchApiService,
    private val pointMapper: PointMapper = PointMapper()
) : RemoteRepository {


    override suspend fun getSearchResult(
        query: String,
        locale: String,
        emptyResultEventListener: () -> Unit, //when result is empty
        noInternetEventListener: () -> Unit   //when was exception
    ): List<PointItem> {
        var result: List<PointItem> = listOf()
        try {
            //get result from api and map result items to PointItem
            result = apiService.getSearchResult(query, locale)
                .map { pointMapper.mapPointDtoToPointEntity(it) }
            if (result.isEmpty())
                emptyResultEventListener.invoke()
        } catch (t: Throwable) {
            noInternetEventListener.invoke()
        }
        return result
    }

}