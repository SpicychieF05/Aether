package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AqiData
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyItem
import com.example.ui.util.WeatherFormatters
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StatChipsRow(
    currentWeather: CurrentWeather,
    aqiData: AqiData?,
    todayForecast: DailyItem?,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    val sunLabel = formatSunTime(todayForecast?.sunrise, todayForecast?.sunset, currentWeather.isDay)

    val aqiLevelColor = when (aqiData?.levelLabel?.lowercase()) {
        "good" -> Color(0xFF81C784)
        "satisfactory" -> Color(0xFFAED581)
        "moderate", "fair" -> Color(0xFFFFD54F)
        "sensitive" -> Color(0xFFFFB74D)
        "poor" -> Color(0xFFFF8A65)
        "very poor", "unhealthy" -> Color(0xFFE57373)
        "severe", "hazardous", "very unhealthy" -> Color(0xFFEF5350)
        else -> Color(0xFFE57373)
    }

    val isNaqi = aqiData?.isNaqiStandard == true || aqiData?.standardName?.contains("NAQI", ignoreCase = true) == true
    val aqiTitle = if (isNaqi) "NAQI (INDIA)" else (aqiData?.standardName?.uppercase() ?: "AIR QUALITY")
    val aqiSubtext = when {
        aqiData?.stationName != null && aqiData.distanceKm != null -> {
            "${aqiData.stationName} (${aqiData.distanceKm.toInt()} km)"
        }
        aqiData?.stationName != null -> {
            aqiData.stationName
        }
        isNaqi -> {
            "7d: ${aqiData?.lowestPast7Days ?: 0} - ${aqiData?.highestPast7Days ?: 0}"
        }
        else -> {
            "7d: ${aqiData?.lowestPast7Days ?: 0} - ${aqiData?.highestPast7Days ?: 0}"
        }
    }

    // Adaptive layout based on available container width
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            // WIDE / TABLET / EXPANDED: Single row with match-constraint weights (0dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    icon = Icons.Default.WaterDrop,
                    iconTint = Color(0xFF64B5F6),
                    title = "HUMIDITY",
                    value = "${currentWeather.humidity}%",
                    subtext = if (currentWeather.humidity > 70) "Humid" else "Comfortable",
                    modifier = Modifier.weight(1f)
                )

                if (aqiData != null) {
                    StatChip(
                        icon = Icons.Default.Air,
                        iconTint = aqiLevelColor,
                        title = aqiTitle,
                        value = "${aqiData.currentValue} • ${aqiData.levelLabel}",
                        subtext = aqiSubtext,
                        modifier = Modifier.weight(1.25f)
                    )
                }

                StatChip(
                    icon = Icons.Default.WbSunny,
                    iconTint = Color(0xFFFFB300),
                    title = if (currentWeather.isDay) "SUNSET" else "SUNRISE",
                    value = sunLabel,
                    subtext = if (currentWeather.isDay) "Dusk approaching" else "Dawn approaching",
                    modifier = Modifier.weight(1f)
                )

                if (currentWeather.isFoggy) {
                    StatChip(
                        icon = Icons.Default.Visibility,
                        iconTint = Color(0xFFB0BEC5),
                        title = "VISIBILITY",
                        value = WeatherFormatters.formatVisibility(currentWeather.visibilityMeters, isFahrenheit),
                        subtext = "Foggy conditions",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // COMPACT PHONES (e.g. Lava Blaze 5G 360-384dp width):
            // 2-Column Grid with match-constraint weights (0dp) so text never cramps or splits awkwardly
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Humidity & AQI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(
                        icon = Icons.Default.WaterDrop,
                        iconTint = Color(0xFF64B5F6),
                        title = "HUMIDITY",
                        value = "${currentWeather.humidity}%",
                        subtext = if (currentWeather.humidity > 70) "Humid" else "Comfortable",
                        modifier = Modifier.weight(1f)
                    )

                    if (aqiData != null) {
                        StatChip(
                            icon = Icons.Default.Air,
                            iconTint = aqiLevelColor,
                            title = aqiTitle,
                            value = "${aqiData.currentValue} • ${aqiData.levelLabel}",
                            subtext = aqiSubtext,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        StatChip(
                            icon = Icons.Default.WbSunny,
                            iconTint = Color(0xFFFFB300),
                            title = if (currentWeather.isDay) "SUNSET" else "SUNRISE",
                            value = sunLabel,
                            subtext = if (currentWeather.isDay) "Dusk approaching" else "Dawn approaching",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: Sun Event & (Optional) Visibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (aqiData != null) {
                        StatChip(
                            icon = Icons.Default.WbSunny,
                            iconTint = Color(0xFFFFB300),
                            title = if (currentWeather.isDay) "SUNSET" else "SUNRISE",
                            value = sunLabel,
                            subtext = if (currentWeather.isDay) "Dusk approaching" else "Dawn approaching",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (currentWeather.isFoggy) {
                        StatChip(
                            icon = Icons.Default.Visibility,
                            iconTint = Color(0xFFB0BEC5),
                            title = "VISIBILITY",
                            value = WeatherFormatters.formatVisibility(currentWeather.visibilityMeters, isFahrenheit),
                            subtext = "Foggy conditions",
                            modifier = Modifier.weight(1f)
                        )
                    } else if (aqiData == null) {
                        // Empty spacer to keep balance if only 1 item in row 2
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatSunTime(sunriseIso: String?, sunsetIso: String?, isDay: Boolean): String {
    val targetIso = if (isDay) sunsetIso else sunriseIso
    if (targetIso.isNullOrEmpty()) return if (isDay) "6:15 PM" else "5:45 AM"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        val formatter = SimpleDateFormat("h:mm a", Locale.US)
        val parsed = parser.parse(targetIso)
        if (parsed != null) formatter.format(parsed) else targetIso
    } catch (e: Exception) {
        targetIso
    }
}
