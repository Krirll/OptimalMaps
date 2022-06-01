package ru.krirll.optimalmaps.presentation.enums

enum class PointZoom(private val importance: Double, val zoom: Double) {

    SMALL_1(0.1, 20.0),
    SMALL_2(0.2, 18.0),
    SMALL_3(0.3, 16.0),
    MEDIUM_1(0.4, 14.0),
    MEDIUM_2(0.5, 12.0),
    MEDIUM_3(0.6, 10.0),
    LARGE_1(0.7, 8.0),
    LARGE_2(0.8,6.0),
    LARGE_3(0.9, 4.0);

    companion object {

        fun getZoomByImportance(value: String): Double {
            var type = MEDIUM_2
            if (value != "") {
                val valueDouble = value.toDouble()
                type =
                    if (valueDouble > 0.1)
                        values().first {
                            it.importance == (valueDouble * 10).toInt().toDouble() / 10
                        }
                    else
                        SMALL_1
            }
            return type.zoom
        }

        fun getZoomByRouteLength(length: Double): Double =
            when(length.toInt()) {
                in 0..5 -> SMALL_3.zoom
                in 5..10 -> MEDIUM_1.zoom
                in 10..15 -> MEDIUM_2.zoom
                in 15..200 -> MEDIUM_3.zoom
                in 200..500 -> LARGE_1.zoom
                in 500..1500 -> LARGE_2.zoom
                else -> LARGE_3.zoom
            }
    }
}