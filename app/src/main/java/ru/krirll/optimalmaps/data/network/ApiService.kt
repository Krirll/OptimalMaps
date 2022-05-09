package ru.krirll.optimalmaps.data.network

import retrofit2.http.GET
import retrofit2.http.Query
import ru.krirll.optimalmaps.data.network.model.PointItemDto

interface SearchApiService {

    //get retrofit query, return addresses by string
    @GET("search?format=json&addressdetails=1&limit=50")
    suspend fun getSearchResult(
        @Query(QUERY_PARAM_Q) query: String,
        @Query(LANGUAGE) locale: String
    ): List<PointItemDto>

    companion object {
        private const val QUERY_PARAM_Q = "q"
        private const val LANGUAGE = "accept-language"
    }
}