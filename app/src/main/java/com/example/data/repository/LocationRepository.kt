package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.local.AetherDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.LocationItem
import com.example.data.model.TerrainCategory
import com.example.data.network.NetworkModule
import com.example.data.network.OpenMeteoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface responsible for location resolution:
 * - Setting Kolkata as the default showcase location
 * - Checking and requesting user GPS location when permissions are available
 * - Managing saved location slots, recent search history, and geocoding search
 */
interface LocationRepository {

    /**
     * Default location for the application: Kolkata, West Bengal, India.
     */
    fun getDefaultLocation(): LocationItem

    /**
     * Checks whether fine or coarse location runtime permission has been granted.
     */
    fun isLocationPermissionGranted(): Boolean

    /**
     * Requests user's current GPS location if permissions are available.
     * Returns null if permissions are absent or location fix fails.
     */
    suspend fun getCurrentLocation(): LocationItem?

    /**
     * Returns the initial location:
     * Kolkata is set as the default location, transitioning to GPS if permissions are already available.
     */
    suspend fun getInitialLocation(): LocationItem

    /**
     * Curated global destinations spanning miniature diorama terrain categories.
     */
    fun getCuratedDestinations(): List<LocationItem>

    // Persistence & Search
    val savedLocations: Flow<List<SavedLocationEntity>>
    val recentSearches: Flow<List<SearchHistoryEntity>>
    suspend fun searchLocations(query: String): List<LocationItem>
    suspend fun saveLocationSlot(slot: String, location: LocationItem)
    suspend fun removeLocationSlot(slot: String)
    suspend fun addSearchHistory(location: LocationItem)
    suspend fun clearSearchHistory()
}

/**
 * Default implementation of [LocationRepository].
 */
class DefaultLocationRepository(
    private val context: Context,
    private val database: AetherDatabase,
    private val locationHelper: LocationHelper = LocationHelper(context),
    private val api: OpenMeteoApi = NetworkModule.openMeteoApi
) : LocationRepository {

    override fun getDefaultLocation(): LocationItem = LocationHelper.DEFAULT_LOCATION

    override fun isLocationPermissionGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override suspend fun getCurrentLocation(): LocationItem? {
        if (!isLocationPermissionGranted()) {
            return null
        }
        return locationHelper.getCurrentLocation()
    }

    override suspend fun getInitialLocation(): LocationItem {
        return if (isLocationPermissionGranted()) {
            getCurrentLocation() ?: getDefaultLocation()
        } else {
            getDefaultLocation()
        }
    }

    override fun getCuratedDestinations(): List<LocationItem> = LocationHelper.CURATED_DESTINATIONS

    override val savedLocations: Flow<List<SavedLocationEntity>> =
        database.locationDao().getAllSavedLocations()

    override val recentSearches: Flow<List<SearchHistoryEntity>> =
        database.locationDao().getRecentSearches()

    override suspend fun searchLocations(query: String): List<LocationItem> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val response = api.searchLocations(name = query.trim())
            response.results?.map { res ->
                val terrain = TerrainCategory.resolveTerrain(
                    name = res.name,
                    country = res.country,
                    admin1 = res.admin1,
                    latitude = res.latitude,
                    longitude = res.longitude,
                    elevation = res.elevation,
                    population = res.population
                )
                LocationItem(
                    id = res.id,
                    name = res.name,
                    admin1 = res.admin1,
                    country = res.country,
                    latitude = res.latitude,
                    longitude = res.longitude,
                    terrainCategory = terrain,
                    elevation = res.elevation
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveLocationSlot(slot: String, location: LocationItem) = withContext(Dispatchers.IO) {
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

    override suspend fun removeLocationSlot(slot: String) = withContext(Dispatchers.IO) {
        database.locationDao().deleteSavedLocation(slot)
    }

    override suspend fun addSearchHistory(location: LocationItem) = withContext(Dispatchers.IO) {
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

    override suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        database.locationDao().clearSearchHistory()
    }
}
