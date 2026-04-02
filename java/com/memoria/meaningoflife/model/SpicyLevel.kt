package com.memoria.meaningoflife.model

enum class SpicyLevel(val value: Int, val text: String, val icon: String, val colorRes: Int) {
    NONE(0, "不辣", "🌶️", com.memoria.meaningoflife.R.color.spicy_none),
    MILD(1, "微辣", "🌶️", com.memoria.meaningoflife.R.color.spicy_mild),
    MEDIUM(2, "中辣", "🌶️🌶️", com.memoria.meaningoflife.R.color.spicy_medium),
    HOT(3, "特辣", "🌶️🌶️🌶️", com.memoria.meaningoflife.R.color.spicy_hot);

    companion object {
        fun fromValue(value: Int): SpicyLevel {
            return values().find { it.value == value } ?: NONE
        }

        fun getAllTexts(): List<String> = values().map { it.text }
    }
}