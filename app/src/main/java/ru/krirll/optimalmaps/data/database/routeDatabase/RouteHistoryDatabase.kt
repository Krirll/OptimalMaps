package ru.krirll.optimalmaps.data.database.routeDatabase

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.krirll.optimalmaps.data.database.routeDatabase.converters.PointItemConverter
import ru.krirll.optimalmaps.data.database.routeDatabase.converters.PolylineConverter
import ru.krirll.optimalmaps.data.database.routeDatabase.converters.RoadNodeConverter

@Database(
    entities = [RouteItemDbModel::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    PolylineConverter::class,
    RoadNodeConverter::class,
    PointItemConverter::class
)
abstract class RouteHistoryDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteHistoryDao

    companion object {
        private var INSTANCE: RouteHistoryDatabase? = null
        private const val DATABASE_NAME = "RouteHistoryDatabase"

        fun getInstance(application: Application): RouteHistoryDatabase =
            synchronized(RouteHistoryDatabase::class) { //it is necessary to prevent conflicts
                if (INSTANCE == null)
                    INSTANCE =
                        Room.databaseBuilder(
                            application,
                            RouteHistoryDatabase::class.java,
                            DATABASE_NAME
                        ).build()
                return INSTANCE!!
            }
    }
}