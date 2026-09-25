package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.update.AppUpdateInfo
import com.example.ui.UpdateUiState
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.WeatherProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    sheetState: SheetState,
    isFahrenheit: Boolean,
    thunderHapticsEnabled: Boolean,
    scrubberHapticsEnabled: Boolean,
    selectedProvider: WeatherProvider,
    updateState: UpdateUiState = UpdateUiState(),
    onUnitChanged: (Boolean) -> Unit,
    onThunderHapticsChanged: (Boolean) -> Unit,
    onScrubberHapticsChanged: (Boolean) -> Unit,
    onProviderSelected: (WeatherProvider) -> Unit,
    onSaveDefaultProvider: (WeatherProvider) -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onInstallUpdate: (AppUpdateInfo) -> Unit = {},
    onDismiss: () -> Unit
) {
    var savedFeedbackText by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131A2A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "SETTINGS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weather Provider Section (Question 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Weather API Provider",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Choose weather & air quality forecast source",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Provider Choice Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Open-Meteo Choice
                val isOpenMeteoSelected = selectedProvider == WeatherProvider.OPEN_METEO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isOpenMeteoSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = if (isOpenMeteoSelected) 1.5.dp else 1.dp,
                            color = if (isOpenMeteoSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            savedFeedbackText = null
                            onProviderSelected(WeatherProvider.OPEN_METEO)
                        }
                        .padding(12.dp)
                        .testTag("provider_open_meteo")
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Open-Meteo",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isOpenMeteoSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isOpenMeteoSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Free, unlimited rate limits, global open models",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Tomorrow.io Choice
                val isTomorrowSelected = selectedProvider == WeatherProvider.TOMORROW_IO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isTomorrowSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            width = if (isTomorrowSelected) 1.5.dp else 1.dp,
                            color = if (isTomorrowSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            savedFeedbackText = null
                            onProviderSelected(WeatherProvider.TOMORROW_IO)
                        }
                        .padding(12.dp)
                        .testTag("provider_tomorrow_io")
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tomorrow.io",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isTomorrowSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isTomorrowSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hyper-local v4 API, includes auto-fallback",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save as Default Provider Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onSaveDefaultProvider(selectedProvider)
                        savedFeedbackText = "${selectedProvider.displayName} saved as default!"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color(0xFFFFD54F)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_default_provider_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save as Default",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (savedFeedbackText != null) {
                    Text(
                        text = savedFeedbackText!!,
                        color = Color(0xFFAED581),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // Unit Preference Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Temperature Units",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isFahrenheit) "Fahrenheit (°F)" else "Celsius (°C)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "°C",
                        color = if (!isFahrenheit) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isFahrenheit,
                        onCheckedChange = onUnitChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFD54F),
                            checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "°F",
                        color = if (isFahrenheit) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // Tactile Haptics Section
            Text(
                text = "TACTILE HAPTIC ACCENTS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Thunderclap Haptics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Thunderclap Vibrations",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Low-amplitude tactile rumble during lightning flashes",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = thunderHapticsEnabled,
                    onCheckedChange = onThunderHapticsChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFD54F),
                        checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrubber Micro-ticks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Timeline Scrubber Clicks",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Delicate haptic feedback when scrubbing hours",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = scrubberHapticsEnabled,
                    onCheckedChange = onScrubberHapticsChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFD54F),
                        checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.4f)
                    )
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // About Aether
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "æther",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "v${com.example.BuildConfig.VERSION_NAME}",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A minimalist weather visualizer with living miniature diorama backgrounds responsive to real geography and live atmosphere.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Powered by ${selectedProvider.displayName} • CPCB / WAQI Ground Stations • OSM Indian Places",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // In-App Self-Update Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF64B5F6).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "App Updates",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Current: v${com.example.BuildConfig.VERSION_NAME}",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = onCheckForUpdates,
                            enabled = !updateState.isChecking && !updateState.isDownloading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF64B5F6).copy(alpha = 0.2f),
                                contentColor = Color(0xFF64B5F6)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.5f))
                        ) {
                            if (updateState.isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF64B5F6),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Searching...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search for Updates", fontSize = 12.sp)
                            }
                        }
                    }

                    // Update Status / Feedback Message
                    updateState.checkMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = msg,
                                color = if (updateState.updateInfo?.hasUpdate == true) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // If a newer release is detected
                    val updateInfo = updateState.updateInfo
                    if (updateInfo != null && updateInfo.hasUpdate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Update Available",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = updateInfo.latestVersion,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }

                            if (updateInfo.releaseTitle.isNotBlank() && updateInfo.releaseTitle != updateInfo.latestVersion) {
                                Text(
                                    text = updateInfo.releaseTitle,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = updateInfo.releaseNotes.take(180) + if (updateInfo.releaseNotes.length > 180) "..." else "",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onInstallUpdate(updateInfo) },
                            enabled = !updateState.isDownloading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (updateState.installReadyUri != null) Color(0xFF81C784) else Color(0xFF64B5F6),
                                contentColor = if (updateState.installReadyUri != null) Color(0xFF1B5E20) else Color(0xFF0D47A1)
                            )
                        ) {
                            if (updateState.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF0D47A1),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading Update...", fontWeight = FontWeight.Bold)
                            } else if (updateState.installReadyUri != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install ${updateInfo.latestVersion}", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Update to ${updateInfo.latestVersion}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Connect with Developer Button
            val context = LocalContext.current
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chirantanmallick.vercel.app/"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F).copy(alpha = 0.16f),
                    contentColor = Color(0xFFFFD54F)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.45f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connect with the Dev",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Privacy Policy Button
            Button(
                onClick = { showPrivacyDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White.copy(alpha = 0.85f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF81C784)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy Policy",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // Privacy Policy Dialog
            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Privacy Policy", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Aether values your privacy.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Location Access: Used solely to fetch real-time local weather forecast and diorama visualization. Location coordinates are processed ephemerally and are never tracked, stored on any server, or sold to third parties.\n\n" +
                                        "• Local Storage: Search history, saved favorite cities, and cached weather telemetry stay 100% on your device in a secure local Room database.\n\n" +
                                        "• Network Security: All weather requests (Open-Meteo, Tomorrow.io, WAQI) are communicated over encrypted HTTPS connections.\n\n" +
                                        "• Sideloaded Updates: App update checks communicate directly with the open-source GitHub Releases repository without telemetry or user profiling.",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text("Close", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF1E2638),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
