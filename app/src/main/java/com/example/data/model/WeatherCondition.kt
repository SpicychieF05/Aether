package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.ui.graphics.vector.ImageVector

enum class WeatherEffectType {
    CLEAR,
    CLOUDY,
    RAIN,
    THUNDERSTORM,
    SNOW,
    FOG,
    WINDY
}

data class WeatherCondition(
    val code: Int,
    val displayName: String,
    val effectType: WeatherEffectType,
    val isDay: Boolean = true,
    val rainIntensity: Float = 0f,
    val snowIntensity: Float = 0f,
    val fogIntensity: Float = 0f,
    val isThunder: Boolean = false
) {
    val isFog: Boolean get() = effectType == WeatherEffectType.FOG
    val isRain: Boolean get() = effectType == WeatherEffectType.RAIN || effectType == WeatherEffectType.THUNDERSTORM
    val isSnow: Boolean get() = effectType == WeatherEffectType.SNOW

    fun getIcon(): ImageVector {
        return when (effectType) {
            WeatherEffectType.CLEAR -> if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay
            WeatherEffectType.CLOUDY -> Icons.Default.Cloud
            WeatherEffectType.RAIN -> Icons.Default.WaterDrop
            WeatherEffectType.THUNDERSTORM -> Icons.Default.FlashOn
            WeatherEffectType.SNOW -> Icons.Outlined.AcUnit
            WeatherEffectType.FOG -> Icons.Default.Air
            WeatherEffectType.WINDY -> Icons.Default.Air
        }
    }

    companion object {
        fun fromWmoCode(code: Int, isDay: Boolean = true, precipitation: Double = 0.0, windSpeed: Double = 0.0): WeatherCondition {
            val highWind = windSpeed > 40.0
            return when (code) {
                0 -> WeatherCondition(
                    code = code,
                    displayName = if (isDay) "Clear Sky" else "Clear Night",
                    effectType = if (highWind) WeatherEffectType.WINDY else WeatherEffectType.CLEAR,
                    isDay = isDay
                )
                1 -> WeatherCondition(
                    code = code,
                    displayName = if (isDay) "Mainly Clear" else "Fair Night",
                    effectType = if (highWind) WeatherEffectType.WINDY else WeatherEffectType.CLEAR,
                    isDay = isDay
                )
                2 -> WeatherCondition(
                    code = code,
                    displayName = "Partly Cloudy",
                    effectType = WeatherEffectType.CLOUDY,
                    isDay = isDay
                )
                3 -> WeatherCondition(
                    code = code,
                    displayName = "Overcast",
                    effectType = WeatherEffectType.CLOUDY,
                    isDay = isDay
                )
                45 -> WeatherCondition(
                    code = code,
                    displayName = "Fog",
                    effectType = WeatherEffectType.FOG,
                    isDay = isDay,
                    fogIntensity = 0.7f
                )
                48 -> WeatherCondition(
                    code = code,
                    displayName = "Depositing Rime Fog",
                    effectType = WeatherEffectType.FOG,
                    isDay = isDay,
                    fogIntensity = 0.9f
                )
                51, 53, 55 -> WeatherCondition(
                    code = code,
                    displayName = "Light Drizzle",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.25f
                )
                56, 57 -> WeatherCondition(
                    code = code,
                    displayName = "Freezing Drizzle",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.35f
                )
                61 -> WeatherCondition(
                    code = code,
                    displayName = "Slight Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.4f
                )
                63 -> WeatherCondition(
                    code = code,
                    displayName = "Moderate Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.65f
                )
                65 -> WeatherCondition(
                    code = code,
                    displayName = "Heavy Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 1.0f
                )
                66, 67 -> WeatherCondition(
                    code = code,
                    displayName = "Freezing Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.6f
                )
                71 -> WeatherCondition(
                    code = code,
                    displayName = "Slight Snow Fall",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = 0.3f
                )
                73 -> WeatherCondition(
                    code = code,
                    displayName = "Moderate Snow Fall",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = 0.65f
                )
                75, 77 -> WeatherCondition(
                    code = code,
                    displayName = "Heavy Snow Fall",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = 1.0f
                )
                80, 81 -> WeatherCondition(
                    code = code,
                    displayName = "Rain Showers",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.5f
                )
                82 -> WeatherCondition(
                    code = code,
                    displayName = "Violent Rain Showers",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 1.0f
                )
                85, 86 -> WeatherCondition(
                    code = code,
                    displayName = "Snow Showers",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = 0.7f
                )
                95 -> WeatherCondition(
                    code = code,
                    displayName = "Thunderstorm",
                    effectType = WeatherEffectType.THUNDERSTORM,
                    isDay = isDay,
                    rainIntensity = 0.85f,
                    isThunder = true
                )
                96, 99 -> WeatherCondition(
                    code = code,
                    displayName = "Thunderstorm with Hail",
                    effectType = WeatherEffectType.THUNDERSTORM,
                    isDay = isDay,
                    rainIntensity = 1.0f,
                    isThunder = true
                )
                else -> WeatherCondition(
                    code = code,
                    displayName = if (isDay) "Fair" else "Clear",
                    effectType = WeatherEffectType.CLEAR,
                    isDay = isDay
                )
            }
        }

        fun fromTomorrowCode(code: Int, isDay: Boolean = true, windSpeed: Double = 0.0): WeatherCondition {
            val highWind = windSpeed > 40.0
            return when (code) {
                1000 -> WeatherCondition(
                    code = code,
                    displayName = if (isDay) "Clear Sky" else "Clear Night",
                    effectType = if (highWind) WeatherEffectType.WINDY else WeatherEffectType.CLEAR,
                    isDay = isDay
                )
                1100 -> WeatherCondition(
                    code = code,
                    displayName = if (isDay) "Mostly Clear" else "Fair Night",
                    effectType = if (highWind) WeatherEffectType.WINDY else WeatherEffectType.CLEAR,
                    isDay = isDay
                )
                1101 -> WeatherCondition(
                    code = code,
                    displayName = "Partly Cloudy",
                    effectType = WeatherEffectType.CLOUDY,
                    isDay = isDay
                )
                1102 -> WeatherCondition(
                    code = code,
                    displayName = "Mostly Cloudy",
                    effectType = WeatherEffectType.CLOUDY,
                    isDay = isDay
                )
                1001 -> WeatherCondition(
                    code = code,
                    displayName = "Overcast",
                    effectType = WeatherEffectType.CLOUDY,
                    isDay = isDay
                )
                2000, 2100 -> WeatherCondition(
                    code = code,
                    displayName = if (code == 2100) "Light Fog" else "Fog",
                    effectType = WeatherEffectType.FOG,
                    isDay = isDay,
                    fogIntensity = if (code == 2100) 0.5f else 0.85f
                )
                4000 -> WeatherCondition(
                    code = code,
                    displayName = "Drizzle",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.3f
                )
                4001 -> WeatherCondition(
                    code = code,
                    displayName = "Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.65f
                )
                4200 -> WeatherCondition(
                    code = code,
                    displayName = "Light Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.4f
                )
                4201 -> WeatherCondition(
                    code = code,
                    displayName = "Heavy Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 1.0f
                )
                5000, 5100 -> WeatherCondition(
                    code = code,
                    displayName = if (code == 5100) "Light Snow" else "Snow",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = if (code == 5100) 0.35f else 0.7f
                )
                5001 -> WeatherCondition(
                    code = code,
                    displayName = "Heavy Snow",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = 1.0f
                )
                6000, 6200, 6001, 6201 -> WeatherCondition(
                    code = code,
                    displayName = "Freezing Rain",
                    effectType = WeatherEffectType.RAIN,
                    isDay = isDay,
                    rainIntensity = 0.6f
                )
                7000, 7101, 7102 -> WeatherCondition(
                    code = code,
                    displayName = "Ice Pellets",
                    effectType = WeatherEffectType.SNOW,
                    isDay = isDay,
                    snowIntensity = 0.5f
                )
                8000 -> WeatherCondition(
                    code = code,
                    displayName = "Thunderstorm",
                    effectType = WeatherEffectType.THUNDERSTORM,
                    isDay = isDay,
                    rainIntensity = 0.9f,
                    isThunder = true
                )
                else -> WeatherCondition(
                    code = code,
                    displayName = if (isDay) "Fair" else "Clear",
                    effectType = WeatherEffectType.CLEAR,
                    isDay = isDay
                )
            }
        }
    }
}
