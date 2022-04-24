package ru.krirll.optimalmaps.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PointItemDbModel(

    @ColumnInfo(name = "zoom")
    val zoom: Double,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "isHistorySearch")
    val isHistorySearch: Boolean,

    @ColumnInfo(name = "lat")
    val lat: Double,

    @ColumnInfo(name = "lon")
    val lon: Double

) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}