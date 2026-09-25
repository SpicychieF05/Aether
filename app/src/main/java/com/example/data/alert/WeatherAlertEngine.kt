package com.example.data.alert

import com.example.data.model.CurrentWeather
import com.example.data.model.HourlyItem
import com.example.data.model.LocationItem
import com.example.data.model.WeatherEffectType
import com.example.data.util.WeatherIconMapper
import kotlin.math.roundToInt

object WeatherAlertEngine {

    /**
     * Analyzes the upcoming forecast window (next 6-12 hours) and intelligently detects
     * whether there is a meaningful event worth notifying the user about.
     */
    fun analyzeForecast(
        location: LocationItem,
        current: CurrentWeather,
        hourly: List<HourlyItem>,
        isFahrenheit: Boolean = false,
        hasOfficialWarning: Boolean = false,
        officialWarningTitle: String? = null,
        officialWarningDetails: String? = null
    ): WeatherAlertEvent? {
        val locationName = location.name.ifBlank { "your area" }

        // 1. Check for Genuine Official Government / Meteorological Warnings
        if (hasOfficialWarning && !officialWarningTitle.isNullOrBlank()) {
            val title = "⚠ $officialWarningTitle"
            val body = officialWarningDetails?.ifBlank { "Official warning issued for $locationName" }
                ?: "Official warning issued for $locationName"
            val hash = "warning_${officialWarningTitle.hashCode()}_${body.hashCode()}"
            return WeatherAlertEvent(
                eventId = "official_warning_${location.id}_${title.hashCode()}",
                title = title,
                body = body,
                ctaText = "Tap to view details for $locationName",
                condition = current.condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(current.condition, current.isDay, isOfficialWarning = true),
                focusedHourOfDay = null,
                startTimeLabel = "Now",
                durationHours = 6,
                severity = AlertSeverity.OFFICIAL_WARNING,
                isOfficialWarning = true,
                materialHash = hash
            )
        }

        if (hourly.isEmpty()) return null

        // Consider the upcoming window of 8 to 12 hours
        val forecastWindow = hourly.take(10)
        val rainHours = forecastWindow.filter { it.condition.effectType == WeatherEffectType.RAIN }
        val stormHours = forecastWindow.filter { it.condition.effectType == WeatherEffectType.THUNDERSTORM }
        val fogHours = forecastWindow.filter { it.condition.effectType == WeatherEffectType.FOG }
        val snowHours = forecastWindow.filter { it.condition.effectType == WeatherEffectType.SNOW }

        // 2. Intelligent Weather Combination: Rain + Thunderstorm (Section 9 & 31)
        if (stormHours.isNotEmpty()) {
            val firstStorm = stormHours.first()
            val totalStormyCount = stormHours.size + rainHours.size
            val duration = totalStormyCount.coerceIn(1, 6)
            val hasHeavy = stormHours.any { it.condition.rainIntensity >= 0.7f } || rainHours.any { it.condition.rainIntensity >= 0.8f }
            val firstStormIndex = forecastWindow.indexOf(firstStorm)

            val title = when {
                hasHeavy && rainHours.isNotEmpty() -> "Heavy rain with thunderstorms"
                stormHours.size >= 2 -> "Rain with thunderstorms"
                rainHours.isNotEmpty() -> "Rain with thunderstorms"
                else -> "Thunderstorm expected"
            }

            val body = if (firstStormIndex <= 1) {
                if (duration > 1) {
                    "Expected in $locationName over the next $duration hours"
                } else {
                    "Expected in $locationName around ${firstStorm.timeLabel}"
                }
            } else {
                "Expected in $locationName around ${firstStorm.timeLabel}"
            }

            val hash = "storm_${firstStorm.hourOfDay}_${duration}_${hasHeavy}_${title.hashCode()}"
            return WeatherAlertEvent(
                eventId = "storm_event_${location.name}_${firstStorm.hourOfDay}",
                title = title,
                body = body,
                ctaText = "See full forecast for $locationName",
                condition = firstStorm.condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(firstStorm.condition, firstStorm.isDay),
                focusedHourOfDay = firstStorm.hourOfDay,
                startTimeLabel = firstStorm.timeLabel,
                durationHours = duration,
                severity = AlertSeverity.ELEVATED,
                isOfficialWarning = false,
                materialHash = hash
            )
        }

        // 3. Rain Developing or Continuous Rain
        if (rainHours.isNotEmpty()) {
            val firstRain = rainHours.first()
            val firstRainIndex = forecastWindow.indexOf(firstRain)
            val consecutiveRainCount = calculateConsecutiveHours(forecastWindow, firstRainIndex) {
                it.condition.effectType == WeatherEffectType.RAIN
            }
            val maxRainIntensity = rainHours.maxOf { it.condition.rainIntensity }
            val isHeavy = maxRainIntensity >= 0.75f

            val title = when {
                consecutiveRainCount >= 3 -> {
                    if (isHeavy) "Heavy rain expected for the next $consecutiveRainCount hours"
                    else "Rain expected for the next $consecutiveRainCount hours"
                }
                isHeavy -> "Heavy rain expected"
                else -> "Rain expected"
            }

            val body = when {
                firstRainIndex == 0 && consecutiveRainCount >= 2 -> {
                    "Continuous rain is expected in $locationName for the next $consecutiveRainCount hours"
                }
                consecutiveRainCount >= 3 -> {
                    "Continuous rain is expected in $locationName from around ${firstRain.timeLabel}"
                }
                firstRainIndex <= 2 -> {
                    "Rain is expected in $locationName around ${firstRain.timeLabel}"
                }
                else -> {
                    "Rain is expected from around ${firstRain.timeLabel} in $locationName"
                }
            }

            val hash = "rain_${firstRain.hourOfDay}_${consecutiveRainCount}_${isHeavy}_${title.hashCode()}"
            return WeatherAlertEvent(
                eventId = "rain_event_${location.name}_${firstRain.hourOfDay}",
                title = title,
                body = body,
                ctaText = "See full forecast for $locationName",
                condition = firstRain.condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(firstRain.condition, firstRain.isDay),
                focusedHourOfDay = firstRain.hourOfDay,
                startTimeLabel = firstRain.timeLabel,
                durationHours = consecutiveRainCount.coerceAtLeast(1),
                severity = if (isHeavy) AlertSeverity.ELEVATED else AlertSeverity.NORMAL,
                isOfficialWarning = false,
                materialHash = hash
            )
        }

        // 4. Snowfall
        if (snowHours.isNotEmpty()) {
            val firstSnow = snowHours.first()
            val duration = snowHours.size.coerceIn(1, 6)
            val title = if (snowHours.any { it.condition.snowIntensity >= 0.7f }) "Heavy snow expected" else "Snow expected"
            val body = "Snow is expected in $locationName around ${firstSnow.timeLabel}"
            val hash = "snow_${firstSnow.hourOfDay}_${duration}_${title.hashCode()}"
            return WeatherAlertEvent(
                eventId = "snow_event_${location.name}_${firstSnow.hourOfDay}",
                title = title,
                body = body,
                ctaText = "See full forecast for $locationName",
                condition = firstSnow.condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(firstSnow.condition, firstSnow.isDay),
                focusedHourOfDay = firstSnow.hourOfDay,
                startTimeLabel = firstSnow.timeLabel,
                durationHours = duration,
                severity = AlertSeverity.NORMAL,
                isOfficialWarning = false,
                materialHash = hash
            )
        }

        // 5. Dense Fog or Haze
        if (fogHours.isNotEmpty() && current.condition.effectType != WeatherEffectType.FOG) {
            val firstFog = fogHours.first()
            val isHaze = firstFog.condition.displayName.contains("Haze", ignoreCase = true)
            val title = if (isHaze) "Hazy conditions expected" else "Dense fog expected"
            val body = "Reduced visibility expected in $locationName around ${firstFog.timeLabel}"
            val hash = "fog_${firstFog.hourOfDay}_${isHaze}"
            return WeatherAlertEvent(
                eventId = "fog_event_${location.name}_${firstFog.hourOfDay}",
                title = title,
                body = body,
                ctaText = "See full forecast for $locationName",
                condition = firstFog.condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(firstFog.condition, firstFog.isDay),
                focusedHourOfDay = firstFog.hourOfDay,
                startTimeLabel = firstFog.timeLabel,
                durationHours = fogHours.size.coerceIn(1, 4),
                severity = AlertSeverity.NORMAL,
                isOfficialWarning = false,
                materialHash = hash
            )
        }

        // 6. Strong Wind Gusts (Wind > 45 km/h)
        if (current.windSpeedKmh >= 45.0) {
            val speedLabel = if (isFahrenheit) "${(current.windSpeedKmh * 0.621371).roundToInt()} mph" else "${current.windSpeedKmh.roundToInt()} km/h"
            val title = "Strong wind gusts"
            val body = "Winds of $speedLabel expected in $locationName today"
            val hash = "wind_${(current.windSpeedKmh / 5).toInt()}"
            return WeatherAlertEvent(
                eventId = "wind_event_${location.name}",
                title = title,
                body = body,
                ctaText = "See full forecast for $locationName",
                condition = current.condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(current.condition, current.isDay),
                focusedHourOfDay = null,
                startTimeLabel = "Now",
                durationHours = 4,
                severity = AlertSeverity.NORMAL,
                isOfficialWarning = false,
                materialHash = hash
            )
        }

        // 7. Clear Weather Notification (Section 5, 31: "Bright, sunny day ahead")
        // Triggered when daytime conditions ahead are predominantly clear and calm
        val clearHours = forecastWindow.take(6).filter {
            it.condition.effectType == WeatherEffectType.CLEAR && it.isDay
        }
        if (clearHours.size >= 4 && current.isDay) {
            val duration = clearHours.size
            val title = "Bright, sunny day ahead"
            val body = "Clear skies expected in $locationName for the next $duration hours"
            val hash = "clear_${clearHours.first().hourOfDay}_$duration"
            return WeatherAlertEvent(
                eventId = "clear_event_${location.name}_${clearHours.first().hourOfDay}",
                title = title,
                body = body,
                ctaText = "See full forecast for $locationName",
                condition = clearHours.first().condition,
                iconResId = WeatherIconMapper.getIconDrawableRes(clearHours.first().condition, isDay = true),
                focusedHourOfDay = clearHours.first().hourOfDay,
                startTimeLabel = "Now",
                durationHours = duration,
                severity = AlertSeverity.NORMAL,
                isOfficialWarning = false,
                materialHash = hash
            )
        }

        return null
    }

    private fun calculateConsecutiveHours(
        items: List<HourlyItem>,
        startIndex: Int,
        predicate: (HourlyItem) -> Boolean
    ): Int {
        var count = 0
        for (i in startIndex until items.size) {
            if (predicate(items[i])) count++ else break
        }
        return count
    }
}
