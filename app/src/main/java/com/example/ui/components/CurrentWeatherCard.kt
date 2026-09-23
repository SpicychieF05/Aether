package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scrubber preview indicator if active
        if (scrubbedHour != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD54F).copy(alpha = 0.25f))
                    .clickable(onClick = onResetScrubber)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main Large Temperature
        Text(
            text = WeatherFormatters.formatTemp(displayedTemp, isFahrenheit),
            color = Color.White,
            fontSize = 76.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp
        )

        // Condition Title & Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = displayedCondition.getIcon(),
                contentDescription = displayedCondition.displayName,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayedCondition.displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Feels like + High/Low summary
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Feels like ${WeatherFormatters.formatTemp(feelsLike, isFahrenheit)}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            if (todayForecast != null) {
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Text(
                    text = "H: ${WeatherFormatters.formatTemp(todayForecast.tempMaxC, isFahrenheit)}  L: ${WeatherFormatters.formatTemp(todayForecast.tempMinC, isFahrenheit)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
