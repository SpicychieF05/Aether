package com.example

import com.example.data.alert.AlertSeverity
import com.example.data.alert.WeatherAlertEngine
import com.example.data.model.CurrentWeather
import com.example.data.model.HourlyItem
import com.example.data.model.LocationItem
import com.example.data.model.TerrainCategory
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherEffectType
import com.example.data.util.WeatherIconMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAlertEngineTest {

    private val testLocation = LocationItem(
        id = 1275004L,
        name = "Bolpur",
        admin1 = "West Bengal",
        country = "India",
        latitude = 23.67,
        longitude = 87.72,
        terrainCategory = TerrainCategory.HILLS_PLAINS,
        isGps = true
    )

    private val baseCurrent = CurrentWeather(
        tempC = 28.0,
        feelsLikeC = 29.0,
        humidity = 60,
        condition = WeatherCondition(0, "Clear Sky", WeatherEffectType.CLEAR, isDay = true),
        isDay = true,
        windSpeedKmh = 12.0,
        visibilityMeters = 10000.0,
        precipitationMm = 0.0
    )

    @Test
    fun testRainDevelopingAlert() {
        val hourly = listOf(
            HourlyItem("12 PM", "2026-09-25T12:00", 12, 28.0, WeatherCondition(0, "Clear", WeatherEffectType.CLEAR, true), 0, true),
            HourlyItem("1 PM", "2026-09-25T13:00", 13, 27.5, WeatherCondition(2, "Partly Cloudy", WeatherEffectType.CLOUDY, true), 10, true),
            HourlyItem("2 PM", "2026-09-25T14:00", 14, 25.0, WeatherCondition(61, "Slight Rain", WeatherEffectType.RAIN, true, rainIntensity = 0.4f), 70, true),
            HourlyItem("3 PM", "2026-09-25T15:00", 15, 24.0, WeatherCondition(63, "Moderate Rain", WeatherEffectType.RAIN, true, rainIntensity = 0.6f), 80, true)
        )

        val alert = WeatherAlertEngine.analyzeForecast(testLocation, baseCurrent, hourly)
        assertNotNull(alert)
        assertEquals("Rain expected", alert!!.title)
        assertTrue(alert.body.contains("Bolpur"))
        assertTrue(alert.body.contains("2 PM"))
        assertEquals("See full forecast for Bolpur", alert.ctaText)
        assertEquals(14, alert.focusedHourOfDay)
    }

    @Test
    fun testContinuousRainAlert() {
        val rainCondition = WeatherCondition(63, "Moderate Rain", WeatherEffectType.RAIN, true, rainIntensity = 0.6f)
        val hourly = listOf(
            HourlyItem("2 PM", "2026-09-25T14:00", 14, 24.0, rainCondition, 85, true),
            HourlyItem("3 PM", "2026-09-25T15:00", 15, 23.5, rainCondition, 90, true),
            HourlyItem("4 PM", "2026-09-25T16:00", 16, 23.0, rainCondition, 85, true),
            HourlyItem("5 PM", "2026-09-25T17:00", 17, 22.5, rainCondition, 80, true)
        )

        val alert = WeatherAlertEngine.analyzeForecast(testLocation, baseCurrent, hourly)
        assertNotNull(alert)
        assertTrue(alert!!.title.contains("Rain expected for the next 4 hours"))
        assertTrue(alert.body.contains("Bolpur"))
        assertEquals(4, alert.durationHours)
    }

    @Test
    fun testRainAndThunderstormConsolidatedAlert() {
        val rainCond = WeatherCondition(63, "Moderate Rain", WeatherEffectType.RAIN, true, rainIntensity = 0.6f)
        val stormCond = WeatherCondition(95, "Thunderstorm", WeatherEffectType.THUNDERSTORM, true, rainIntensity = 0.85f, isThunder = true)

        val hourly = listOf(
            HourlyItem("1 PM", "2026-09-25T13:00", 13, 27.0, WeatherCondition(2, "Cloudy", WeatherEffectType.CLOUDY, true), 10, true),
            HourlyItem("2 PM", "2026-09-25T14:00", 14, 25.0, rainCond, 75, true),
            HourlyItem("3 PM", "2026-09-25T15:00", 15, 23.0, stormCond, 90, true),
            HourlyItem("4 PM", "2026-09-25T16:00", 16, 22.5, stormCond, 95, true),
            HourlyItem("5 PM", "2026-09-25T17:00", 17, 22.0, rainCond, 70, true)
        )

        val alert = WeatherAlertEngine.analyzeForecast(testLocation, baseCurrent, hourly)
        assertNotNull(alert)
        // Consolidates into single rain + thunderstorm alert
        assertTrue(alert!!.title.contains("thunderstorms", ignoreCase = true))
        assertTrue(alert.body.contains("Bolpur"))
        assertEquals(AlertSeverity.ELEVATED, alert.severity)
        assertEquals(15, alert.focusedHourOfDay)
    }

    @Test
    fun testClearWeatherNotification() {
        val clearCond = WeatherCondition(0, "Clear Sky", WeatherEffectType.CLEAR, true)
        val hourly = (8..15).map { hour ->
            HourlyItem("$hour AM", "2026-09-25T0$hour:00", hour, 26.0 + (hour - 8), clearCond, 0, true)
        }

        val alert = WeatherAlertEngine.analyzeForecast(testLocation, baseCurrent, hourly)
        assertNotNull(alert)
        assertEquals("Bright, sunny day ahead", alert!!.title)
        assertTrue(alert.body.contains("Bolpur"))
        assertTrue(alert.body.contains("Clear skies expected in Bolpur for the next 6 hours"))
    }

    @Test
    fun testOfficialWarningNotification() {
        val alert = WeatherAlertEngine.analyzeForecast(
            location = testLocation,
            current = baseCurrent,
            hourly = emptyList(),
            hasOfficialWarning = true,
            officialWarningTitle = "Severe Cyclone Warning",
            officialWarningDetails = "Very heavy precipitation and winds exceeding 90 km/h"
        )
        assertNotNull(alert)
        assertTrue(alert!!.title.contains("⚠ Severe Cyclone Warning"))
        assertEquals(AlertSeverity.OFFICIAL_WARNING, alert.severity)
        assertTrue(alert.isOfficialWarning)
        assertTrue(alert.body.contains("winds exceeding 90 km/h"))
    }

    @Test
    fun testWeatherIconMapper() {
        val clearDay = WeatherCondition(0, "Clear", WeatherEffectType.CLEAR, isDay = true)
        val clearNight = WeatherCondition(0, "Clear Night", WeatherEffectType.CLEAR, isDay = false)
        val rain = WeatherCondition(63, "Rain", WeatherEffectType.RAIN, isDay = true, rainIntensity = 0.5f)
        val heavyRain = WeatherCondition(65, "Heavy Rain", WeatherEffectType.RAIN, isDay = true, rainIntensity = 0.9f)
        val storm = WeatherCondition(95, "Thunderstorm", WeatherEffectType.THUNDERSTORM, isDay = true, isThunder = true)

        assertEquals(R.drawable.ic_weather_clear_day, WeatherIconMapper.getIconDrawableRes(clearDay, true))
        assertEquals(R.drawable.ic_weather_clear_night, WeatherIconMapper.getIconDrawableRes(clearNight, false))
        assertEquals(R.drawable.ic_weather_rainy_2, WeatherIconMapper.getIconDrawableRes(rain, true))
        assertEquals(R.drawable.ic_weather_rainy_3, WeatherIconMapper.getIconDrawableRes(heavyRain, true))
        assertEquals(R.drawable.ic_weather_thunderstorms, WeatherIconMapper.getIconDrawableRes(storm, true))
        assertEquals(R.drawable.ic_weather_warning, WeatherIconMapper.getIconDrawableRes(storm, true, isOfficialWarning = true))
    }
}
