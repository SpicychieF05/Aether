package com.example.data.repository

import com.example.data.local.AetherDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.WeatherCacheEntity
import com.example.data.model.AirQualityResponse
import com.example.data.model.AetherWeatherState
import com.example.data.model.AqiData
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyItem
import com.example.data.model.ForecastResponse
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyItem
import com.example.data.model.LocationItem
import com.example.data.model.TerrainCategory
import com.example.data.model.WeatherCondition
import com.example.data.network.NetworkModule
import com.example.data.util.NaqiCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherRepository(private val database: AetherDatabase) {

    private val api = NetworkModule.openMeteoApi
    private val moshi = NetworkModule.moshi
    private val forecastAdapter = moshi.adapter(ForecastResponse::class.java)
    private val aqiAdapter = moshi.adapter(AirQualityResponse::class.java)

    val savedLocations: Flow<List<SavedLocationEntity>> = database.locationDao().getAllSavedLocations()
    val recentSearches: Flow<List<SearchHistoryEntity>> = database.locationDao().getRecentSearches()

    suspend fun fetchWeather(location: LocationItem): Result<AetherWeatherState> = withContext(Dispatchers.IO) {
        val cacheKey = "${String.format(Locale.US, "%.3f", location.latitude)}_${String.format(Locale.US, "%.3f", location.longitude)}"
        try {
            val forecast = api.getForecast(location.latitude, location.longitude)
            val aqi = try {
                api.getAirQuality(latitude = location.latitude, longitude = location.longitude)
            } catch (e: Exception) {
                null
            }

            // Cache to database
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
                    terrainCategory = location.terrainCategory.name
                )
            )

            val weatherState = mapToWeatherState(location, forecast, aqi, isCached = false)
            Result.success(weatherState)
        } catch (e: Exception) {
            // Attempt to restore from cache
            val cached = database.locationDao().getWeatherCache(cacheKey)
            if (cached != null) {
                try {
                    val forecast = forecastAdapter.fromJson(cached.forecastJson)
                    val aqi = cached.airQualityJson?.let { aqiAdapter.fromJson(it) }
                    if (forecast != null) {
                        val state = mapToWeatherState(location, forecast, aqi, isCached = true)
                        return@withContext Result.success(state)
                    }
                } catch (cacheErr: Exception) {
                    // Fallthrough to error
                }
            }
            Result.failure(e)
        }
    }

    suspend fun searchLocations(query: String): List<LocationItem> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val response = api.searchLocations(name = query.trim())
            response.results?.map { mapGeocodingToLocation(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
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

    fun isIndianLocation(latitude: Double, longitude: Double, country: String?): Boolean {
        if (country != null) {
            val c = country.lowercase().trim()
            if (c == "india" || c == "in" || c.contains("india")) {
                return true
            }
        }
        return latitude in 6.5..37.5 && longitude in 68.0..97.5
    }

    private fun isEuropeanLocation(latitude: Double, longitude: Double, country: String?): Boolean {
        val euroCountries = setOf(
            "germany", "france", "united kingdom", "italy", "spain", "poland",
            "netherlands", "belgium", "switzerland", "austria", "sweden", "norway",
            "finland", "denmark", "portugal", "greece", "czech republic", "ireland",
            "hungary", "romania", "croatia", "iceland", "slovakia", "estonia", "latvia", "lithuania"
        )
        if (country != null && euroCountries.contains(country.lowercase().trim())) {
            return true
        }
        return latitude in 35.0..71.0 && longitude in -25.0..40.0
    }

    private fun mapToWeatherState(
        location: LocationItem,
        forecast: ForecastResponse,
        airQuality: AirQualityResponse?,
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

        // Match next 24 entries or from current hour
        val currentTimeIso = cur?.time ?: ""
        var startIndex = hourlyTimes.indexOfFirst { it >= currentTimeIso }
        if (startIndex < 0) startIndex = 0

        val endIndex = minOf(startIndex + 24, hourlyTimes.size)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        val displayHourFormat = SimpleDateFormat("h a", Locale.US)

        for (i in startIndex until endIndex) {
            val isoTime = hourlyTimes.getOrNull(i) ?: ""
            val hTemp = hourlyTemps.getOrNull(i) ?: temp
            val hCode = hourlyCodes.getOrNull(i) ?: code
            val hIsDay = (hourlyIsDay.getOrNull(i) ?: 1) == 1
            val hPrecip = hourlyPrecip.getOrNull(i) ?: 0

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
                    condition = WeatherCondition.fromWmoCode(hCode, hIsDay),
                    precipProb = hPrecip ?: 0,
                    isDay = hIsDay
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

        // AQI (Indian NAQI, European AQI, or US AQI based on geographic origin)
        val isIndia = isIndianLocation(location.latitude, location.longitude, location.country)
        val isEurope = !isIndia && isEuropeanLocation(location.latitude, location.longitude, location.country)

        val aqiData = if (airQuality != null) {
            val standardName: String
            val currentVal: Int
            val levelLabel: String
            val high: Int
            val low: Int

            if (isIndia) {
                standardName = NaqiCalculator.STANDARD_NAME
                currentVal = NaqiCalculator.calculateNaqi(
                    pm25 = airQuality.current?.pm25,
                    pm10 = airQuality.current?.pm10,
                    fallbackUsAqi = airQuality.current?.usAqi
                )
                levelLabel = NaqiCalculator.getCategoryLabel(currentVal)

                val hourlyPm25 = airQuality.hourly?.pm25 ?: emptyList()
                val hourlyPm10 = airQuality.hourly?.pm10 ?: emptyList()
                val hourlyUsAqi = airQuality.hourly?.usAqi ?: emptyList()
                val count = maxOf(hourlyPm25.size, hourlyPm10.size, hourlyUsAqi.size)

                val hourlyNaqiList = mutableListOf<Int>()
                for (i in 0 until count) {
                    val p25 = hourlyPm25.getOrNull(i)
                    val p10 = hourlyPm10.getOrNull(i)
                    val uAqi = hourlyUsAqi.getOrNull(i)
                    if (p25 != null || p10 != null || uAqi != null) {
                        hourlyNaqiList.add(NaqiCalculator.calculateNaqi(p25, p10, uAqi))
                    }
                }

                high = if (hourlyNaqiList.isNotEmpty()) hourlyNaqiList.maxOrNull() ?: currentVal else currentVal
                low = if (hourlyNaqiList.isNotEmpty()) hourlyNaqiList.minOrNull() ?: currentVal else currentVal
            } else if (isEurope) {
                standardName = "European AQI"
                currentVal = airQuality.current?.europeanAqi ?: 25
                val hourlyVals = airQuality.hourly?.europeanAqi?.filterNotNull() ?: emptyList()
                high = if (hourlyVals.isNotEmpty()) hourlyVals.maxOrNull() ?: currentVal else currentVal
                low = if (hourlyVals.isNotEmpty()) hourlyVals.minOrNull() ?: currentVal else currentVal
                levelLabel = when {
                    currentVal <= 20 -> "Good"
                    currentVal <= 40 -> "Fair"
                    currentVal <= 60 -> "Moderate"
                    currentVal <= 80 -> "Poor"
                    else -> "Very Poor"
                }
            } else {
                standardName = "US AQI"
                currentVal = airQuality.current?.usAqi ?: 55
                val hourlyVals = airQuality.hourly?.usAqi?.filterNotNull() ?: emptyList()
                high = if (hourlyVals.isNotEmpty()) hourlyVals.maxOrNull() ?: currentVal else currentVal
                low = if (hourlyVals.isNotEmpty()) hourlyVals.minOrNull() ?: currentVal else currentVal
                levelLabel = when {
                    currentVal <= 50 -> "Good"
                    currentVal <= 100 -> "Moderate"
                    currentVal <= 150 -> "Sensitive"
                    currentVal <= 200 -> "Unhealthy"
                    currentVal <= 300 -> "Very Unhealthy"
                    else -> "Hazardous"
                }
            }

            AqiData(
                isEuropeanStandard = isEurope,
                standardName = standardName,
                currentValue = currentVal,
                levelLabel = levelLabel,
                highestPast7Days = high,
                lowestPast7Days = low
            )
        } else null

        return AetherWeatherState(
            location = location,
            current = currentWeather,
            hourly = hourlyList,
            daily = dailyList,
            aqi = aqiData,
            isCached = isCached
        )
    }
}
