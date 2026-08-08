package com.example.propertyconsultancy.utils

object UrlUtils {
    /**
     * Appends a cache-busting timestamp to the URL to ensure the latest version is fetched from the server.
     * Refresh interval: 5 minutes.
     */
    fun getPropertyImageUrl(url: String?): String? {
        if (url == null) return null
        
        // Clean existing version parameters to avoid accumulation
        val cleanUrl = url.substringBefore("?")
        
        val fullUrl = if (!cleanUrl.startsWith("http")) {
            "http://fabkraft.in/property/$cleanUrl"
        } else {
            cleanUrl
        }
        
        // Add a fresh timestamp
        val cacheBuster = System.currentTimeMillis() / (1000 * 60 * 5)
        return "$fullUrl?v=$cacheBuster"
    }
}
