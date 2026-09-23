package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.data.model.LocationItem
import com.example.data.model.TerrainCategory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        // Default showcase location: Kolkata (West Bengal, India) as requested
        val DEFAULT_LOCATION = LocationItem(
            id = 1275004L,
            name = "Kolkata",
            admin1 = "West Bengal",
            country = "India",
            latitude = 22.5726,
            longitude = 88.3639,
            terrainCategory = TerrainCategory.COASTAL,
            elevation = 9.0
        )

        val CURATED_DESTINATIONS = listOf(
            DEFAULT_LOCATION,
            LocationItem(
                id = 1850147L,
                name = "Tokyo",
                admin1 = "Tokyo",
                country = "Japan",
                latitude = 35.6895,
                longitude = 139.6917,
                terrainCategory = TerrainCategory.CITY_SKYLINE,
                elevation = 40.0
            ),
            LocationItem(
                id = 1857910L,
                name = "Kyoto",
                admin1 = "Kyoto",
                country = "Japan",
                latitude = 35.0116,
                longitude = 135.7681,
                terrainCategory = TerrainCategory.HILLS_PLAINS,
                elevation = 55.0
            ),
            LocationItem(
                id = 2657928L,
                name = "Zermatt",
                admin1 = "Valais",
                country = "Switzerland",
                latitude = 45.9765,
                longitude = 7.7491,
                terrainCategory = TerrainCategory.ALPINE,
                elevation = 1608.0
            ),
            LocationItem(
                id = 5128581L,
                name = "New York",
                admin1 = "New York",
                country = "United States",
                latitude = 40.7128,
                longitude = -74.0060,
                terrainCategory = TerrainCategory.CITY_SKYLINE,
                elevation = 10.0
            ),
            LocationItem(
                id = 1269515L,
                name = "Jaisalmer",
                admin1 = "Rajasthan",
                country = "India",
                latitude = 26.9157,
                longitude = 70.9083,
                terrainCategory = TerrainCategory.DESERT,
                elevation = 225.0
            ),
            LocationItem(
                id = 259693L,
                name = "Santorini",
                admin1 = "South Aegean",
                country = "Greece",
                latitude = 36.3932,
                longitude = 25.4615,
                terrainCategory = TerrainCategory.COASTAL,
                elevation = 120.0
            ),
            LocationItem(
                id = 1884844L,
                name = "Bali",
                admin1 = "Bali",
                country = "Indonesia",
                latitude = -8.4095,
                longitude = 115.1889,
                terrainCategory = TerrainCategory.TROPICAL,
                elevation = 150.0
            ),
            LocationItem(
                id = 2779836L,
                name = "Hallstatt",
                admin1 = "Upper Austria",
                country = "Austria",
                latitude = 47.5622,
                longitude = 13.6493,
                terrainCategory = TerrainCategory.VILLAGE,
                elevation = 511.0
            )
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationItem? = withContext(Dispatchers.IO) {
        try {
            val cts = CancellationTokenSource()
            val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).addOnSuccessListener { loc ->
                    cont.resume(loc)
                }.addOnFailureListener {
                    cont.resume(null)
                }
            } ?: return@withContext null

            val lat = location.latitude
            val lon = location.longitude

            // Geocode to get city name
            var cityName = "My Location"
            var admin1: String? = null
            var country: String? = null

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val addresses = suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { addrs ->
                            cont.resume(addrs)
                        }
                    }
                    val addr = addresses?.firstOrNull()
                    if (addr != null) {
                        cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                        admin1 = addr.adminArea
                        country = addr.countryName
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val addr = addresses?.firstOrNull()
                    if (addr != null) {
                        cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                        admin1 = addr.adminArea
                        country = addr.countryName
                    }
                }
            } catch (e: Exception) {
                // Fallback to coordinates
            }

            val terrain = TerrainCategory.resolveTerrain(
                name = cityName,
                country = country,
                admin1 = admin1,
                latitude = lat,
                longitude = lon
            )

            LocationItem(
                id = 999999L,
                name = cityName,
                admin1 = admin1,
                country = country,
                latitude = lat,
                longitude = lon,
                terrainCategory = terrain,
                isGps = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
