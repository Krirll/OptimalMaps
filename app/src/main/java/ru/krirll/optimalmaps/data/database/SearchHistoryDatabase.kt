package ru.krirll.optimalmaps.data.database

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.krirll.optimalmaps.data.database.model.PointItemDbModel

@Database(
    entities = [PointItemDbModel::class],
    version = 1,
    exportSchema = false
)
abstract class SearchHistoryDatabase : RoomDatabase() {

    abstract fun searchDao(): SearchHistoryDao

    companion object {
        private var INSTANCE: SearchHistoryDatabase? = null
        private val LOCK = Any()
        private const val DATABASE_NAME = "SearchHistoryDatabase"

        fun getInstance(application: Application): SearchHistoryDatabase =
            synchronized(LOCK) { //it is necessary to prevent conflicts
                if (INSTANCE == null)
                    INSTANCE =
                        Room.databaseBuilder(
                            application,
                            SearchHistoryDatabase::class.java,
                            DATABASE_NAME
                        ).build()
                return INSTANCE!!
            }
    }
}