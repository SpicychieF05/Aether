package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeather
import com.example.data.model.LocationItem
import com.example.ui.util.WeatherFormatters
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AmbianceOverlay(
    location: LocationItem,
    currentWeather: CurrentWeather,
    isFahrenheit: Boolean,
    onExitZen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("h:mm a", Locale.US).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            currentTimeString = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onExitZen)
            .padding(24.dp)
    ) {
        // Subtle top wordmark
        Text(
            text = "æther",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Serif,
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Minimalist Ambient Corner Widget
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = WeatherFormatters.formatTemp(currentWeather.tempC, isFahrenheit),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 52.sp,
                fontWeight = FontWeight.Thin
            )

            Text(
                text = "${location.name} • ${currentWeather.condition.displayName}",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = currentTimeString,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Ambient Mode hint pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onExitZen)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Show Forecast",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zen Mode • Tap anywhere to restore forecast",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
