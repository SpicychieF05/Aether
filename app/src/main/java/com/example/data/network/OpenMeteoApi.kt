package com.example.data.network

import com.example.data.model.AirQualityResponse
import com.example.data.model.ForecastResponse
import com.example.data.model.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,visibility",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,is_day,precipitation_probability",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): ForecastResponse

    @GET
    suspend fun getAirQuality(
        @Url url: String = "https://air-quality-api.open-meteo.com/v1/air-quality",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi,pm10,pm2_5",
        @Query("hourly") hourly: String = "us_aqi,european_aqi,pm10,pm2_5",
        @Query("past_days") pastDays: Int = 7,
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto"
    ): AirQualityResponse

    @GET
    suspend fun searchLocations(
        @Url url: String = "https://geocoding-api.open-meteo.com/v1/search",
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}
