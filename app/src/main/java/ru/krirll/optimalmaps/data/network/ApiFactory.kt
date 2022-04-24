package ru.krirll.optimalmaps.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiFactory {
    companion object {
        val searchApiService: SearchApiService =
            Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://nominatim.openstreetmap.org/")
                .build()
                .create(SearchApiService::class.java)
    }
}