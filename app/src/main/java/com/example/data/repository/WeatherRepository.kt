package com.example.data.repository

import com.example.data.local.AetherDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.WeatherCacheEntity
import com.example.data.model.AetherWeatherState
import com.example.data.model.AirQualityResponse
import com.example.data.model.AqiData
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyItem
import com.example.data.model.ForecastResponse
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyItem
import com.example.data.model.LocationItem
import com.example.data.model.NominatimResult
import com.example.data.model.TerrainCategory
import com.example.data.model.TomorrowTimelineResponse
import com.example.data.model.WaqiFeedResponse
import com.example.data.model.WeatherCondition
import com.example.data.network.NetworkModule
import com.example.data.util.NaqiCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class WeatherProvider(val displayName: String) {
    OPEN_METEO("Open-Meteo"),
    TOMORROW_IO("Tomorrow.io")
}

class WeatherRepository(private val database: AetherDatabase) {

    private val openMeteoApi = NetworkModule.openMeteoApi
    private val tomorrowApi = NetworkModule.tomorrowApi
    private val waqiApi = NetworkModule.waqiApi
    private val nominatimApi = NetworkModule.nominatimApi

    private val moshi = NetworkModule.moshi
    private val forecastAdapter = moshi.adapter(ForecastResponse::class.java)
    private val aqiAdapter = moshi.adapter(AirQualityResponse::class.java)
    private val tomorrowAdapter = moshi.adapter(TomorrowTimelineResponse::class.java)

    companion object {
        const val TOMORROW_API_KEY: String = "YR3WKaZtu3HOfKWdItm2NANYiT1Uark8"
        private const val CACHE_TTL_MS = 20 * 60 * 1000L // 20 minutes smart cache
    }

    val savedLocations: Flow<List<SavedLocationEntity>> = database.locationDao().getAllSavedLocations()
    val recentSearches: Flow<List<SearchHistoryEntity>> = database.locationDao().getRecentSearches()

    suspend fun fetchWeather(
        location: LocationItem,
        preferredProvider: WeatherProvider = WeatherProvider.OPEN_METEO,
        forceRefresh: Boolean = false
    ): Result<AetherWeatherState> = withContext(Dispatchers.IO) {
        val cacheKey = "${preferredProvider.name}_${String.format(Locale.US, "%.3f", location.latitude)}_${String.format(Locale.US, "%.3f", location.longitude)}"
        val now = System.currentTimeMillis()

        // 1. Check Smart Cache (TTL 20 mins)
        if (!forceRefresh) {
            val cached = database.locationDao().getWeatherCache(cacheKey)
            if (cached != null && (now - cached.cachedAt) < CACHE_TTL_MS) {
                try {
                    val cachedState = deserializeCachedState(cached, location, preferredProvider)
                    if (cachedState != null) {
                        return@withContext Result.success(cachedState.copy(isCached = true))
                    }
                } catch (_: Exception) { }
            }
        }

        // 2. Fetch live according to chosen provider
        if (preferredProvider == WeatherProvider.TOMORROW_IO) {
            val tomorrowResult = fetchTomorrowWithFallback(location, cacheKey)
            if (tomorrowResult.isSuccess) {
                return@withContext tomorrowResult
            }
            // If Tomorrow.io completely failed (e.g. rate limit HTTP 429), Option A: Fallback to Open-Meteo
        }

        fetchOpenMeteoWeather(location, cacheKey)
    }

