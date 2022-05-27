package ru.krirll.optimalmaps.domain.model

import org.osmdroid.bonuspack.routing.Road

data class RouteItem(
    val route: Road,
    val points: String,
    val list: List<PointItem>
)
