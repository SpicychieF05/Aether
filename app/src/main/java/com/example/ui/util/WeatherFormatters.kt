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
}
