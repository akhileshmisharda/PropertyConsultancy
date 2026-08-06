package com.example.propertyconsultancy.data.cache

import android.content.Context
import com.example.propertyconsultancy.data.dto.CategoryGroupDTO
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CategoryCache {
    private const val PREFS_NAME = "CategoryCache"
    private const val KEY_CATEGORIES = "categories_json"

    fun saveCategories(context: Context, categories: List<CategoryGroupDTO>) {
        val json = Gson().toJson(categories)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CATEGORIES, json).apply()
    }

    fun getCategories(context: Context): List<CategoryGroupDTO>? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CATEGORIES, null) ?: return null
        val type = object : TypeToken<List<CategoryGroupDTO>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}
