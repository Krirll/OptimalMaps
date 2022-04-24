package ru.krirll.optimalmaps.data.network.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PointItemDto(

    @SerializedName("importance")
    @Expose
    val importance: String,

    @SerializedName("display_name")
    @Expose
    val text: String,

    @SerializedName("lat")
    @Expose
    val lat: Float,

    @SerializedName("lon")
    @Expose
    val lon: Float
)