package com.memoria.meaningoflife.model

enum class Weather(val value: Int, val text: String, val icon: String) {
    SUNNY(0, "晴", "☀️"),
    CLOUDY(1, "阴", "☁️"),
    RAINY(2, "雨", "🌧️"),
    SNOWY(3, "雪", "❄️"),
    FOGGY(4, "雾", "🌫️"),
    PARTLY_CLOUDY(5, "多云", "⛅");

    companion object {
        fun fromValue(value: Int): Weather {
            return values().find { it.value == value } ?: SUNNY
        }
    }

    fun toValue(): Int = value
}