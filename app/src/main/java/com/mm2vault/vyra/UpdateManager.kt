package com.mm2vault.vyra

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckResult {
    data class Available(val version: String, val apkUrl: String) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
}

object UpdateManager {
    fun check(context: Context): UpdateCheckResult {
        val connection = URL("https://api.github.com/repos/mm2vault/VYRA-Next/releases/latest").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode !in 200..299) error("GitHub release check failed: HTTP " + connection.responseCode)
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = release.optString("tag_name").removePrefix("v").ifBlank { error("Release tag is missing") }
            val assets = release.optJSONArray("assets") ?: error("Release has no assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            val url = apkUrl?.takeIf { it.startsWith("https://") } ?: error("Release APK not found")
            val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
            return if (compareVersions(version, current) > 0) UpdateCheckResult.Available(version, url) else UpdateCheckResult.UpToDate
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
