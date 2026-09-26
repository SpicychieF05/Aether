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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyItem
import com.example.data.model.HourlyItem
import com.example.data.model.LocationItem
import com.example.ui.util.WeatherFormatters

@Composable
fun CurrentWeatherCard(
    location: LocationItem,
    currentWeather: CurrentWeather,
    todayForecast: DailyItem?,
    scrubbedHour: HourlyItem?,
    isFahrenheit: Boolean,
    onResetScrubber: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedTemp = scrubbedHour?.tempC ?: currentWeather.tempC
    val displayedCondition = scrubbedHour?.condition ?: currentWeather.condition
    val feelsLike = if (scrubbedHour == null) currentWeather.feelsLikeC else displayedTemp
    val localTimeStr = WeatherFormatters.formatLocationTime(
        latitude = location.latitude,
        longitude = location.longitude,
        country = location.country,
        admin1 = location.admin1
    )

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scrubber preview indicator if active, otherwise Local Time badge
        if (scrubbedHour != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD54F).copy(alpha = 0.25f))
                    .clickable {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } catch (_: Exception) {}
                        onResetScrubber()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Previewing ${scrubbedHour.timeLabel} diorama",
                        color = Color(0xFFFFE082),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            // Location Local Time badge: Clean design signature, only the time is shown
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = localTimeStr,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Large Temperature with symmetric degree spacing for pixel-perfect X-axis centering
        val tempFormatted = WeatherFormatters.formatTemp(displayedTemp, isFahrenheit)
        val tempDigits = tempFormatted.removeSuffix("°")
        val hasDegree = tempFormatted.endsWith("°")

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            if (hasDegree) {
                Spacer(modifier = Modifier.width(20.dp))
            }

            Text(
                text = tempDigits,
                color = Color.White,
                fontSize = 82.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            if (hasDegree) {
                Text(
                    text = "°",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .width(20.dp)
                        .padding(top = 4.dp),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Condition Title & Icon - symmetric balancing so the condition text & digits align on both X & Y axes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = displayedCondition.getIcon(),
                contentDescription = displayedCondition.displayName,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayedCondition.displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            // Symmetric 22dp spacer on the right balances the 22dp icon on the left for exact vertical axis alignment
            Spacer(modifier = Modifier.width(22.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Feels like + High/Low summary with balanced spacing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Feels like ${WeatherFormatters.formatTemp(feelsLike, isFahrenheit)}",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal
            )

            if (todayForecast != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "H: ${WeatherFormatters.formatTemp(todayForecast.tempMaxC, isFahrenheit)}  L: ${WeatherFormatters.formatTemp(todayForecast.tempMinC, isFahrenheit)}",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
