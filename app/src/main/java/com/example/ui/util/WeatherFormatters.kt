package com.example.ui.util

import java.util.Locale
import kotlin.math.roundToInt

object WeatherFormatters {

    fun formatTemp(tempC: Double, isFahrenheit: Boolean): String {
        return if (isFahrenheit) {
            val f = (tempC * 9.0 / 5.0) + 32.0
            "${f.roundToInt()}°"
        } else {
            "${tempC.roundToInt()}°"
        }
    }

    fun formatTempWithUnit(tempC: Double, isFahrenheit: Boolean): String {
        return if (isFahrenheit) {
            val f = (tempC * 9.0 / 5.0) + 32.0
            "${f.roundToInt()}°F"
        } else {
            "${tempC.roundToInt()}°C"
        }
    }

    fun formatSpeed(speedKmh: Double, isFahrenheit: Boolean): String {
        return if (isFahrenheit) {
            val mph = speedKmh * 0.621371
            "${mph.roundToInt()} mph"
        } else {
            "${speedKmh.roundToInt()} km/h"
        }
    }

    fun formatVisibility(meters: Double?, isFahrenheit: Boolean): String {
        if (meters == null) return "N/A"
        return if (isFahrenheit) {
            val miles = (meters / 1000.0) * 0.621371
            String.format(Locale.US, "%.1f mi", miles)
        } else {
            val km = meters / 1000.0
            String.format(Locale.US, "%.1f km", km)
        }
    }

    /**
     * Formats current time according to location coordinates, country and device timezone.
     * If the location is in the same timezone as the user's device, or if offline,
     * it directly leverages the device clock and timezone.
     */
    fun formatLocationTime(
        latitude: Double,
        longitude: Double,
        country: String?,
        admin1: String? = null
    ): String {
        val deviceTz = java.util.TimeZone.getDefault()
        val locTz: java.util.TimeZone = resolveTimeZone(latitude, longitude, country, admin1)

        // Compare raw offsets: if they are in the same offset bucket, use device timezone directly
        val targetTz = if (Math.abs(deviceTz.rawOffset - locTz.rawOffset) < 1800000) { // within 30 min
            deviceTz
        } else {
            locTz
        }

        val sdf = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.timeZone = targetTz
        return sdf.format(java.util.Date())
    }

    private fun resolveTimeZone(
        latitude: Double,
        longitude: Double,
        country: String?,
        admin1: String?
    ): java.util.TimeZone {
        val c = country?.lowercase()?.trim() ?: ""
        // Check India
        if (c == "india" || c == "in" || c.contains("india") || (latitude in 6.0..37.6 && longitude in 68.0..97.6)) {
            return java.util.TimeZone.getTimeZone("Asia/Kolkata")
        }
        // Approximate time offset by longitude: longitude / 15.0 hours offset
        val rawOffsetHours = (longitude / 15.0).roundToInt()
        val customTzId = if (rawOffsetHours >= 0) "GMT+$rawOffsetHours" else "GMT$rawOffsetHours"
        return java.util.TimeZone.getTimeZone(customTzId)
    }
}
