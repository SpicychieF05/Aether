package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AqiData
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyItem
import com.example.ui.util.WeatherFormatters
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatChipsRow(
    currentWeather: CurrentWeather,
    aqiData: AqiData?,
    todayForecast: DailyItem?,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    // Format sunrise/sunset cleanly
    val sunLabel = formatSunTime(todayForecast?.sunrise, todayForecast?.sunset, currentWeather.isDay)

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Humidity Chip
        StatChip(
            icon = Icons.Default.WaterDrop,
            iconTint = Color(0xFF64B5F6),
            title = "HUMIDITY",
            value = "${currentWeather.humidity}%",
            subtext = if (currentWeather.humidity > 70) "Humid" else "Comfortable",
            modifier = Modifier.weight(1f)
        )

        // 2. AQI Chip (Dynamically mentions US AQI or European AQI + 7-day range)
        if (aqiData != null) {
            val aqiLevelColor = when (aqiData.levelLabel.lowercase()) {
                "good" -> Color(0xFF81C784)
                "satisfactory" -> Color(0xFFAED581)
                "moderate", "fair" -> Color(0xFFFFD54F)
                "sensitive" -> Color(0xFFFFB74D)
                "poor" -> Color(0xFFFF8A65)
                "very poor", "unhealthy" -> Color(0xFFE57373)
                "severe", "hazardous", "very unhealthy" -> Color(0xFFEF5350)
                else -> Color(0xFFE57373)
            }

            StatChip(
                icon = Icons.Default.Air,
                iconTint = aqiLevelColor,
                title = aqiData.standardName.uppercase(),
                value = "${aqiData.currentValue} • ${aqiData.levelLabel}",
                subtext = "7d: ${aqiData.lowestPast7Days} - ${aqiData.highestPast7Days}",
                modifier = Modifier.weight(1.4f)
            )
        }

        // 3. Sun Event Chip (Sunrise or Sunset)
        StatChip(
            icon = Icons.Default.WbSunny,
            iconTint = Color(0xFFFFB300),
            title = if (currentWeather.isDay) "SUNSET" else "SUNRISE",
            value = sunLabel,
            subtext = if (currentWeather.isDay) "Dusk approaching" else "Dawn approaching",
            modifier = Modifier.weight(1f)
        )

        // 4. Visibility Chip (Strictly conditional: ONLY visible when foggy/misty!)
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
            .background(Color.Black.copy(alpha = 0.28f))
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
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
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
