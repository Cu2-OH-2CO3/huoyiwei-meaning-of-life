package com.memoria.meaningoflife.model

enum class Mood(val value: Int, val text: String, val icon: String) {
    TERRIBLE(0, "糟糕", "😫"),
    BAD(1, "低落", "😔"),
    NORMAL(2, "平静", "😐"),
    GOOD(3, "开心", "🙂"),
    EXCITED(4, "兴奋", "🤩");

    companion object {
        fun fromValue(value: Int): Mood {
            return values().find { it.value == value } ?: NORMAL
        }

        fun fromText(text: String): Mood? {
            return values().find { it.text == text }
        }
    }

    fun toValue(): Int = value
}