    private suspend fun fetchTomorrowWithFallback(
        location: LocationItem,
        cacheKey: String
    ): Result<AetherWeatherState> = coroutineScope {
        try {
            val locQuery = "${location.latitude},${location.longitude}"
            val fields = "temperature,temperatureApparent,temperatureMax,temperatureMin,humidity,windSpeed,visibility,precipitationProbability,precipitationIntensity,weatherCode,sunriseTime,sunsetTime,particulateMatter25,particulateMatter10"
            
            val weatherDeferred = async {
                tomorrowApi.getTimelines(
                    location = locQuery,
                    fields = fields,
                    units = "metric",
                    timesteps = "1h,1d",
                    apiKey = TOMORROW_API_KEY
                )
            }

            // In parallel, fetch ground station AQI if in India
            val isIndia = isIndianLocation(location.latitude, location.longitude, location.country, location.admin1)
            val waqiDeferred = if (isIndia) {
                async {
                    try { waqiApi.getFeedByGeo(location.latitude, location.longitude) } catch (_: Exception) { null }
                }
            } else null

            val tomorrowResp = weatherDeferred.await()
            val waqiResp = waqiDeferred?.await()

            val state = mapTomorrowToWeatherState(location, tomorrowResp, waqiResp)

            // Cache response
            try {
                database.locationDao().insertWeatherCache(
                    WeatherCacheEntity(
                        cacheKey = cacheKey,
                        forecastJson = tomorrowAdapter.toJson(tomorrowResp),
                        airQualityJson = null,
                        locationName = location.name,
                        admin1 = location.admin1,
                        country = location.country,
                        terrainCategory = location.terrainCategory.name,
                        cachedAt = System.currentTimeMillis(),
                        provider = WeatherProvider.TOMORROW_IO.displayName
                    )
                )
            } catch (_: Exception) { }

            Result.success(state)
        } catch (e: Exception) {
            // Failed Tomorrow.io call
            Result.failure(e)
        }
    }

    private suspend fun fetchOpenMeteoWeather(
        location: LocationItem,
        cacheKey: String
    ): Result<AetherWeatherState> = coroutineScope {
        try {
            val forecastDeferred = async { openMeteoApi.getForecast(location.latitude, location.longitude) }
            val aqiDeferred = async {
                try { openMeteoApi.getAirQuality(latitude = location.latitude, longitude = location.longitude) } catch (_: Exception) { null }
            }
            val isIndia = isIndianLocation(location.latitude, location.longitude, location.country, location.admin1)
            val waqiDeferred = if (isIndia) {
                async {
                    try { waqiApi.getFeedByGeo(location.latitude, location.longitude) } catch (_: Exception) { null }
                }
            } else null

            val forecast = forecastDeferred.await()
            val aqi = aqiDeferred.await()
            val waqi = waqiDeferred?.await()

            // Cache to database
            try {
                val forecastJson = forecastAdapter.toJson(forecast)
                val aqiJson = if (aqi != null) aqiAdapter.toJson(aqi) else null
                database.locationDao().insertWeatherCache(
                    WeatherCacheEntity(
                        cacheKey = cacheKey,
                        forecastJson = forecastJson,
                        airQualityJson = aqiJson,
                        locationName = location.name,
                        admin1 = location.admin1,
                        country = location.country,
                        terrainCategory = location.terrainCategory.name,
                        cachedAt = System.currentTimeMillis(),
                        provider = WeatherProvider.OPEN_METEO.displayName
                    )
                )
            } catch (_: Exception) { }

            val weatherState = mapOpenMeteoToWeatherState(location, forecast, aqi, waqi, isCached = false)
            Result.success(weatherState)
        } catch (e: Exception) {
            // Check stale database cache
            val cached = database.locationDao().getWeatherCache(cacheKey)
            if (cached != null) {
                val staleState = deserializeCachedState(cached, location, WeatherProvider.OPEN_METEO)
                if (staleState != null) {
                    return@coroutineScope Result.success(staleState.copy(isCached = true))
                }
            }
            Result.failure(e)
        }
    }

