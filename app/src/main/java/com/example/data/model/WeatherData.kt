package com.example.data.model

data class LocationItem(
    val id: Long = 0L,
    val name: String,
    val admin1: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
    val terrainCategory: TerrainCategory,
    val elevation: Double? = null,
    val isGps: Boolean = false
) {
    val formattedTitle: String
        get() = when {
            admin1 != null && country != null -> "$name, $admin1"
            country != null -> "$name, $country"
            else -> name
        }

    val subtitle: String
        get() = when {
            country != null && admin1 != null -> "$admin1, $country"
            country != null -> country
            else -> ""
        }
}

data class CurrentWeather(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val windSpeedKmh: Double,
    val visibilityMeters: Double?,
    val precipitationMm: Double
) {
    val isFoggy: Boolean get() = condition.isFog || (visibilityMeters != null && visibilityMeters < 3000)
}

data class HourlyItem(
    val timeLabel: String,
    val rawIsoTime: String,
    val hourOfDay: Int,
    val tempC: Double,
    val condition: WeatherCondition,
    val precipProb: Int,
    val isDay: Boolean
)

data class DailyItem(
    val dayLabel: String,
    val dateStr: String,
    val tempMaxC: Double,
    val tempMinC: Double,
    val condition: WeatherCondition,
    val sunrise: String?,
    val sunset: String?
)

data class AqiData(
    val isEuropeanStandard: Boolean = false,
    val standardName: String,
    val currentValue: Int,
    val levelLabel: String,
    val highestPast7Days: Int,
    val lowestPast7Days: Int,
    val isNaqiStandard: Boolean = false,
    val providerDescription: String = ""
)

data class AetherWeatherState(
    val location: LocationItem,
    val current: CurrentWeather,
    val hourly: List<HourlyItem>,
    val daily: List<DailyItem>,
    val aqi: AqiData?,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isCached: Boolean = false
)
