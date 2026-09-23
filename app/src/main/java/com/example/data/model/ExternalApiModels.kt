package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WaqiFeedResponse(
    val status: String? = null,
    val data: WaqiData? = null
)

@JsonClass(generateAdapter = true)
data class WaqiData(
    val aqi: Int? = null,
    val idx: Long? = null,
    val city: WaqiCity? = null,
    val dominentpol: String? = null,
    val iaqi: Map<String, WaqiIaqiValue>? = null,
    val time: WaqiTime? = null
)

@JsonClass(generateAdapter = true)
data class WaqiCity(
    val name: String? = null,
    val url: String? = null,
    val geo: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class WaqiIaqiValue(
    val v: Double? = null
)

@JsonClass(generateAdapter = true)
data class WaqiTime(
    val s: String? = null,
    val tz: String? = null,
    val v: Long? = null,
    val iso: String? = null
)

@JsonClass(generateAdapter = true)
data class NominatimResult(
    @param:Json(name = "place_id") val placeId: Long? = null,
    val lat: String? = null,
    val lon: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
    val name: String? = null,
    val type: String? = null,
    val address: NominatimAddress? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddress(
    val village: String? = null,
    val town: String? = null,
    val city: String? = null,
    val municipality: String? = null,
    val suburb: String? = null,
    val county: String? = null,
    val state_district: String? = null,
    val state: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val postcode: String? = null
)
