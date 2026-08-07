package com.example.propertyconsultancy.utils

object UrlUtils {
    /**
     * Appends a cache-busting timestamp to the URL to ensure the latest version is fetched from the server.
     * Refresh interval: 5 minutes.
     */
    fun getPropertyImageUrl(url: String?): String? {
        if (url == null) return null
        
        val fullUrl = if (!url.startsWith("http")) {
            "http://fabkraft.in/property/$url"
        } else {
            url
        }
        
        // Add a timestamp that changes every 5 minutes to bypass cache but still allow some reuse
        val cacheBuster = System.currentTimeMillis() / (1000 * 60 * 5)
        return if (fullUrl.contains("?")) {
            "$fullUrl&v=$cacheBuster"
        } else {
            "$fullUrl?v=$cacheBuster"
        }
    }
}
