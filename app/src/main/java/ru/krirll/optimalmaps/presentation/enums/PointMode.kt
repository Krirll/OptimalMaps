package ru.krirll.optimalmaps.presentation.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class PointMode : Parcelable {
    CURRENT_LOCATION_POINT,
    CURRENT_LOCATION_IN_CONSTRUCTOR,
    START_POINT,
    ADDITIONAL_POINT_IN_CONSTRUCTOR,
    ADDITIONAL_POINT_ADD,
    ADDITIONAL_POINT_EDIT,
    FINISH_POINT,
    DEFAULT_POINT
}