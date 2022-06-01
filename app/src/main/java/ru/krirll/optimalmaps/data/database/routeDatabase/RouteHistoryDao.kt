package ru.krirll.optimalmaps.data.database.routeDatabase

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.krirll.optimalmaps.domain.model.PointItem

@Dao
interface RouteHistoryDao {

    @Insert
    suspend fun saveRoute(route: RouteItemDbModel)

    @Query("SELECT * FROM RouteItemDbModel")
    fun getRouteHistory(): LiveData<List<RouteItemDbModel>>

    @Query("SELECT EXISTS(SELECT * FROM RouteItemDbModel WHERE start_point == :startPoint AND additional_points == :additionalPoints AND finish_point == :finishPoint)")
    suspend fun checkExist(
        startPoint: PointItem,
        additionalPoints: List<PointItem>?,
        finishPoint: PointItem?
    ): Boolean

    @Query("SELECT COUNT(id) FROM RouteItemDbModel")
    suspend fun getCount(): Int

    @Query("DELETE FROM RouteItemDbModel WHERE id = (SELECT MIN(id) FROM RouteItemDbModel)")
    suspend fun deleteEarliestRoute()
}