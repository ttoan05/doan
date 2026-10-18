package com.example.foodhub.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object SearchHistoryManager {
    private const val PREF_NAME = "foodhub_search_history"
    private const val KEY_HISTORY = "recent_queries"
    private const val MAX_HISTORY = 10

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getRecentSearches(context: Context): List<String> {
        val jsonStr = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "").trim()
                if (item.isNotEmpty()) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addSearchQuery(context: Context, query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return

        val current = getRecentSearches(context).toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        if (current.size > MAX_HISTORY) {
            current.subList(MAX_HISTORY, current.size).clear()
        }

        saveList(context, current)
    }

    fun removeSearchQuery(context: Context, query: String) {
        val current = getRecentSearches(context).toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        saveList(context, current)
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }

    private fun saveList(context: Context, list: List<String>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(item)
        }
        getPrefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
