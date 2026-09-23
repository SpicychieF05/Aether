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
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    val visibility: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyUnitsAndValues(
    val time: List<String>? = null,
    @Json(name = "temperature_2m") val temperature2m: List<Double>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @Json(name = "is_day") val isDay: List<Int>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null
)

@JsonClass(generateAdapter = true)
data class DailyUnitsAndValues(
    val time: List<String>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>? = null,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>? = null,
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
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "european_aqi") val europeanAqi: Int? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityHourly(
    val time: List<String>? = null,
    @Json(name = "us_aqi") val usAqi: List<Int?>? = null,
    @Json(name = "european_aqi") val europeanAqi: List<Int?>? = null,
    @Json(name = "pm10") val pm10: List<Double?>? = null,
    @Json(name = "pm2_5") val pm25: List<Double?>? = null
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
    @Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val population: Long? = null,
    val timezone: String? = null
)
