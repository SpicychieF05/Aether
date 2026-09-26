package com.example.data.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val htmlUrl: String
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    const val GITHUB_OWNER = "SpicychieF05"
    const val GITHUB_REPO = "Aether"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private const val NOTIFICATION_CHANNEL_ID = "aether_update_channel"
    private const val NOTIFICATION_ID = 1002

    private var activeDownloadId: Long = -1L
    private var isReceiverRegistered = false

    /**
     * Extracts pure numeric semantic version (e.g. "1.2.1" from "v1.2.1" or "Release v1.2.1-beta")
     */
    fun extractSemVer(raw: String): String {
        if (raw.isBlank()) return ""
        val match = Regex("""\d+(\.\d+)+""").find(raw)
        return match?.value ?: raw.filter { it.isDigit() || it == '.' }
    }

    /**
     * Queries the GitHub Releases API and compares the latest release tag with the installed version.
     */
    suspend fun checkLatestRelease(currentVersionName: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Aether-Android-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("GitHub API error: HTTP ${conn.responseCode}"))
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")

            var apkUrl = ""
            var apkName = "Aether-latest.apk"

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        break
                    }
                }
            }

            val remoteSemVer = extractSemVer(tagName)
            val currentSemVer = extractSemVer(currentVersionName)

            // Strict check: Only prompt if remote version is strictly newer than current version
            val hasUpdate = isNewerVersion(remoteSemVer, currentSemVer) && apkUrl.isNotEmpty()

            Result.success(
                AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = tagName,
                    currentVersion = currentVersionName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    apkFileName = apkName,
                    htmlUrl = htmlUrl
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check latest release", e)
            Result.failure(e)
        }
    }

    /**
     * Semantic version comparison (e.g., 1.2.1 > 1.2.0 -> true; 1.2.1 == 1.2.1 -> false)
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        if (current.isBlank()) return true
        if (remote == current) return false

        val remoteParts = remote.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Posts a system notification to inform the user that a new version is available.
     * Clicking the notification opens the Settings screen in the app.
     */
    fun postUpdateNotification(context: Context, updateInfo: AppUpdateInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Aether Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when a new version of Aether is released"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_SETTINGS_UPDATE", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("New Version Available: ${updateInfo.latestVersion}")
            .setContentText("Tap to open Settings and install the update.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Version ${updateInfo.latestVersion} is available. Open Settings in Aether to review and install."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Checks if a previously downloaded valid APK already exists in the app cache/downloads folder.
     */
    fun getExistingDownloadedApkUri(context: Context, fileName: String): Uri? {
        return try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists() && file.length() > 500_000) { // Valid APK size
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get existing APK Uri", e)
            null
        }
    }

    /**
     * Starts the APK download via Android's DownloadManager and registers receiver for completion.
     */
    fun startDownload(
        context: Context,
        apkUrl: String,
        fileName: String,
        onDownloadStarted: (Long) -> Unit,
        onInstallReady: (Uri) -> Unit
    ) {
        try {
            // Delete any existing older downloaded file with the same name to avoid DownloadManager renaming
            val existingFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (existingFile.exists()) {
                existingFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete old download file", e)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Downloading Aether Update")
            setDescription(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadId = downloadManager.enqueue(request)
        activeDownloadId = downloadId
        onDownloadStarted(downloadId)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId) {
                    try {
                        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                        if (file.exists()) {
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            onInstallReady(contentUri)
                            promptInstall(context, contentUri)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling completed download", e)
                    } finally {
                        try {
                            context.unregisterReceiver(this)
                            isReceiverRegistered = false
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        isReceiverRegistered = true
    }

    /**
     * Launches the Android Package Installer using the secure FileProvider content Uri.
     * On Android 8.0+ (Oreo+), verifies permission to install unknown apps first.
     */
    fun promptInstall(context: Context, contentUri: Uri) {
        try {
            // Check for Unknown App Sources permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return
                }
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        }
    }

    /**
     * Opens the direct APK or GitHub Release page in browser as a 100% reliable fallback.
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
        }
    }
}
