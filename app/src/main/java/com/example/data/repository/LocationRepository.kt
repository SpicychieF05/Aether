package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.local.AetherDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.LocationItem
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getDefaultLocation(): LocationItem
    fun isLocationPermissionGranted(): Boolean
    suspend fun getCurrentLocation(): LocationItem?
    suspend fun getInitialLocation(): LocationItem
    fun getCuratedDestinations(): List<LocationItem>

    val savedLocations: Flow<List<SavedLocationEntity>>
    val recentSearches: Flow<List<SearchHistoryEntity>>
    suspend fun searchLocations(query: String): List<LocationItem>
    suspend fun saveLocationSlot(slot: String, location: LocationItem)
    suspend fun removeLocationSlot(slot: String)
    suspend fun addSearchHistory(location: LocationItem)
    suspend fun clearSearchHistory()
}

class DefaultLocationRepository(
    private val context: Context,
    private val database: AetherDatabase,
    private val weatherRepository: WeatherRepository,
    private val locationHelper: LocationHelper = LocationHelper(context)
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

    override suspend fun searchLocations(query: String): List<LocationItem> {
        return weatherRepository.searchLocations(query)
    }

    override suspend fun saveLocationSlot(slot: String, location: LocationItem) {
        weatherRepository.saveLocationSlot(slot, location)
    }

    override suspend fun removeLocationSlot(slot: String) {
        weatherRepository.removeLocationSlot(slot)
    }

    override suspend fun addSearchHistory(location: LocationItem) {
        weatherRepository.addSearchHistory(location)
    }

    override suspend fun clearSearchHistory() {
        weatherRepository.clearSearchHistory()
    }
}