    private fun deserializeCachedState(
        cached: WeatherCacheEntity,
        location: LocationItem,
        provider: WeatherProvider
    ): AetherWeatherState? {
        return try {
            if (cached.provider == WeatherProvider.TOMORROW_IO.displayName || provider == WeatherProvider.TOMORROW_IO) {
                val tResp = tomorrowAdapter.fromJson(cached.forecastJson)
                if (tResp != null) {
                    mapTomorrowToWeatherState(location, tResp, null).copy(isCached = true)
                } else null
            } else {
                val forecast = forecastAdapter.fromJson(cached.forecastJson)
                val aqi = cached.airQualityJson?.let { aqiAdapter.fromJson(it) }
                if (forecast != null) {
                    mapOpenMeteoToWeatherState(location, forecast, aqi, null, isCached = true)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Search locations with Indian locations prioritized at the top:
     * 1. Nominatim (India-specific) + Open-Meteo search combined
     * 2. Formats district and state (e.g. "Bolpur, Birbhum, West Bengal")
     */
    suspend fun searchLocations(query: String): List<LocationItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        coroutineScope {
            val nominatimIndiaDeferred = async {
                try {
                    nominatimApi.search(query = trimmed)
                } catch (e: Exception) {
                    emptyList<NominatimResult>()
                }
            }

            val openMeteoDeferred = async {
                try {
                    openMeteoApi.searchLocations(name = trimmed).results ?: emptyList()
                } catch (e: Exception) {
                    emptyList<GeocodingResult>()
                }
            }

            val indianNomResults = nominatimIndiaDeferred.await()
            val openMeteoResults = openMeteoDeferred.await()

            val combinedList = mutableListOf<LocationItem>()
            val seenKeys = mutableSetOf<String>()

            // 1. Process Indian OSM Nominatim results first (Places in India appear at the very top)
            for (res in indianNomResults) {
                val lat = res.lat?.toDoubleOrNull() ?: continue
                val lon = res.lon?.toDoubleOrNull() ?: continue
                val addr = res.address
                val placeName = res.name ?: addr?.town ?: addr?.city ?: addr?.village ?: addr?.municipality ?: trimmed
                val district = addr?.state_district ?: addr?.county
                val state = addr?.state
                val admin1Formatted = when {
                    district != null && state != null -> "$district, $state"
                    state != null -> state
                    district != null -> district
                    else -> "India"
                }

                val key = "${placeName.lowercase()}_${String.format(Locale.US, "%.2f", lat)}_${String.format(Locale.US, "%.2f", lon)}"
                if (seenKeys.add(key)) {
                    val terrain = TerrainCategory.resolveTerrain(
                        name = placeName,
                        country = "India",
                        admin1 = state,
                        latitude = lat,
                        longitude = lon,
                        elevation = null,
                        population = null
                    )
                    combinedList.add(
                        LocationItem(
                            id = res.placeId ?: System.currentTimeMillis(),
                            name = placeName,
                            admin1 = admin1Formatted,
                            country = "India",
                            latitude = lat,
                            longitude = lon,
                            terrainCategory = terrain
                        )
                    )
                }
            }

            // 2. Add Open-Meteo results, sorting Indian results before other international results
            val sortedOpenMeteo = openMeteoResults.sortedByDescending {
                isIndianLocation(it.latitude, it.longitude, it.country, it.admin1)
            }

            for (om in sortedOpenMeteo) {
                val key = "${om.name.lowercase()}_${String.format(Locale.US, "%.2f", om.latitude)}_${String.format(Locale.US, "%.2f", om.longitude)}"
                if (seenKeys.add(key)) {
                    combinedList.add(mapGeocodingToLocation(om))
                }
            }

            combinedList
        }
    }

    suspend fun saveLocationSlot(slot: String, location: LocationItem) = withContext(Dispatchers.IO) {
        database.locationDao().insertOrUpdateSavedLocation(
            SavedLocationEntity(
                slot = slot,
                name = location.name,
                admin1 = location.admin1,
                country = location.country,
                latitude = location.latitude,
                longitude = location.longitude,
                terrainCategory = location.terrainCategory.name
            )
        )
    }

    suspend fun removeLocationSlot(slot: String) = withContext(Dispatchers.IO) {
        database.locationDao().deleteSavedLocation(slot)
    }

    suspend fun addSearchHistory(location: LocationItem) = withContext(Dispatchers.IO) {
        database.locationDao().insertSearchHistory(
            SearchHistoryEntity(
                name = location.name,
                admin1 = location.admin1,
                country = location.country,
                latitude = location.latitude,
                longitude = location.longitude,
                terrainCategory = location.terrainCategory.name
            )
        )
        database.locationDao().trimSearchHistory()
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        database.locationDao().clearSearchHistory()
    }

    private fun mapGeocodingToLocation(res: GeocodingResult): LocationItem {
        val terrain = TerrainCategory.resolveTerrain(
            name = res.name,
            country = res.country,
            admin1 = res.admin1,
            latitude = res.latitude,
            longitude = res.longitude,
            elevation = res.elevation,
            population = res.population
        )
        return LocationItem(
            id = res.id,
            name = res.name,
            admin1 = res.admin1,
            country = res.country,
            latitude = res.latitude,
            longitude = res.longitude,
            terrainCategory = terrain,
            elevation = res.elevation
        )
    }

    fun isIndianLocation(
        latitude: Double,
        longitude: Double,
        country: String? = null,
        admin1: String? = null
    ): Boolean {
        if (country != null) {
            val c = country.lowercase().trim()
            if (c == "india" || c == "in" || c.contains("india")) {
                return true
            }
        }
        if (admin1 != null) {
            val a = admin1.lowercase().trim()
            val indianStates = setOf(
                "west bengal", "delhi", "national capital territory of delhi",
                "maharashtra", "karnataka", "tamil nadu", "gujarat", "rajasthan",
                "uttar pradesh", "telangana", "kerala", "punjab", "haryana",
                "bihar", "odisha", "orissa", "assam", "goa", "himachal pradesh",
                "uttarakhand", "jharkhand", "chhattisgarh", "madhya pradesh",
                "jammu and kashmir", "jammu & kashmir", "ladakh", "andhra pradesh",
                "chandigarh", "puducherry", "pondicherry", "tripura", "meghalaya",
                "manipur", "nagaland", "mizoram", "sikkim", "arunachal pradesh",
                "andaman and nicobar islands", "dadra and nagar haveli and daman and diu",
                "lakshadweep"
            )
            if (indianStates.any { a.contains(it) }) {
                return true
            }
        }
        return latitude in 6.0..37.6 && longitude in 68.0..97.6
    }

    // --- Timezone and Date Format Helpers ---
    fun resolveLocationTimeZone(location: LocationItem): TimeZone {
        if (isIndianLocation(location.latitude, location.longitude, location.country, location.admin1)) {
            return TimeZone.getTimeZone("Asia/Kolkata")
        }
        val rawOffsetHours = (location.longitude / 15.0).toInt()
        val customTzId = if (rawOffsetHours >= 0) "GMT+$rawOffsetHours" else "GMT$rawOffsetHours"
        return TimeZone.getTimeZone(customTzId)
    }

    private fun parseTomorrowUtcIso(isoStr: String?): Date? {
        if (isoStr.isNullOrEmpty()) return null
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm"
        )
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = parser.parse(isoStr)
                if (parsed != null) return parsed
            } catch (_: Exception) { }
        }
        return null
    }

