package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AetherDatabase
import com.example.data.model.TerrainCategory
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherEffectType
import com.example.data.repository.DefaultLocationRepository
import com.example.data.repository.WeatherRepository
import com.example.data.util.NaqiCalculator
import com.example.ui.util.WeatherFormatters
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aether", appName)
    }

    @Test
    fun `location repository sets kolkata as default location`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AetherDatabase.getDatabase(context)
        val locationRepo = DefaultLocationRepository(context, database)

        val defaultLoc = locationRepo.getDefaultLocation()
        assertEquals("Kolkata", defaultLoc.name)
        assertEquals("India", defaultLoc.country)
        assertEquals("West Bengal", defaultLoc.admin1)
        assertEquals(22.5726, defaultLoc.latitude, 0.001)
        assertEquals(88.3639, defaultLoc.longitude, 0.001)

        // Without permissions granted in test environment, initial location defaults to Kolkata
        assertFalse(locationRepo.isLocationPermissionGranted())
        val initialLoc = locationRepo.getInitialLocation()
        assertEquals("Kolkata", initialLoc.name)
    }

    @Test
    fun `verify indian location detection in weather repository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AetherDatabase.getDatabase(context)
        val weatherRepo = WeatherRepository(database)

        // Kolkata (in India)
        assertTrue(weatherRepo.isIndianLocation(22.57, 88.36, "India", "West Bengal"))
        // New Delhi (coordinates in India)
        assertTrue(weatherRepo.isIndianLocation(28.61, 77.20, null, null))
        // Mumbai coordinates
        assertTrue(weatherRepo.isIndianLocation(19.0760, 72.8777, null, "Maharashtra"))
        // Bengaluru coordinates
        assertTrue(weatherRepo.isIndianLocation(12.9716, 77.5946, null, null))
        // Chennai coordinates
        assertTrue(weatherRepo.isIndianLocation(13.0827, 80.2707, null, "Tamil Nadu"))
        // Port Blair (Andaman & Nicobar Islands)
        assertTrue(weatherRepo.isIndianLocation(11.6234, 92.7265, null, null))
        // Srinagar, Kashmir coordinates
        assertTrue(weatherRepo.isIndianLocation(34.0837, 74.7973, null, null))
        // Jaisalmer, Rajasthan
        assertTrue(weatherRepo.isIndianLocation(26.91, 70.90, "IN", "Rajasthan"))

        // Coordinates outside India
        assertFalse(weatherRepo.isIndianLocation(48.85, 2.35, "France", null)) // Paris
        assertFalse(weatherRepo.isIndianLocation(35.67, 139.65, "Japan", null)) // Tokyo
        assertFalse(weatherRepo.isIndianLocation(40.71, -74.00, "United States", "New York")) // New York
        assertFalse(weatherRepo.isIndianLocation(51.50, -0.12, "United Kingdom", "London")) // London
    }

    @Test
    fun `verify CPCB NAQI calculation and breakpoints for India`() {
        // PM2.5 = 20 -> Good (<= 50)
        val subIndexGood = NaqiCalculator.calculatePm25SubIndex(20.0)
        assertEquals(33, subIndexGood)
        assertEquals("Good", NaqiCalculator.getCategoryLabel(subIndexGood))

        // PM2.5 = 45 -> Satisfactory (75)
        val subIndexSat = NaqiCalculator.calculatePm25SubIndex(45.0)
        assertEquals(75, subIndexSat)
        assertEquals("Satisfactory", NaqiCalculator.getCategoryLabel(subIndexSat))

        // PM2.5 = 75 -> Moderate (150)
        val subIndexMod = NaqiCalculator.calculatePm25SubIndex(75.0)
        assertEquals(150, subIndexMod)
        assertEquals("Moderate", NaqiCalculator.getCategoryLabel(subIndexMod))

        // PM2.5 = 105 -> Poor (250)
        val subIndexPoor = NaqiCalculator.calculatePm25SubIndex(105.0)
        assertEquals(250, subIndexPoor)
        assertEquals("Poor", NaqiCalculator.getCategoryLabel(subIndexPoor))

        // PM2.5 = 185 -> Very Poor (350)
        val subIndexVeryPoor = NaqiCalculator.calculatePm25SubIndex(185.0)
        assertEquals(350, subIndexVeryPoor)
        assertEquals("Very Poor", NaqiCalculator.getCategoryLabel(subIndexVeryPoor))

        // PM2.5 = 315 -> Severe (450)
        val subIndexSevere = NaqiCalculator.calculatePm25SubIndex(315.0)
        assertEquals(450, subIndexSevere)
        assertEquals("Severe", NaqiCalculator.getCategoryLabel(subIndexSevere))

        // Overall NAQI takes the maximum of PM2.5 and PM10
        val overallNaqi = NaqiCalculator.calculateNaqi(pm25 = 45.0, pm10 = 175.0)
        // PM2.5 = 75, PM10 = 150 -> max is 150 (Moderate)
        assertEquals(150, overallNaqi)
        assertEquals("Moderate", NaqiCalculator.getCategoryLabel(overallNaqi))
    }

    @Test
    fun `resolve terrain heuristics correctly`() {
        val kolkataTerrain = TerrainCategory.resolveTerrain(
            name = "Kolkata",
            country = "India",
            latitude = 22.57,
            longitude = 88.36
        )
        assertEquals(TerrainCategory.COASTAL, kolkataTerrain)

        val zermattTerrain = TerrainCategory.resolveTerrain(
            name = "Zermatt",
            country = "Switzerland",
            elevation = 1620.0
        )
        assertEquals(TerrainCategory.ALPINE, zermattTerrain)

        val desertTerrain = TerrainCategory.resolveTerrain(
            name = "Jaisalmer",
            country = "India"
        )
        assertEquals(TerrainCategory.DESERT, desertTerrain)
    }

    @Test
    fun `map WMO codes to weather conditions`() {
        val thunder = WeatherCondition.fromWmoCode(95, isDay = true)
        assertEquals(WeatherEffectType.THUNDERSTORM, thunder.effectType)
        assertTrue(thunder.isThunder)

        val rain = WeatherCondition.fromWmoCode(63, isDay = true)
        assertEquals(WeatherEffectType.RAIN, rain.effectType)

        val clear = WeatherCondition.fromWmoCode(0, isDay = false)
        assertEquals(WeatherEffectType.CLEAR, clear.effectType)
    }

    @Test
    fun `verify temperature and visibility formatters`() {
        val celsiusStr = WeatherFormatters.formatTemp(25.4, isFahrenheit = false)
        assertEquals("25°", celsiusStr)

        val fahrStr = WeatherFormatters.formatTemp(25.0, isFahrenheit = true)
        assertEquals("77°", fahrStr)

        val visKm = WeatherFormatters.formatVisibility(2500.0, isFahrenheit = false)
        assertEquals("2.5 km", visKm)
    }
}
