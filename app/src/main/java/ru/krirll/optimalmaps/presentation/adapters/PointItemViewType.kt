package ru.krirll.optimalmaps.presentation.adapters

import ru.krirll.optimalmaps.R

//ViewTypes in PointListAdapter
enum class PointItemViewType(val layout: Int) {

    DEFAULT_POINT(R.layout.point_default_item),
    HISTORY_POINT(R.layout.point_history_item);
}
