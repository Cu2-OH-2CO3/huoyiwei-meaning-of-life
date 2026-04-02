package com.memoria.meaningoflife.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class QuoteManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 默认语句库
    private val defaultQuotes = listOf(
        "记录创作，品味生活",
        "活着呗",
        "打游戏摇不到人好想哭",
        "前面忘了，后面也是",
        "有早八之物，呵呵",
        "本来想叫活了么，但是感觉有点蹭死了么",
        "活着的意义之一是吃饭",
        "活意味啊？",
        "其实叫吃点东西也不错，我饿了",
        "不错不错，继续活着"
    )

    fun getQuotes(): List<String> {
        val json = prefs.getString("quotes", null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            defaultQuotes
        }
    }

    fun saveQuotes(quotes: List<String>) {
        val json = gson.toJson(quotes)
        prefs.edit().putString("quotes", json).apply()
    }

    fun addQuote(quote: String) {
        val quotes = getQuotes().toMutableList()
        quotes.add(quote)
        saveQuotes(quotes)
    }

    fun deleteQuote(index: Int) {
        val quotes = getQuotes().toMutableList()
        if (index in quotes.indices) {
            quotes.removeAt(index)
            saveQuotes(quotes)
        }
    }

    fun updateQuote(index: Int, newQuote: String) {
        val quotes = getQuotes().toMutableList()
        if (index in quotes.indices) {
            quotes[index] = newQuote
            saveQuotes(quotes)
        }
    }

    fun getDailyQuote(): String {
        val quotes = getQuotes()
        if (quotes.isEmpty()) return "记录创作，品味生活"

        // 根据日期获取固定索引，实现每日刷新
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = today % quotes.size
        return quotes[index]
    }
}