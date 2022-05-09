package ru.krirll.optimalmaps.presentation.enums

enum class Locale(private val locale: String) {
    RU("ru-RU"),
    EN("en-US");

    companion object {
        fun getLocale(str: String) =
            values().firstOrNull {
                it.locale == str
            }?.locale ?: ""
    }
}