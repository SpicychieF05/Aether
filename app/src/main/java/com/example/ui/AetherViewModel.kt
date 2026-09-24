package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
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
    val selectedProvider: com.example.data.repository.WeatherProvider = com.example.data.repository.WeatherProvider.OPEN_METEO,
    val savedLocations: List<SavedLocationEntity> = emptyList(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val searchResults: List<LocationItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = ""
)

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: com.example.data.update.AppUpdateInfo? = null,
    val checkMessage: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val installReadyUri: android.net.Uri? = null
)

class AetherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AetherDatabase.getDatabase(application)
    private val weatherRepository = WeatherRepository(database)
    private val locationRepository: LocationRepository = DefaultLocationRepository(application, database, weatherRepository)

    private val prefs = application.getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)

    private val savedProviderName = prefs.getString("pref_weather_provider", com.example.data.repository.WeatherProvider.OPEN_METEO.name)
    private val initialProvider = try {
        com.example.data.repository.WeatherProvider.valueOf(savedProviderName ?: com.example.data.repository.WeatherProvider.OPEN_METEO.name)
    } catch (_: Exception) {
        com.example.data.repository.WeatherProvider.OPEN_METEO
    }

    private val _uiState = MutableStateFlow(
        AetherUiState(
            isFahrenheit = prefs.getBoolean("pref_is_fahrenheit", false),
            thunderHapticsEnabled = prefs.getBoolean("pref_thunder_haptics", true),
            scrubberHapticsEnabled = prefs.getBoolean("pref_scrubber_haptics", true),
            selectedProvider = initialProvider
        )
    )
    val uiState: StateFlow<AetherUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

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

        // Automatic background check for updates on startup
        checkForAppUpdates(isManual = false)
    }

    /**
     * Checks for updates from GitHub Releases API.
     * When isManual = false: silently checks, and if a newer version is released,
     * it displays an in-app banner/dialog and triggers a system status bar notification.
     * When isManual = true: displays a feedback message ("Aether is up to date" or new version details).
     */
    fun checkForAppUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _updateState.update { it.copy(isChecking = true, checkMessage = if (isManual) "Checking for updates..." else null) }
            val currentVersion = com.example.BuildConfig.VERSION_NAME
            val result = com.example.data.update.UpdateManager.checkLatestRelease(currentVersion)

            result.fold(
                onSuccess = { info ->
                    _updateState.update {
                        it.copy(
                            isChecking = false,
                            updateInfo = info,
                            checkMessage = if (info.hasUpdate) {
                                "New version ${info.latestVersion} available!"
                            } else if (isManual) {
                                "Aether is up to date (v$currentVersion)"
                            } else null
                        )
                    }
                    // Trigger Android system notification if a newer version is available
                    if (info.hasUpdate) {
                        try {
                            com.example.data.update.UpdateManager.postUpdateNotification(getApplication(), info)
                        } catch (e: Exception) {
                            Log.e("AetherViewModel", "Failed to post notification", e)
                        }
                    }
                },
                onFailure = { error ->
                    _updateState.update {
                        it.copy(
                            isChecking = false,
                            checkMessage = if (isManual) "Unable to check updates: ${error.localizedMessage ?: "Network error"}" else null
                        )
                    }
                }
            )
        }
    }

    fun startApkDownload(context: Context, info: com.example.data.update.AppUpdateInfo) {
        _updateState.update { it.copy(isDownloading = true, checkMessage = "Downloading ${info.apkFileName}...") }
        com.example.data.update.UpdateManager.startDownload(
            context = context,
            apkUrl = info.apkDownloadUrl,
            fileName = info.apkFileName,
            onDownloadStarted = {
                _updateState.update { state -> state.copy(isDownloading = true) }
            },
            onInstallReady = { uri ->
                _updateState.update { state ->
                    state.copy(
                        isDownloading = false,
                        installReadyUri = uri,
                        checkMessage = "Download completed. Tap Install to finish."
                    )
                }
            }
        )
    }

    fun clearUpdateFeedbackMessage() {
        _updateState.update { it.copy(checkMessage = null) }
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
            val currentProvider = _uiState.value.selectedProvider
            val result = weatherRepository.fetchWeather(
                location = location,
                preferredProvider = currentProvider,
                forceRefresh = isRefresh
            )
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
                    // Option A: If Tomorrow.io failed (e.g. rate limit), automatically fallback to Open-Meteo
                    if (currentProvider == com.example.data.repository.WeatherProvider.TOMORROW_IO) {
                        val fallback = weatherRepository.fetchWeather(
                            location = location,
                            preferredProvider = com.example.data.repository.WeatherProvider.OPEN_METEO,
                            forceRefresh = isRefresh
                        )
                        fallback.fold(
                            onSuccess = { fallbackState ->
                                _uiState.update {
                                    it.copy(
                                        weatherState = fallbackState,
                                        isLoading = false,
                                        isRefreshing = false,
                                        errorMessage = "Tomorrow.io limit reached; showing Open-Meteo"
                                    )
                                }
                            },
                            onFailure = {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isRefreshing = false,
                                        errorMessage = error.localizedMessage ?: "Unable to fetch live weather"
                                    )
                                }
                            }
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = error.localizedMessage ?: "Unable to fetch live weather"
                            )
                        }
                    }
                }
            )
        }
    }

    fun setWeatherProvider(provider: com.example.data.repository.WeatherProvider) {
        _uiState.update { it.copy(selectedProvider = provider) }
        _uiState.value.weatherState?.location?.let {
            loadWeather(it, isRefresh = true)
        }
    }

    fun saveDefaultWeatherProvider(provider: com.example.data.repository.WeatherProvider) {
        prefs.edit().putString("pref_weather_provider", provider.name).apply()
        _uiState.update { it.copy(selectedProvider = provider) }
        _uiState.value.weatherState?.location?.let {
            loadWeather(it, isRefresh = true)
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
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Try createPredefined(EFFECT_CLICK) first which is much more widely supported than EFFECT_TICK
                try {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } catch (_: Exception) {
                    try {
                        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                    } catch (_: Exception) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(25)
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } catch (_: Exception) {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (e: Exception) {
            Log.e("AetherViewModel", "performTickHaptic error", e)
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
