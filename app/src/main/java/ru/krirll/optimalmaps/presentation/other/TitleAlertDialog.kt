package ru.krirll.optimalmaps.presentation.other

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ru.krirll.optimalmaps.R

@Parcelize
enum class TitleAlertDialog(val stringRes: Int) : Parcelable {
    LOCATION_PERMISSION_DENIED(R.string.title_location_permission),
    GEO_POSITION_DISABLED(R.string.title_geo_position),
    INTERNET(R.string.internet)
}