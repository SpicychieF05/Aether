package com.example.data.alert

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AetherDatabase
import com.example.data.repository.LocationHelper
import com.example.data.repository.WeatherProvider
import com.example.data.repository.WeatherRepository
import java.util.concurrent.TimeUnit

class WeatherAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WeatherAlertWorker"
        const val UNIQUE_PERIODIC_WORK_NAME = "aether_adaptive_weather_alert_work"
        const val UNIQUE_ONETIME_WORK_NAME = "aether_adaptive_onetime_weather_alert"

        /**
         * Schedules background forecast monitoring using WorkManager.
         */
        fun scheduleAdaptiveWork(context: Context, delayMinutes: Long? = null) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            if (delayMinutes != null) {
                // Adaptive one-time check timed closely to an upcoming weather transition
                val oneTimeRequest = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .build()

                workManager.enqueueUniqueWork(
                    UNIQUE_ONETIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
            } else {
                // Standard periodic check (minimum 2 hours for battery efficiency)
                val periodicRequest = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
                    2, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRequest
                )
            }
        }
    }

    override suspend fun doWork(): Result {
        try {
            if (WeatherNotificationManager.isAlertsPaused(context)) {
                return Result.success()
            }

            // 1. Get current GPS Location (or fallback to default location)
            val locationHelper = LocationHelper(context)
            val location = locationHelper.getCurrentLocation() ?: LocationHelper.DEFAULT_LOCATION

            // 2. Read configured Weather Provider
            val prefs = context.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
            val providerName = prefs.getString("pref_weather_provider", WeatherProvider.OPEN_METEO.name)
            val provider = try {
                WeatherProvider.valueOf(providerName ?: WeatherProvider.OPEN_METEO.name)
            } catch (_: Exception) {
                WeatherProvider.OPEN_METEO
            }
            val isFahrenheit = prefs.getBoolean("pref_is_fahrenheit", false)

            // 3. Fetch fresh weather forecast
            val db = AetherDatabase.getDatabase(context)
            val repository = WeatherRepository(db)
            val weatherResult = repository.fetchWeather(location, preferredProvider = provider, forceRefresh = true)

            weatherResult.fold(
                onSuccess = { weatherState ->
                    // 4. Run Weather Alert Engine
                    val alertEvent = WeatherAlertEngine.analyzeForecast(
                        location = weatherState.location,
                        current = weatherState.current,
                        hourly = weatherState.hourly,
                        isFahrenheit = isFahrenheit
                    )

                    if (alertEvent != null) {
                        if (WeatherNotificationManager.shouldNotify(context, alertEvent)) {
                            WeatherNotificationManager.postAlertNotification(context, alertEvent, weatherState.location)
                        }

                        // Adaptive timing: If severe storm or rain is approaching within 1-2 hours,
                        // schedule an adaptive follow-up check sooner
                        if (alertEvent.severity == AlertSeverity.ELEVATED) {
                            scheduleAdaptiveWork(context, delayMinutes = 90)
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Background weather fetch failed: ${error.localizedMessage}")
                }
            )

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in WeatherAlertWorker", e)
            return Result.retry()
        }
    }
}
