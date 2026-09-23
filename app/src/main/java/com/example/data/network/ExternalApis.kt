package com.example.data.network

import com.example.data.model.NominatimResult
import com.example.data.model.TomorrowTimelineResponse
import com.example.data.model.WaqiFeedResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TomorrowApi {
    @GET("v4/timelines")
    suspend fun getTimelines(
        @Query("location") location: String, // "lat,lon"
        @Query("fields") fields: String,
        @Query("units") units: String = "metric",
        @Query("timesteps") timesteps: String, // "1h,1d"
        @Query("apikey") apiKey: String
    ): TomorrowTimelineResponse
}

interface WaqiApi {
    @GET("feed/geo:{lat};{lng}/")
    suspend fun getFeedByGeo(
        @Path("lat") lat: Double,
        @Path("lng") lng: Double,
        @Query("token") token: String = "demo"
    ): WaqiFeedResponse
}

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countryCodes: String = "in",
        @Header("User-Agent") userAgent: String = "AetherWeatherApp/1.0 (mallickchirantan@gmail.com)"
    ): List<NominatimResult>

    @GET("search")
    suspend fun searchGlobal(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Header("User-Agent") userAgent: String = "AetherWeatherApp/1.0 (mallickchirantan@gmail.com)"
    ): List<NominatimResult>
}
