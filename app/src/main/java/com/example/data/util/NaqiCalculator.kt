package com.example.data.util

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * National Air Quality Index (NAQI) calculator for Indian locations.
 * Follows the official Indian Central Pollution Control Board (CPCB) standards and breakpoints:
 *
 * Categories & Breakpoints:
 * 1. Good: 0 - 50 (PM2.5: 0-30, PM10: 0-50)
 * 2. Satisfactory: 51 - 100 (PM2.5: 31-60, PM10: 51-100)
 * 3. Moderate: 101 - 200 (PM2.5: 61-90, PM10: 101-250)
 * 4. Poor: 201 - 300 (PM2.5: 91-120, PM10: 251-350)
 * 5. Very Poor: 301 - 400 (PM2.5: 121-250, PM10: 351-430)
 * 6. Severe: 401 - 500 (PM2.5: 250+, PM10: 430+)
 *
 * Sub-index Formula (Linear Interpolation):
 * I = I_low + [ (I_high - I_low) / (B_high - B_low) ] * (C - B_low)
 * Overall NAQI = max(sub-index_PM2.5, sub-index_PM10)
 */
object NaqiCalculator {

    const val STANDARD_NAME = "National AQI (NAQI)"

    /**
     * Calculates PM2.5 sub-index according to CPCB guidelines.
     */
    fun calculatePm25SubIndex(pm25: Double): Int {
        val c = max(0.0, pm25)
        val index = when {
            c <= 30.0 -> (c * 50.0 / 30.0)
            c <= 60.0 -> 50.0 + ((c - 30.0) * 50.0 / 30.0)
            c <= 90.0 -> 100.0 + ((c - 60.0) * 100.0 / 30.0)
            c <= 120.0 -> 200.0 + ((c - 90.0) * 100.0 / 30.0)
            c <= 250.0 -> 300.0 + ((c - 120.0) * 100.0 / 130.0)
            else -> {
                val severe = 400.0 + ((c - 250.0) * 100.0 / 130.0)
                severe.coerceAtMost(500.0)
            }
        }
        return index.roundToInt().coerceIn(0, 500)
    }

    /**
     * Calculates PM10 sub-index according to CPCB guidelines.
     */
    fun calculatePm10SubIndex(pm10: Double): Int {
        val c = max(0.0, pm10)
        val index = when {
            c <= 50.0 -> c
            c <= 100.0 -> c
            c <= 250.0 -> 100.0 + ((c - 100.0) * 100.0 / 150.0)
            c <= 350.0 -> 200.0 + ((c - 250.0) * 100.0 / 100.0)
            c <= 430.0 -> 300.0 + ((c - 350.0) * 100.0 / 80.0)
            else -> {
                val severe = 400.0 + ((c - 430.0) * 100.0 / 70.0)
                severe.coerceAtMost(500.0)
            }
        }
        return index.roundToInt().coerceIn(0, 500)
    }

    /**
     * Computes the overall NAQI from available pollutant concentrations,
     * falling back to US AQI conversion if particulate data is unavailable.
     */
    fun calculateNaqi(pm25: Double?, pm10: Double?, fallbackUsAqi: Int? = null): Int {
        val pm25Index = pm25?.let { calculatePm25SubIndex(it) }
        val pm10Index = pm10?.let { calculatePm10SubIndex(it) }

        return when {
            pm25Index != null && pm10Index != null -> max(pm25Index, pm10Index)
            pm25Index != null -> pm25Index
            pm10Index != null -> pm10Index
            fallbackUsAqi != null -> convertUsAqiToNaqi(fallbackUsAqi)
            else -> 75 // Neutral default
        }
    }

    /**
     * Returns the official CPCB descriptive level label for an NAQI value.
     */
    fun getCategoryLabel(naqi: Int): String {
        return when {
            naqi <= 50 -> "Good"
            naqi <= 100 -> "Satisfactory"
            naqi <= 200 -> "Moderate"
            naqi <= 300 -> "Poor"
            naqi <= 400 -> "Very Poor"
            else -> "Severe"
        }
    }

    /**
     * Converts US AQI to Indian NAQI equivalent based on the corresponding PM concentration curves.
     */
    fun convertUsAqiToNaqi(usAqi: Int): Int {
        return when {
            usAqi <= 50 -> usAqi
            usAqi <= 100 -> usAqi
            usAqi <= 150 -> 100 + ((usAqi - 100) * 1.6).toInt()
            usAqi <= 200 -> 180 + ((usAqi - 150) * 2.0).toInt()
            usAqi <= 300 -> 280 + ((usAqi - 200) * 1.0).toInt()
            else -> (380 + (usAqi - 300)).coerceAtMost(500)
        }
    }
}
