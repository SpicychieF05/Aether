package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val elevation: Double? = null,
    val current: CurrentUnitsAndValues? = null,
    val hourly: HourlyUnitsAndValues? = null,
    val daily: DailyUnitsAndValues? = null
)

@JsonClass(generateAdapter = true)
data class CurrentUnitsAndValues(
    val time: String? = null,
    @param:Json(name = "temperature_2m") val temperature2m: Double? = null,
    @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @param:Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @param:Json(name = "is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @param:Json(name = "weather_code") val weatherCode: Int? = null,
    @param:Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    val visibility: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyUnitsAndValues(
    val time: List<String>? = null,
    @param:Json(name = "temperature_2m") val temperature2m: List<Double>? = null,
    @param:Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @param:Json(name = "is_day") val isDay: List<Int>? = null,
    @param:Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null
)

@JsonClass(generateAdapter = true)
data class DailyUnitsAndValues(
    val time: List<String>? = null,
    @param:Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @param:Json(name = "temperature_2m_max") val temperature2mMax: List<Double>? = null,
    @param:Json(name = "temperature_2m_min") val temperature2mMin: List<Double>? = null,
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: AirQualityCurrent? = null,
    val hourly: AirQualityHourly? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrent(
    val time: String? = null,
    @param:Json(name = "us_aqi") val usAqi: Int? = null,
    @param:Json(name = "european_aqi") val europeanAqi: Int? = null,
    @param:Json(name = "pm10") val pm10: Double? = null,
    @param:Json(name = "pm2_5") val pm25: Double? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityHourly(
    val time: List<String>? = null,
    @param:Json(name = "us_aqi") val usAqi: List<Int?>? = null,
    @param:Json(name = "european_aqi") val europeanAqi: List<Int?>? = null,
    @param:Json(name = "pm10") val pm10: List<Double?>? = null,
    @param:Json(name = "pm2_5") val pm25: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val country: String? = null,
    @param:Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val population: Long? = null,
    val timezone: String? = null
)
