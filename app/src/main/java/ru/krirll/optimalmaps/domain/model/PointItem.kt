package ru.krirll.optimalmaps.domain.model

data class PointItem(
    val zoom: Double,
    val text: String,
    val isHistorySearch: Boolean,
    val lat: Double,
    val lon: Double,
)