package com.example.npucourse.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 瓜瓜课程表自托管更新检查。
 *
 * 更新元数据由 GitHub 仓库 main 分支的 update/latest.json 提供。
 * App 仅负责检查并打开官方发布页，不静默安装 APK。
 */
data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val title: String,
    val changelog: List<String>,
    val downloadUrl: String,
    val publishedAt: String,
    val forceUpdate: Boolean
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object AppUpdateManager {

    const val UPDATE_MANIFEST_URL =
        "https://raw.githubusercontent.com/chyyy1006-rgb/guaguacourse/main/update/latest.json"

    const val REPOSITORY_URL =
        "https://github.com/chyyy1006-rgb/guaguacourse"

    const val RELEASES_URL =
        "https://github.com/chyyy1006-rgb/guaguacourse/releases"

    private const val PREFS = "guagua_update_center"
    private const val KEY_LAST_CHECK_AT = "last_check_at"
    private const val KEY_LAST_PROMPTED_VERSION = "last_prompted_version"
    private const val KEY_LAST_PROMPTED_AT = "last_prompted_at"

    private const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    private const val RE_PROMPT_INTERVAL_MS = 24L * 60L * 60L * 1000L

    fun currentVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    fun currentVersionName(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return info.versionName ?: "未知"
    }

    fun shouldAutoCheck(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val last = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_CHECK_AT, 0L)
        return now - last >= AUTO_CHECK_INTERVAL_MS
    }

    fun shouldPromptUpdate(
        context: Context,
        info: AppUpdateInfo,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val promptedVersion = prefs.getLong(KEY_LAST_PROMPTED_VERSION, -1L)
        val promptedAt = prefs.getLong(KEY_LAST_PROMPTED_AT, 0L)
        return promptedVersion != info.versionCode || now - promptedAt >= RE_PROMPT_INTERVAL_MS
    }

    fun markPrompted(context: Context, versionCode: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_PROMPTED_VERSION, versionCode)
            .putLong(KEY_LAST_PROMPTED_AT, System.currentTimeMillis())
            .apply()
    }

    suspend fun checkForUpdates(
        context: Context,
        manual: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!manual && !shouldAutoCheck(context)) {
            return@withContext UpdateCheckResult.UpToDate(currentVersionName(context))
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis()).apply()

        try {
            val cacheBuster = System.currentTimeMillis()
            val url = URL("$UPDATE_MANIFEST_URL?t=$cacheBuster")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "GuaguaCourse-Android")
            }

            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    return@withContext UpdateCheckResult.Failed("更新服务器返回 HTTP $status")
                }

                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(body)
                val info = parseUpdateInfo(json)
                    ?: return@withContext UpdateCheckResult.Failed("更新信息格式不正确")

                val currentCode = currentVersionCode(context)
                if (info.versionCode > currentCode) {
                    UpdateCheckResult.UpdateAvailable(info)
                } else {
                    UpdateCheckResult.UpToDate(currentVersionName(context))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(
                when {
                    e.message.isNullOrBlank() -> "无法连接更新服务器，请稍后重试"
                    else -> "检查更新失败：${e.message}"
                }
            )
        }
    }

    private fun parseUpdateInfo(json: JSONObject): AppUpdateInfo? {
        val versionCode = json.optLong("versionCode", -1L)
        val versionName = json.optString("versionName", "").trim()
        val downloadUrl = json.optString("downloadUrl", "").trim()
        if (versionCode <= 0L || versionName.isBlank() || !downloadUrl.startsWith("https://")) {
            return null
        }

        val changelogJson = json.optJSONArray("changelog")
        val changelog = buildList {
            if (changelogJson != null) {
                for (index in 0 until changelogJson.length()) {
                    val item = changelogJson.optString(index).trim()
                    if (item.isNotBlank()) add(item)
                }
            }
        }

        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            title = json.optString("title", "瓜瓜课程表 $versionName").ifBlank {
                "瓜瓜课程表 $versionName"
            },
            changelog = changelog,
            downloadUrl = downloadUrl,
            publishedAt = json.optString("publishedAt", "").trim(),
            forceUpdate = json.optBoolean("forceUpdate", false)
        )
    }

    fun openDownloadPage(context: Context, url: String) {
        if (!url.startsWith("https://")) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openRepository(context: Context) {
        openDownloadPage(context, REPOSITORY_URL)
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
