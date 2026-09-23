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
     * Formats current time according to location coordinates and country.
     * Uses timezone lookup based on longitude or known regions (e.g. India = IST GMT+5:30).
     */
    fun formatLocationTime(
        latitude: Double,
        longitude: Double,
        country: String?,
        admin1: String? = null
    ): String {
        val tz: java.util.TimeZone = resolveTimeZone(latitude, longitude, country, admin1)
        val sdf = java.text.SimpleDateFormat("h:mm a", Locale.US)
        sdf.timeZone = tz
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
