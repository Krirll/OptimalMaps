package ru.krirll.optimalmaps.utils

import android.content.Context
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import ru.krirll.optimalmaps.BuildConfig
import ru.krirll.optimalmaps.domain.model.PointItem

class OptimalRouteSearchUtil(private val context: Context) {

    fun getRoute(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (Int) -> Unit
    ): Road? =
        getOptimalRoute(points, withEndPoint, onErrorEventListener)

    private fun getOptimalRoute(
        points: List<PointItem>,
        withEndPoint: Boolean = false,
        onErrorEventListener: (Int) -> Unit
    ): Road? {
        val rm = OSRMRoadManager(
            context,
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME
        ).apply {
            setMean(OSRMRoadManager.MEAN_BY_FOOT)
        }
        var resultRoad: Road? = null
        var startPoint: GeoPoint? = null
        var secondPoint: GeoPoint? = null
        if (points.size > 1) {
            startPoint = GeoPoint(points[0].lat, points[0].lon)
            secondPoint = GeoPoint(points[1].lat, points[1].lon)
        }
        if (points.size == 2) {
            resultRoad = getRoad(arrayListOf(startPoint!!, secondPoint!!), rm)
            if (resultRoad?.mStatus != Road.STATUS_OK) resultRoad = null
        } else {
            if (points.size == 3) {
                val thirdPoint = GeoPoint(points[2].lat, points[2].lon)
                if (withEndPoint) {
                    resultRoad = getRoad(arrayListOf(startPoint!!, secondPoint!!, thirdPoint), rm)
                    if (resultRoad?.mStatus != Road.STATUS_OK) {
                        onErrorEventListener(resultRoad?.mStatus!!)
                        resultRoad = null
                    }
                } else {
                    val firstRoadFromStart = getRoad(arrayListOf(startPoint!!, secondPoint!!), rm)
                    if (firstRoadFromStart.mStatus == Road.STATUS_OK) {
                        val secondRoadFromStart = getRoad(arrayListOf(startPoint, thirdPoint), rm)
                        if (secondRoadFromStart.mStatus == Road.STATUS_OK) {
                            resultRoad =
                                getRoad(
                                    arrayListOf(
                                        startPoint
                                    ).apply {
                                        if (firstRoadFromStart.mDuration > secondRoadFromStart.mDuration) {
                                            add(thirdPoint)
                                            add(secondPoint)
                                        } else {
                                            add(secondPoint)
                                            add(thirdPoint)
                                        }
                                    },
                                    rm
                                )
                            if (resultRoad?.mStatus != Road.STATUS_OK) {
                                onErrorEventListener(resultRoad?.mStatus!!)
                                resultRoad = null
                            }
                        } else
                            onErrorEventListener(secondRoadFromStart.mStatus)
                    } else
                        onErrorEventListener(firstRoadFromStart.mStatus)
                }
            } else {
                resultRoad = getOptimalRouteByList(points, withEndPoint, onErrorEventListener)
            }
        }
        return resultRoad
    }

    private fun getOptimalRouteByList(
        listOfPoints: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (Int) -> Unit
    ): Road? {
        val listOfRoads: MutableList<Road>? = null
        //здесь будет алгоритм
        /*for (i in points.indices) {
            val currentPoint = GeoPoint(points[i].lat, points[i].lon)
            if (i + 1 != points.size) {
                val nextPoint = GeoPoint(points[i + 1].lat, points[i + 1].lon)
                listOfRoads?.add(
                    rm.getRoad(
                        arrayListOf(
                            currentPoint,
                            nextPoint
                        )
                    )
                )
            }
        }*/
        return null
    }

    private fun getRoad(geoPoints: ArrayList<GeoPoint>, roadManager: RoadManager) =
        roadManager.getRoad(geoPoints)
}