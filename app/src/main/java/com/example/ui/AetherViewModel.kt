package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AetherDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AetherWeatherState
import com.example.data.model.LocationItem
import com.example.data.repository.DefaultLocationRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AetherUiState(
    val weatherState: AetherWeatherState? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isZenMode: Boolean = false,
    val scrubbedHourIndex: Int? = null,
    val isFahrenheit: Boolean = false,
    val thunderHapticsEnabled: Boolean = true,
    val scrubberHapticsEnabled: Boolean = true,
    val savedLocations: List<SavedLocationEntity> = emptyList(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val searchResults: List<LocationItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = ""
)

class AetherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AetherDatabase.getDatabase(application)
    private val weatherRepository = WeatherRepository(database)
    private val locationRepository: LocationRepository = DefaultLocationRepository(application, database)

    private val prefs = application.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        AetherUiState(
            isFahrenheit = prefs.getBoolean("pref_is_fahrenheit", false),
            thunderHapticsEnabled = prefs.getBoolean("pref_thunder_haptics", true),
            scrubberHapticsEnabled = prefs.getBoolean("pref_scrubber_haptics", true)
        )
    )
    val uiState: StateFlow<AetherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Observe saved locations & search history from Room
        viewModelScope.launch {
            locationRepository.savedLocations.collect { saved ->
                _uiState.update { it.copy(savedLocations = saved) }
            }
        }
        viewModelScope.launch {
            locationRepository.recentSearches.collect { history ->
                _uiState.update { it.copy(recentSearches = history) }
            }
        }

        // Set Kolkata as default for everyone on first launch via LocationRepository
        loadWeather(locationRepository.getDefaultLocation())
    }

    fun loadWeather(location: LocationItem, isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && it.weatherState == null,
                    isRefreshing = isRefresh,
                    errorMessage = null,
                    scrubbedHourIndex = null
                )
            }
            val result = weatherRepository.fetchWeather(location)
            result.fold(
                onSuccess = { state ->
                    _uiState.update {
                        it.copy(
                            weatherState = state,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage ?: "Unable to fetch live weather"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        _uiState.value.weatherState?.location?.let {
            loadWeather(it, isRefresh = true)
        }
    }

    fun selectLocation(location: LocationItem) {
        viewModelScope.launch {
            locationRepository.addSearchHistory(location)
            _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
            loadWeather(location)
        }
    }

    fun requestGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val gpsLoc = locationRepository.getCurrentLocation()
            if (gpsLoc != null) {
                selectLocation(gpsLoc)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (!locationRepository.isLocationPermissionGranted()) {
                            "Location permission needed for GPS location"
                        } else {
                            "Current GPS location unavailable"
                        }
                    )
                }
            }
        }
    }

    fun toggleZenMode() {
        _uiState.update { it.copy(isZenMode = !it.isZenMode) }
    }

    fun setScrubbedHour(index: Int?) {
        if (index != _uiState.value.scrubbedHourIndex) {
            if (_uiState.value.scrubberHapticsEnabled && index != null) {
                performTickHaptic()
            }
            _uiState.update { it.copy(scrubbedHourIndex = index) }
        }
    }

    fun setTemperatureUnit(isFahrenheit: Boolean) {
        prefs.edit().putBoolean("pref_is_fahrenheit", isFahrenheit).apply()
        _uiState.update { it.copy(isFahrenheit = isFahrenheit) }
    }

    fun setThunderHaptics(enabled: Boolean) {
        prefs.edit().putBoolean("pref_thunder_haptics", enabled).apply()
        _uiState.update { it.copy(thunderHapticsEnabled = enabled) }
    }

    fun setScrubberHaptics(enabled: Boolean) {
        prefs.edit().putBoolean("pref_scrubber_haptics", enabled).apply()
        _uiState.update { it.copy(scrubberHapticsEnabled = enabled) }
    }

    fun triggerThunderHaptic() {
        if (_uiState.value.thunderHapticsEnabled) {
            performThunderHaptic()
        }
    }

    fun saveLocationSlot(slot: String, location: LocationItem) {
        viewModelScope.launch {
            locationRepository.saveLocationSlot(slot, location)
        }
    }

    fun removeLocationSlot(slot: String) {
        viewModelScope.launch {
            locationRepository.removeLocationSlot(slot)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length >= 2) {
            searchJob = viewModelScope.launch {
                delay(350) // Debounce
                _uiState.update { it.copy(isSearching = true) }
                val results = locationRepository.searchLocations(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            locationRepository.clearSearchHistory()
        }
    }

    private fun performTickHaptic() {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun performThunderHaptic() {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 50, 140)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getVibrator(): Vibrator? {
        val app = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
