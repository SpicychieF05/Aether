package com.example.data.alert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.LocationItem
import com.example.data.util.WeatherIconMapper

object WeatherNotificationManager {

    private const val PREFS_NAME = "aether_alert_prefs"
    private const val KEY_LAST_EVENT_ID = "last_event_id"
    private const val KEY_LAST_HASH = "last_material_hash"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    private const val KEY_ALERTS_PAUSED = "pref_weather_alerts_paused"

    const val CHANNEL_FORECAST_ALERTS = "aether_forecast_channel"
    const val CHANNEL_WEATHER_WARNINGS = "aether_warning_channel"

    const val NOTIFICATION_ID_FORECAST = 2001
    const val NOTIFICATION_ID_WARNING = 2002

    private const val COOLDOWN_MS = 90 * 60 * 1000L // 90 minutes cooldown for identical / similar general alerts

    fun isAlertsPaused(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ALERTS_PAUSED, false)
    }

    fun setAlertsPaused(context: Context, paused: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALERTS_PAUSED, paused).apply()
        if (paused) {
            cancelAllAlertNotifications(context)
        }
    }

    fun cancelAllAlertNotifications(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_FORECAST)
        nm.cancel(NOTIFICATION_ID_WARNING)
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Forecast Alerts Channel (Default importance, non-intrusive)
            val forecastChannel = NotificationChannel(
                CHANNEL_FORECAST_ALERTS,
                "Aether Forecast Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Proactive upcoming weather forecast notifications"
                enableLights(true)
                lightColor = Color.CYAN
            }

            // Official Weather Warnings Channel (High importance, vibration)
            val warningChannel = NotificationChannel(
                CHANNEL_WEATHER_WARNINGS,
                "Aether Weather Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Official government and meteorological severe weather alerts"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            nm.createNotificationChannel(forecastChannel)
            nm.createNotificationChannel(warningChannel)
        }
    }

    /**
     * Determines whether this event is materially new or whether cooldown/deduplication applies.
     */
    fun shouldNotify(context: Context, event: WeatherAlertEvent): Boolean {
        if (isAlertsPaused(context)) return false
        if (!isNotificationPermissionGranted(context)) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastEventId = prefs.getString(KEY_LAST_EVENT_ID, null)
        val lastHash = prefs.getString(KEY_LAST_HASH, null)
        val lastTimestamp = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()

        // Official warnings bypass cooldown
        if (event.isOfficialWarning) {
            return lastHash != event.materialHash
        }

        // Exact match with previous notified event & hash -> NO duplicate notification
        if (lastEventId == event.eventId && lastHash == event.materialHash) {
            return false
        }

        // If it's a completely different event or material hash changed, allow update
        if (lastHash != event.materialHash) {
            return true
        }

        // Otherwise check cooldown
        return (now - lastTimestamp) >= COOLDOWN_MS
    }

    /**
     * Posts or updates the Android system notification following the Google Weather style.
     */
    fun postAlertNotification(
        context: Context,
        event: WeatherAlertEvent,
        location: LocationItem
    ) {
        if (isAlertsPaused(context)) return
        if (!isNotificationPermissionGranted(context)) return

        createNotificationChannels(context)

        val channelId = if (event.isOfficialWarning) CHANNEL_WEATHER_WARNINGS else CHANNEL_FORECAST_ALERTS
        val notificationId = if (event.isOfficialWarning) NOTIFICATION_ID_WARNING else NOTIFICATION_ID_FORECAST

        // Context-aware deep-link intent to focus the specific forecast hour
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_FOCUS_HOUR", event.focusedHourOfDay)
            putExtra("EXTRA_ALERT_LAT", location.latitude)
            putExtra("EXTRA_ALERT_LON", location.longitude)
            putExtra("EXTRA_ALERT_LOC_NAME", location.name)
            putExtra("EXTRA_FROM_WEATHER_ALERT", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = WeatherIconMapper.getLargeIconBitmap(
            context = context,
            condition = event.condition,
            isDay = true,
            isOfficialWarning = event.isOfficialWarning,
            sizePx = 144
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(event.title)
            .setContentText(event.body)
            .setSubText("Aether")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (event.isOfficialWarning) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        // Expandable BigTextStyle showing the complete forecast summary and CTA
        val bigStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(event.title)
            .bigText("${event.body}\n\n${event.ctaText}")

        if (event.isOfficialWarning) {
            bigStyle.setSummaryText("Official Warning")
            builder.setColor(Color.RED)
            builder.setColorized(true)
        } else {
            bigStyle.setSummaryText(location.name)
            builder.setColor(0xFF64B5F6.toInt())
        }

        builder.setStyle(bigStyle)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, builder.build())

        // Save last notified alert state for deduplication
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_EVENT_ID, event.eventId)
            .putString(KEY_LAST_HASH, event.materialHash)
            .putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
}
