package ru.krirll.optimalmaps.utils

import android.content.Context
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import ru.krirll.optimalmaps.BuildConfig
import ru.krirll.optimalmaps.domain.model.PointItem
import ru.krirll.optimalmaps.presentation.enums.RouteError

class OptimalRouteSearchUtil(private val context: Context) {

    fun getRoute(
        points: List<PointItem>,
        withEndPoint: Boolean,
        onErrorEventListener: (RouteError) -> Unit
    ): Road? {
        val rm = OSRMRoadManager(
            context,
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME
        ).apply {
            setMean(OSRMRoadManager.MEAN_BY_FOOT)
        }
        var endPoint: PointItem? = null
        var resultRoad: Road? = null
        val pointsCopy = points.toMutableList()
        pointsCopy.removeFirst()
        if (withEndPoint && points.size > 2) {
            endPoint = pointsCopy.last()
            pointsCopy.remove(endPoint)
        }
        if (points.size <= 7) {
            val listOfRoads = getAllRoads(points, rm, onErrorEventListener)
            if (listOfRoads != null) {
                var minRoad: Road? = null
                var buffer: MutableList<Int> = mutableListOf()

                pointsCopy.forEach { _ -> buffer.add(0) }
                var res: Pair<MutableList<Int>, Road>
                do {
                    res = getRoadForCurrentBranch(
                        buffer.toMutableList(),
                        pointsCopy,
                        points[0],
                        listOfRoads
                    )
                    buffer = res.first
                    if (withEndPoint && endPoint != null) {
                        res =
                            Pair(
                                res.first,
                                sumRoads(
                                    res.second,
                                    getRoad(
                                        arrayListOf(
                                            res.second.mNodes.last().mLocation,
                                            GeoPoint(endPoint.lat, endPoint.lon)
                                        ),
                                        rm
                                    )
                                )
                            )
                    }
                    if (minRoad == null || minRoad.mDuration > res.second.mDuration)
                        minRoad = res.second
                } while (res.first.count { it == 0 } != res.first.size)
                resultRoad = minRoad
            }
        }
        if (resultRoad != null) {
            if (resultRoad.mLength > 1000) {
                sendError(ROUTE_TOO_BIG, onErrorEventListener)
            } else {
                if (resultRoad.mStatus != Road.STATUS_OK) {
                    sendError(resultRoad.mStatus, onErrorEventListener)
                    resultRoad = null
                }
            }
        }
        return resultRoad
    }

    private fun getRoadForCurrentBranch(
        listInt: MutableList<Int>,
        listPoints: MutableList<PointItem>,
        currentPoint: PointItem,
        listOfRoads: List<Triple<PointItem, PointItem, Road>>
    ): Pair<MutableList<Int>, Road> {
        if (listPoints.size == 1) {
            return Pair(listInt, listOfRoads.first {
                it.first == currentPoint && it.second == listPoints[0]
            }.third)
        } else {
            var myIndex = listInt.first()
            listInt.removeFirst()
            val res = getRoadForCurrentBranch(
                listInt.toMutableList(),
                mutableListOf<PointItem>().apply {
                    listPoints.forEachIndexed { index, pointItem ->
                        if (index != myIndex)
                            add(pointItem)
                    }
                },
                listPoints[myIndex],
                listOfRoads
            )
            val oldIndex = myIndex
            if (res.first.count { it == 0 } == res.first.size) {
                myIndex++
                if (listPoints.size - 1 < myIndex)
                    myIndex = 0
            }
            res.first.add(0, myIndex)
            return Pair(res.first, sumRoads(listOfRoads.first {
                it.first == currentPoint && it.second == listPoints[oldIndex]
            }.third, res.second))
        }
    }

    private fun sumRoads(r1: Road, r2: Road): Road =
        Road().apply {
            mStatus =
                if (r1.mStatus == Road.STATUS_OK && r1.mStatus == Road.STATUS_OK)
                    Road.STATUS_OK
                else
                    Road.STATUS_INVALID
            mLength = r1.mLength + r2.mLength
            mDuration = r1.mDuration + r2.mDuration
            mNodes = ArrayList(r1.mNodes + r2.mNodes)
            mRouteHigh = ArrayList(r1.mRouteHigh + r2.mRouteHigh)
        }

    private fun getAllRoads(
        list: List<PointItem>,
        rm: RoadManager,
        onErrorEventListener: (RouteError) -> Unit
    ): List<Triple<PointItem, PointItem, Road>>? {
        var listOfRoads: MutableList<Triple<PointItem, PointItem, Road>>? = mutableListOf()
        for (i in list.indices) {
            if (listOfRoads != null) {
                for (j in i + 1 until list.size) {
                    listOfRoads.add(
                        Triple(
                            list[i],
                            list[j],
                            getRoad(
                                arrayListOf(
                                    GeoPoint(list[i].lat, list[i].lon),
                                    GeoPoint(list[j].lat, list[j].lon)
                                ),
                                rm
                            )
                        )
                    )
                    listOfRoads.add(
                        Triple(
                            list[j],
                            list[i],
                            getRoad(
                                arrayListOf(
                                    GeoPoint(list[j].lat, list[j].lon),
                                    GeoPoint(list[i].lat, list[i].lon)
                                ),
                                rm
                            )
                        )
                    )
                }
            }
        }
        listOfRoads?.firstOrNull { it.third.mStatus != Road.STATUS_OK }?.let {
            sendError(it.third.mStatus, onErrorEventListener)
            listOfRoads = null
        }
        return listOfRoads
    }

    private fun getRoad(geoPoints: ArrayList<GeoPoint>, roadManager: RoadManager) =
        roadManager.getRoad(geoPoints)

    private fun sendError(error: Int, onErrorEventListener: (RouteError) -> Unit) {
        when (error) {
            Road.STATUS_INVALID -> onErrorEventListener(RouteError.ROUTE_INVALID)
            Road.STATUS_TECHNICAL_ISSUE -> onErrorEventListener(RouteError.ROUTE_TECHNICAL_ISSUE)
            ROUTE_TOO_BIG -> onErrorEventListener(RouteError.ROUTE_TOO_BIG)
            else -> {
                /*nothing*/
            }
        }
    }

    companion object {
        private const val ROUTE_TOO_BIG = 10
    }
}