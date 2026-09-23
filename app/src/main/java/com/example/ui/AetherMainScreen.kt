package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LocationHelper
import com.example.ui.components.AetherTopBar
import com.example.ui.components.AmbianceOverlay
import com.example.ui.components.CurrentWeatherCard
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.HourlyForecastStrip
import com.example.ui.components.LocationBottomSheet
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.StatChipsRow
import com.example.ui.diorama.MiniatureDioramaView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherMainScreen(
    viewModel: AetherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet visibility states
    var showLocationSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val locationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Runtime location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.requestGpsLocation()
        }
    }

    // Ask for location permissions on first launch after displaying default Kolkata
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Display error messages gracefully in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val weather = uiState.weatherState
    val scrubbedHour = uiState.scrubbedHourIndex?.let { index ->
        weather?.hourly?.getOrNull(index)
    }

    // Active condition & isDay (either live or scrubbed)
    val activeCondition = scrubbedHour?.condition ?: weather?.current?.condition ?: LocationHelper.DEFAULT_LOCATION.terrainCategory.let {
        com.example.data.model.WeatherCondition(
            code = 0,
            displayName = "Clear Sky",
            effectType = com.example.data.model.WeatherEffectType.CLEAR,
            isDay = true
        )
    }
    val activeIsDay = scrubbedHour?.isDay ?: weather?.current?.isDay ?: true
    val activeTerrain = weather?.location?.terrainCategory ?: LocationHelper.DEFAULT_LOCATION.terrainCategory

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0A0F1D),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // -------------------------------------------------------------
            // LIVING MINIATURE DIORAMA BACKGROUND
            // -------------------------------------------------------------
            MiniatureDioramaView(
                terrain = activeTerrain,
                condition = activeCondition,
                isDay = activeIsDay,
                onBackgroundClick = {
                    viewModel.toggleZenMode()
                },
                onThunderclap = {
                    viewModel.triggerThunderHaptic()
                },
                modifier = Modifier.fillMaxSize()
            )

            // -------------------------------------------------------------
            // FOREGROUND INTERACTION LAYER
            // -------------------------------------------------------------
            if (uiState.isZenMode && weather != null) {
                // Zen / Ambiance Mode: Forecast cards faded away for calm diorama view
                AmbianceOverlay(
                    location = weather.location,
                    currentWeather = weather.current,
                    isFahrenheit = uiState.isFahrenheit,
                    onExitZen = { viewModel.toggleZenMode() }
                )
            } else {
                // Standard Forecast View
                AnimatedVisibility(
                    visible = !uiState.isZenMode,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 80 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 80 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Top Navigation & Location Bar
                        AetherTopBar(
                            location = weather?.location,
                            onLocationClick = { showLocationSheet = true },
                            onSearchClick = { showLocationSheet = true },
                            onSettingsClick = { showSettingsSheet = true }
                        )

                        if (uiState.isLoading && weather == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFFFD54F))
                            }
                        } else if (weather != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Main Hero Current Weather
                                CurrentWeatherCard(
                                    location = weather.location,
                                    currentWeather = weather.current,
                                    todayForecast = weather.daily.firstOrNull(),
                                    scrubbedHour = scrubbedHour,
                                    isFahrenheit = uiState.isFahrenheit,
                                    onResetScrubber = { viewModel.setScrubbedHour(null) }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Atmospheric Stat Chips (Humidity, AQI, Sun Event, Conditional Fog Visibility)
                                StatChipsRow(
                                    currentWeather = weather.current,
                                    aqiData = weather.aqi,
                                    todayForecast = weather.daily.firstOrNull(),
                                    isFahrenheit = uiState.isFahrenheit
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 24-Hour Forecast & Time-Scrubber
                                HourlyForecastStrip(
                                    hourlyList = weather.hourly,
                                    selectedIndex = uiState.scrubbedHourIndex,
                                    isFahrenheit = uiState.isFahrenheit,
                                    onHourSelected = { viewModel.setScrubbedHour(it) }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 7-Day Forecast Card
                                DailyForecastCard(
                                    dailyList = weather.daily,
                                    isFahrenheit = uiState.isFahrenheit
                                )

                                // Offline Cache Indicator
                                if (weather.isCached) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Offline Mode • Showing cached forecast",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(36.dp))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // MODAL BOTTOM SHEETS
            // -------------------------------------------------------------
            if (showLocationSheet) {
                LocationBottomSheet(
                    sheetState = locationSheetState,
                    currentLocation = weather?.location,
                    searchQuery = uiState.searchQuery,
                    isSearching = uiState.isSearching,
                    searchResults = uiState.searchResults,
                    savedLocations = uiState.savedLocations,
                    recentSearches = uiState.recentSearches,
                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onLocationSelected = { loc ->
                        viewModel.selectLocation(loc)
                    },
                    onRequestGps = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        viewModel.requestGpsLocation()
                    },
                    onSaveCurrentAsSlot = { slot ->
                        weather?.location?.let { cur ->
                            viewModel.saveLocationSlot(slot, cur)
                        }
                    },
                    onRemoveSlot = { slot -> viewModel.removeLocationSlot(slot) },
                    onClearHistory = { viewModel.clearSearchHistory() },
                    onDismiss = { showLocationSheet = false }
                )
            }

            if (showSettingsSheet) {
                SettingsBottomSheet(
                    sheetState = settingsSheetState,
                    isFahrenheit = uiState.isFahrenheit,
                    thunderHapticsEnabled = uiState.thunderHapticsEnabled,
                    scrubberHapticsEnabled = uiState.scrubberHapticsEnabled,
                    onUnitChanged = { viewModel.setTemperatureUnit(it) },
                    onThunderHapticsChanged = { viewModel.setThunderHaptics(it) },
                    onScrubberHapticsChanged = { viewModel.setScrubberHaptics(it) },
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
    }
}
