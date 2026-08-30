package com.example.npucourse.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 瓜瓜课程表自托管更新中心。
 *
 * update/latest.json 只负责提供版本元数据与正式 APK 地址；
 * 新版本 APK 下载到应用私有缓存目录，校验包名、版本号、签名后再交给 Android 系统安装器。
 * App 不执行静默安装。
 */
data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val title: String,
    val changelog: List<String>,
    val downloadUrl: String,
    val publishedAt: String,
    val forceUpdate: Boolean,
    val sha256: String? = null
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

sealed interface UpdateDownloadResult {
    data class Success(val file: File) : UpdateDownloadResult
    data class Failed(val message: String) : UpdateDownloadResult
}

sealed interface UpdateInstallResult {
    data object Started : UpdateInstallResult
    data object PermissionRequired : UpdateInstallResult
    data class Failed(val message: String) : UpdateInstallResult
}

object AppUpdateManager {

    const val UPDATE_MANIFEST_URL =
        "https://raw.githubusercontent.com/chyyy1006-rgb/guaguacourse/main/update/latest.json"

    const val REPOSITORY_URL =
        "https://github.com/chyyy1006-rgb/guaguacourse"

    const val RELEASES_URL =
        "https://github.com/chyyy1006-rgb/guaguacourse/releases"

    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val PREFS = "guagua_update_center"
    private const val KEY_LAST_CHECK_AT = "last_check_at"
    private const val KEY_LAST_PROMPTED_VERSION = "last_prompted_version"
    private const val KEY_LAST_PROMPTED_AT = "last_prompted_at"

    private const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    private const val RE_PROMPT_INTERVAL_MS = 24L * 60L * 60L * 1000L

    fun currentVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageVersionCode(info)
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

    /**
     * 直接下载正式 APK 到 cache/shared/updates。
     * 下载结束后会校验 APK 包名、versionCode、当前安装签名；latest.json 提供 sha256 时额外校验哈希。
     */
    suspend fun downloadApk(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (Int?) -> Unit = {}
    ): UpdateDownloadResult = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "shared/updates").apply { mkdirs() }
        val target = File(updateDir, "GuaguaCourse-${safeFilePart(info.versionName)}.apk")
        val partial = File(updateDir, "${target.name}.part")

        if (target.isFile) {
            val cachedValidation = validateApk(context, target, info)
            if (cachedValidation == null) {
                withContext(Dispatchers.Main) { onProgress(100) }
                return@withContext UpdateDownloadResult.Success(target)
            }
            target.delete()
        }

        partial.delete()
        updateDir.listFiles()
            ?.filter { it != target && it != partial && (it.name.endsWith(".apk") || it.name.endsWith(".part")) }
            ?.forEach { it.delete() }

        try {
            val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 30_000
                useCaches = false
                instanceFollowRedirects = true
                setRequestProperty("Accept", APK_MIME)
                setRequestProperty("User-Agent", "GuaguaCourse-Android/${currentVersionName(context)}")
            }

            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    return@withContext UpdateDownloadResult.Failed("下载更新失败：HTTP $status")
                }

                val contentType = connection.contentType.orEmpty().lowercase()
                if (contentType.contains("text/html")) {
                    return@withContext UpdateDownloadResult.Failed("更新地址不是 APK 文件，请检查 GitHub Release 附件")
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                var downloadedBytes = 0L
                var lastProgress = -1
                val digest = MessageDigest.getInstance("SHA-256")

                connection.inputStream.use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloadedBytes += count

                            val progress = totalBytes?.let {
                                ((downloadedBytes * 100L) / it).toInt().coerceIn(0, 100)
                            }
                            if (progress != lastProgress && (progress == null || progress == 100 || progress % 2 == 0)) {
                                lastProgress = progress ?: -1
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                }

                if (!partial.isFile || partial.length() <= 0L) {
                    partial.delete()
                    return@withContext UpdateDownloadResult.Failed("更新包下载为空，请稍后重试")
                }

                info.sha256?.let { expected ->
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actual.equals(expected, ignoreCase = true)) {
                        partial.delete()
                        return@withContext UpdateDownloadResult.Failed("更新包校验失败，请重新下载")
                    }
                }

                if (!partial.renameTo(target)) {
                    partial.copyTo(target, overwrite = true)
                    partial.delete()
                }

                val validationError = validateApk(context, target, info)
                if (validationError != null) {
                    target.delete()
                    return@withContext UpdateDownloadResult.Failed(validationError)
                }

                withContext(Dispatchers.Main) { onProgress(100) }
                UpdateDownloadResult.Success(target)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            partial.delete()
            UpdateDownloadResult.Failed(
                if (e.message.isNullOrBlank()) "下载更新失败，请稍后重试"
                else "下载更新失败：${e.message}"
            )
        }
    }

    /**
     * 把已经校验过的 APK 交给 Android 系统安装器。
     * Android 8.0+ 首次使用时可能需要用户在系统设置中允许“安装未知应用”。
     */
    fun installDownloadedApk(context: Context, file: File): UpdateInstallResult {
        if (!file.isFile) {
            return UpdateInstallResult.Failed("更新包不存在，请重新下载")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return UpdateInstallResult.PermissionRequired
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME)
                clipData = ClipData.newRawUri("GuaguaCourse update", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            UpdateInstallResult.Started
        } catch (e: Exception) {
            UpdateInstallResult.Failed(
                if (e.message.isNullOrBlank()) "无法打开系统安装界面"
                else "无法打开系统安装界面：${e.message}"
            )
        }
    }

    fun unknownSourcesSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )

    private fun parseUpdateInfo(json: JSONObject): AppUpdateInfo? {
        val versionCode = json.optLong("versionCode", -1L)
        val versionName = json.optString("versionName", "").trim()
        val downloadUrl = json.optString("downloadUrl", "").trim()
        if (versionCode <= 0L || versionName.isBlank() || !downloadUrl.startsWith("https://")) {
            return null
        }

        val sha256 = json.optString("sha256", "")
            .trim()
            .lowercase()
            .takeIf { it.matches(Regex("[0-9a-f]{64}")) }

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
            forceUpdate = json.optBoolean("forceUpdate", false),
            sha256 = sha256
        )
    }

    private fun validateApk(context: Context, file: File, info: AppUpdateInfo): String? {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        @Suppress("DEPRECATION")
        val archiveInfo = packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: return "下载的文件不是有效的 APK"

        if (archiveInfo.packageName != context.packageName) {
            return "更新包与当前应用不匹配"
        }

        if (packageVersionCode(archiveInfo) != info.versionCode) {
            return "更新包版本与更新信息不一致"
        }

        val installedInfo = try {
            packageManager.getPackageInfo(context.packageName, flags)
        } catch (_: Exception) {
            null
        }

        if (installedInfo != null) {
            val installedSignatures = signingDigests(installedInfo)
            val archiveSignatures = signingDigests(archiveInfo)
            if (installedSignatures.isNotEmpty() &&
                archiveSignatures.isNotEmpty() &&
                installedSignatures.intersect(archiveSignatures).isEmpty()
            ) {
                return "更新包签名与当前安装版本不一致"
            }
        }

        return null
    }

    private fun signingDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.toList()
            } else {
                signingInfo.signingCertificateHistory.toList()
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList().orEmpty()
        }

        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun packageVersionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

    private fun safeFilePart(value: String): String =
        value.replace(Regex("[^0-9A-Za-z._-]"), "_")

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
