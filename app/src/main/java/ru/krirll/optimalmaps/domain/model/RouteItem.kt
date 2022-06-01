package ru.krirll.optimalmaps.domain.model

import org.osmdroid.bonuspack.routing.Road

data class RouteItem(
    val route: Road,
    val points: String,
    val startPoint: PointItem,
    val additionalPoints: List<PointItem>?,
    val finishPoint: PointItem?
)
