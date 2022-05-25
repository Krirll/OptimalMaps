package ru.krirll.optimalmaps.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.krirll.optimalmaps.data.database.model.PointItemDbModel

@Dao
interface SearchHistoryDao {

    //get all data, sorted by descending order
    @Query("SELECT * FROM PointItemDbModel ORDER BY id DESC")
    fun getSearchHistory(): LiveData<List<PointItemDbModel>>

    @Insert
    suspend fun insertPointItem(item: PointItemDbModel)

    //delete all data
    @Query("DELETE FROM PointItemDbModel")
    suspend fun deleteSearchHistory()

    //delete the earliest point item where id = min(id)
    @Query("DELETE FROM PointItemDbModel WHERE id = (SELECT MIN(id) FROM PointItemDbModel)")
    suspend fun deleteEarliestPointItem()

    @Query("SELECT COUNT(id) FROM PointItemDbModel")
    suspend fun getCount(): Int

    @Query("SELECT EXISTS (SELECT * FROM PointItemDbModel WHERE text == :text)")
    suspend fun checkExist(text: String): Boolean
}