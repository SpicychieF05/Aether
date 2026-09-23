package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TomorrowTimelineResponse(
    val data: TomorrowData? = null
)

@JsonClass(generateAdapter = true)
data class TomorrowData(
    val timelines: List<TomorrowTimeline>? = null
)

@JsonClass(generateAdapter = true)
data class TomorrowTimeline(
    val timestep: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val intervals: List<TomorrowInterval>? = null
)

@JsonClass(generateAdapter = true)
data class TomorrowInterval(
    val startTime: String? = null,
    val values: TomorrowValues? = null
)

@JsonClass(generateAdapter = true)
data class TomorrowValues(
    val temperature: Double? = null,
    val temperatureApparent: Double? = null,
    val temperatureMax: Double? = null,
    val temperatureMin: Double? = null,
    val humidity: Double? = null,
    val windSpeed: Double? = null,
    val visibility: Double? = null,
    val precipitationProbability: Double? = null,
    val precipitationIntensity: Double? = null,
    val weatherCode: Int? = null,
    val sunriseTime: String? = null,
    val sunsetTime: String? = null,
    val epaIndex: Int? = null,
    val epaAqi: Int? = null,
    val particulateMatter25: Double? = null,
    val particulateMatter10: Double? = null
)
