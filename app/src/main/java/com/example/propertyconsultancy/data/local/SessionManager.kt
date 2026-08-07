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
        private const val KEY_ACTIVITY_LOGS = "activityLogs"
        private const val KEY_HINTS_ENABLED = "hintsEnabled"
    }

    fun isHintsEnabled(): Boolean {
        return prefs.getBoolean(KEY_HINTS_ENABLED, true)
    }

    fun setHintsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HINTS_ENABLED, enabled).apply()
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

    fun addActivityLog(title: String, detail: String, type: String = "info") {
        val logs = getActivityLogs().toMutableList()
        val newLog = com.example.propertyconsultancy.data.dto.ActivityLogDTO(
            System.currentTimeMillis(), title, detail, type
        )
        logs.add(0, newLog) // Add to top (reverse chronological)
        
        // Keep only last 100 logs
        val trimmedLogs = if (logs.size > 100) logs.take(100) else logs
        
        prefs.edit().putString(KEY_ACTIVITY_LOGS, gson.toJson(trimmedLogs)).apply()
    }

    fun getActivityLogs(): List<com.example.propertyconsultancy.data.dto.ActivityLogDTO> {
        val json = prefs.getString(KEY_ACTIVITY_LOGS, null)
        return if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<com.example.propertyconsultancy.data.dto.ActivityLogDTO>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
