package com.example.propertyconsultancy.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.propertyconsultancy.data.dto.UserDTO
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "PropertyConsultancyPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_DATA = "userData"
        private const val KEY_DASHBOARD_DATA = "dashboardData"
    }

    fun saveUser(user: UserDTO) {
        val json = gson.toJson(user)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_DATA, json)
            apply()
        }
    }

    fun getUser(): UserDTO? {
        val json = prefs.getString(KEY_USER_DATA, null)
        return if (json != null) {
            gson.fromJson(json, UserDTO::class.java)
        } else {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun saveDashboardData(data: String) {
        prefs.edit().putString(KEY_DASHBOARD_DATA, data).apply()
    }

    fun getDashboardData(): String? {
        return prefs.getString(KEY_DASHBOARD_DATA, null)
    }

    fun saveSearchFilters(filters: Map<String, Any?>) {
        val editor = prefs.edit()
        filters.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString("filter_$key", value)
                is Int -> editor.putInt("filter_$key", value)
                is Float -> editor.putFloat("filter_$key", value)
                is Long -> editor.putLong("filter_$key", value)
                is Boolean -> editor.putBoolean("filter_$key", value)
                is Double -> editor.putFloat("filter_$key", value.toFloat())
                is List<*> -> editor.putString("filter_$key", gson.toJson(value))
                null -> editor.remove("filter_$key")
            }
        }
        editor.apply()
    }

    fun getSearchFilters(): Map<String, Any?> {
        val all = prefs.all
        return all.filterKeys { it.startsWith("filter_") }
            .mapKeys { it.key.removePrefix("filter_") }
    }

    fun clearSearchFilters() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("filter_") }.forEach { editor.remove(it) }
        editor.apply()
    }

    fun savePageSize(size: Int) {
        prefs.edit().putInt("search_page_size", size).apply()
    }

    fun getPageSize(): Int {
        return prefs.getInt("search_page_size", 5)
    }
}