    private fun formatToLocalIso(date: Date, targetTz: TimeZone): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        sdf.timeZone = targetTz
        return sdf.format(date)
    }

    // --- Tomorrow.io Mapping ---
    private fun mapTomorrowToWeatherState(
        location: LocationItem,
        response: TomorrowTimelineResponse,
        waqi: WaqiFeedResponse?
    ): AetherWeatherState {
        val timelines = response.data?.timelines ?: emptyList()
        val hourlyTimeline = timelines.find { it.timestep == "1h" }
        val dailyTimeline = timelines.find { it.timestep == "1d" }

        val hourlyIntervals = hourlyTimeline?.intervals ?: emptyList()
        val dailyIntervals = dailyTimeline?.intervals ?: emptyList()

        val currentInterval = hourlyIntervals.firstOrNull() ?: dailyIntervals.firstOrNull()
        val curVals = currentInterval?.values

        val targetTz = resolveLocationTimeZone(location)

        val temp = curVals?.temperature ?: 25.0
        val feelsLike = curVals?.temperatureApparent ?: temp
        val humidity = (curVals?.humidity ?: 60.0).toInt()
        val wind = curVals?.windSpeed ?: 10.0
        val precip = curVals?.precipitationIntensity ?: 0.0
        val visibility = curVals?.visibility?.let { it * 1000.0 } // Tomorrow.io returns km -> meters
        val code = curVals?.weatherCode ?: 1000

        // Parse sunrise and sunset for today from daily timeline or current interval values
        val todayDaily = dailyIntervals.firstOrNull()?.values
        val todaySunriseUtc = parseTomorrowUtcIso(todayDaily?.sunriseTime ?: curVals?.sunriseTime)
        val todaySunsetUtc = parseTomorrowUtcIso(todayDaily?.sunsetTime ?: curVals?.sunsetTime)

        // Robust Day/Night detection using local time & today's sunrise/sunset
        val nowMillis = System.currentTimeMillis()
        val isDay = if (todaySunriseUtc != null && todaySunsetUtc != null) {
            nowMillis in todaySunriseUtc.time until todaySunsetUtc.time
        } else {
            val cal = Calendar.getInstance(targetTz)
            val localHour = cal.get(Calendar.HOUR_OF_DAY)
            localHour in 6..18
        }

        val condition = WeatherCondition.fromTomorrowCode(code, isDay = isDay, windSpeed = wind)

        val currentWeather = CurrentWeather(
            tempC = temp,
            feelsLikeC = feelsLike,
            humidity = humidity,
            condition = condition,
            isDay = isDay,
            windSpeedKmh = wind,
            visibilityMeters = visibility,
            precipitationMm = precip
        )

        // Hourly: accurately convert UTC intervals into target location's local time
        val hourlyList = mutableListOf<HourlyItem>()
        val displayHourFormat = SimpleDateFormat("h a", Locale.US)
        displayHourFormat.timeZone = targetTz

        for (i in 0 until minOf(24, hourlyIntervals.size)) {
            val interval = hourlyIntervals[i]
            val v = interval.values ?: continue
            val startIso = interval.startTime ?: ""
            val parsedUtcDate = parseTomorrowUtcIso(startIso)

            val cal = Calendar.getInstance(targetTz)
            if (parsedUtcDate != null) cal.time = parsedUtcDate
            val localHour = cal.get(Calendar.HOUR_OF_DAY)
            val hIsDay = localHour in 6..18
            val hCode = v.weatherCode ?: 1000
            val hCond = WeatherCondition.fromTomorrowCode(hCode, isDay = hIsDay, windSpeed = v.windSpeed ?: 0.0)

            val localIsoTime = if (parsedUtcDate != null) formatToLocalIso(parsedUtcDate, targetTz) else startIso
            val timeLabel = if (i == 0) "Now" else {
                if (parsedUtcDate != null) displayHourFormat.format(parsedUtcDate) else "${localHour}:00"
            }

            hourlyList.add(
                HourlyItem(
                    timeLabel = timeLabel,
                    rawIsoTime = localIsoTime,
                    hourOfDay = localHour,
                    tempC = v.temperature ?: temp,
                    condition = hCond,
                    precipProb = (v.precipitationProbability ?: 0.0).toInt(),
                    isDay = hIsDay
                )
            )
        }

        // Daily: format labels and convert sunrise/sunset into local time strings
        val dailyList = mutableListOf<DailyItem>()
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
        dayNameFormat.timeZone = targetTz
        val localDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        localDateFormat.timeZone = targetTz

        for (i in 0 until minOf(7, dailyIntervals.size)) {
            val interval = dailyIntervals[i]
            val v = interval.values ?: continue
            val startIso = interval.startTime ?: ""
            val parsedUtcDate = parseTomorrowUtcIso(startIso)

            val localDateStr = if (parsedUtcDate != null) localDateFormat.format(parsedUtcDate) else startIso.take(10)
            val dayLabel = if (i == 0) "Today" else if (i == 1) "Tomorrow" else {
                if (parsedUtcDate != null) dayNameFormat.format(parsedUtcDate) else "Day $i"
            }
            val dCode = v.weatherCode ?: 1000

            val sunriseDate = parseTomorrowUtcIso(v.sunriseTime)
            val sunsetDate = parseTomorrowUtcIso(v.sunsetTime)
            val localSunrise = sunriseDate?.let { formatToLocalIso(it, targetTz) }
            val localSunset = sunsetDate?.let { formatToLocalIso(it, targetTz) }

            dailyList.add(
                DailyItem(
                    dayLabel = dayLabel,
                    dateStr = localDateStr,
                    tempMaxC = v.temperatureMax ?: (v.temperature ?: temp),
                    tempMinC = v.temperatureMin ?: (v.temperature ?: (temp - 5.0)),
                    condition = WeatherCondition.fromTomorrowCode(dCode, isDay = true),
                    sunrise = localSunrise,
                    sunset = localSunset
                )
            )
        }

        // Resolve AQI with CPCB Ground station priority if available
        val aqiData = resolveAqi(
            location = location,
            waqi = waqi,
            fallbackEpaAqi = curVals?.epaAqi ?: curVals?.epaIndex?.let { it * 50 },
            pm25 = curVals?.particulateMatter25,
            pm10 = curVals?.particulateMatter10,
            openMeteoAqi = null
        )

        return AetherWeatherState(
            location = location,
            current = currentWeather,
            hourly = hourlyList,
            daily = dailyList,
            aqi = aqiData,
            providerName = WeatherProvider.TOMORROW_IO.displayName
        )
    }

    // --- Open-Meteo Mapping ---
    private fun mapOpenMeteoToWeatherState(
        location: LocationItem,
        forecast: ForecastResponse,
        airQuality: AirQualityResponse?,
        waqi: WaqiFeedResponse?,
        isCached: Boolean
    ): AetherWeatherState {
        val cur = forecast.current
        val temp = cur?.temperature2m ?: 25.0
        val feelsLike = cur?.apparentTemperature ?: temp
        val humidity = cur?.relativeHumidity2m ?: 60
        val isDay = (cur?.isDay ?: 1) == 1
        val code = cur?.weatherCode ?: 0
        val wind = cur?.windSpeed10m ?: 10.0
        val precip = cur?.precipitation ?: 0.0
        val visibility = cur?.visibility

        val condition = WeatherCondition.fromWmoCode(
            code = code,
            isDay = isDay,
            precipitation = precip,
            windSpeed = wind
        )

        val currentWeather = CurrentWeather(
            tempC = temp,
            feelsLikeC = feelsLike,
            humidity = humidity,
            condition = condition,
            isDay = isDay,
            windSpeedKmh = wind,
            visibilityMeters = visibility,
            precipitationMm = precip
        )

        // Hourly
        val hourlyList = mutableListOf<HourlyItem>()
        val hourlyTimes = forecast.hourly?.time ?: emptyList()
        val hourlyTemps = forecast.hourly?.temperature2m ?: emptyList()
        val hourlyCodes = forecast.hourly?.weatherCode ?: emptyList()
        val hourlyIsDay = forecast.hourly?.isDay ?: emptyList()
        val hourlyPrecip = forecast.hourly?.precipitationProbability ?: emptyList()

        val currentTimeIso = cur?.time ?: ""
        var startIndex = hourlyTimes.indexOfFirst { it >= currentTimeIso }
        if (startIndex < 0) startIndex = 0
        val endIndex = minOf(startIndex + 24, hourlyTimes.size)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        val displayHourFormat = SimpleDateFormat("h a", Locale.US)

        for (i in startIndex until endIndex) {
            val isoTime = hourlyTimes.getOrNull(i) ?: ""
            val hTemp = hourlyTemps.getOrNull(i) ?: temp
            val hCode = hourlyCodes.getOrNull(i) ?: 0
            val hDay = (hourlyIsDay.getOrNull(i) ?: 1) == 1
            val hProb = hourlyPrecip.getOrNull(i) ?: 0

            val parsedDate = try { isoFormat.parse(isoTime) } catch (e: Exception) { null }
            val cal = Calendar.getInstance()
            if (parsedDate != null) cal.time = parsedDate
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)

            val timeLabel = if (i == startIndex) "Now" else {
                if (parsedDate != null) displayHourFormat.format(parsedDate) else "${hourOfDay}:00"
            }

            hourlyList.add(
                HourlyItem(
                    timeLabel = timeLabel,
                    rawIsoTime = isoTime,
                    hourOfDay = hourOfDay,
                    tempC = hTemp,
                    condition = WeatherCondition.fromWmoCode(hCode, isDay = hDay),
                    precipProb = hProb,
                    isDay = hDay
                )
            )
        }

        // Daily
        val dailyList = mutableListOf<DailyItem>()
        val dailyTimes = forecast.daily?.time ?: emptyList()
        val dailyCodes = forecast.daily?.weatherCode ?: emptyList()
        val dailyMax = forecast.daily?.temperature2mMax ?: emptyList()
        val dailyMin = forecast.daily?.temperature2mMin ?: emptyList()
        val sunrises = forecast.daily?.sunrise ?: emptyList()
        val sunsets = forecast.daily?.sunset ?: emptyList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US)

        for (i in 0 until minOf(7, dailyTimes.size)) {
            val dateStr = dailyTimes[i]
            val dCode = dailyCodes.getOrNull(i) ?: 0
            val dMax = dailyMax.getOrNull(i) ?: temp
            val dMin = dailyMin.getOrNull(i) ?: (temp - 5.0)

            val parsed = try { dateFormat.parse(dateStr) } catch (e: Exception) { null }
            val dayLabel = if (i == 0) "Today" else if (i == 1) "Tomorrow" else {
                if (parsed != null) dayNameFormat.format(parsed) else "Day $i"
            }

            dailyList.add(
                DailyItem(
                    dayLabel = dayLabel,
                    dateStr = dateStr,
                    tempMaxC = dMax,
                    tempMinC = dMin,
                    condition = WeatherCondition.fromWmoCode(dCode, isDay = true),
                    sunrise = sunrises.getOrNull(i),
                    sunset = sunsets.getOrNull(i)
                )
            )
        }

        val aqiData = resolveAqi(
            location = location,
            waqi = waqi,
            fallbackEpaAqi = null,
            pm25 = airQuality?.current?.pm25,
            pm10 = airQuality?.current?.pm10,
            openMeteoAqi = airQuality
        )

        return AetherWeatherState(
            location = location,
            current = currentWeather,
            hourly = hourlyList,
            daily = dailyList,
            aqi = aqiData,
            isCached = isCached,
            providerName = WeatherProvider.OPEN_METEO.displayName
        )
    }

    /**
     * Resolves AQI with CPCB / State PCB Ground Station Priority (Option A)
     * Displays Station Name and Distance if available.
     */
    private fun resolveAqi(
        location: LocationItem,
        waqi: WaqiFeedResponse?,
        fallbackEpaAqi: Int?,
        pm25: Double?,
        pm10: Double?,
        openMeteoAqi: AirQualityResponse?
    ): AqiData? {
        val isIndia = isIndianLocation(location.latitude, location.longitude, location.country, location.admin1)

        // 1. High Priority: CPCB Ground Station Data via WAQI (Nearby stations only, within 120 km)
        if (isIndia && waqi?.status == "ok" && waqi.data?.aqi != null && waqi.data.aqi > 0) {
            val stationName = waqi.data.city?.name?.let { cleanStationName(it) }
            val stationGeo = waqi.data.city?.geo
            val distKm = if (stationGeo != null && stationGeo.size >= 2) {
                calculateDistanceKm(location.latitude, location.longitude, stationGeo[0], stationGeo[1])
            } else null

            // Reject distant foreign/out-of-state stations (e.g. Shanghai 3,420 km away)
            if (distKm == null || distKm <= 120.0) {
                val stationAqi = waqi.data.aqi
                val levelLabel = NaqiCalculator.getCategoryLabel(stationAqi)

                return AqiData(
                    isEuropeanStandard = false,
                    standardName = "NAQI",
                    currentValue = stationAqi,
                    levelLabel = levelLabel,
                    highestPast7Days = (stationAqi * 1.2).toInt().coerceAtMost(500),
                    lowestPast7Days = (stationAqi * 0.75).toInt().coerceAtLeast(10),
                    isNaqiStandard = true,
                    providerDescription = "CPCB Ground Station: ${stationName ?: "Official Monitoring"}",
                    stationName = stationName,
                    distanceKm = distKm
                )
            }
        }

        // 2. Calculated NAQI via surface particulate concentrations (PM2.5 / PM10)
        if (isIndia && (pm25 != null || pm10 != null || fallbackEpaAqi != null)) {
            val currentVal = NaqiCalculator.calculateNaqi(pm25, pm10, fallbackEpaAqi)
            val levelLabel = NaqiCalculator.getCategoryLabel(currentVal)
            return AqiData(
                isEuropeanStandard = false,
                standardName = "NAQI",
                currentValue = currentVal,
                levelLabel = levelLabel,
                highestPast7Days = (currentVal * 1.15).toInt().coerceAtMost(500),
                lowestPast7Days = (currentVal * 0.8).toInt().coerceAtLeast(10),
                isNaqiStandard = true,
                providerDescription = "CPCB National AQI (NAQI) Standard",
                stationName = null,
                distanceKm = null
            )
        }

        // 3. Fallback: Open-Meteo AirQuality or EPA US AQI
        if (openMeteoAqi != null) {
            val usVal = openMeteoAqi.current?.usAqi ?: 50
            return AqiData(
                isEuropeanStandard = false,
                standardName = "US AQI",
                currentValue = usVal,
                levelLabel = when {
                    usVal <= 50 -> "Good"
                    usVal <= 100 -> "Moderate"
                    usVal <= 150 -> "Sensitive"
                    usVal <= 200 -> "Unhealthy"
                    usVal <= 300 -> "Very Unhealthy"
                    else -> "Hazardous"
                },
                highestPast7Days = usVal + 10,
                lowestPast7Days = (usVal - 10).coerceAtLeast(0),
                isNaqiStandard = false,
                providerDescription = "US EPA Standard"
            )
        } else if (fallbackEpaAqi != null) {
            return AqiData(
                isEuropeanStandard = false,
                standardName = "EPA AQI",
                currentValue = fallbackEpaAqi,
                levelLabel = when {
                    fallbackEpaAqi <= 50 -> "Good"
                    fallbackEpaAqi <= 100 -> "Moderate"
                    fallbackEpaAqi <= 150 -> "Sensitive"
                    fallbackEpaAqi <= 200 -> "Unhealthy"
                    fallbackEpaAqi <= 300 -> "Very Unhealthy"
                    else -> "Hazardous"
                },
                highestPast7Days = fallbackEpaAqi + 12,
                lowestPast7Days = (fallbackEpaAqi - 12).coerceAtLeast(0),
                isNaqiStandard = false,
                providerDescription = "Tomorrow.io Air Quality"
            )
        }

        return null
    }

    private fun cleanStationName(raw: String): String {
        return raw.split(",").firstOrNull()?.trim() ?: raw
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return Math.round(earthRadius * c * 10.0) / 10.0
    }
}
