package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyItem
import com.example.ui.util.WeatherFormatters

@Composable
fun DailyForecastCard(
    dailyList: List<DailyItem>,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    val globalMin = dailyList.minOf { it.tempMinC }
    val globalMax = dailyList.maxOf { it.tempMaxC }
    val rangeSpan = (globalMax - globalMin).coerceAtLeast(1.0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "7-Day Forecast",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "7-DAY OUTLOOK",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            dailyList.forEachIndexed { index, item ->
                DailyForecastRow(
                    item = item,
                    globalMin = globalMin,
                    globalMax = globalMax,
                    rangeSpan = rangeSpan,
                    isFahrenheit = isFahrenheit
                )

                if (index < dailyList.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 0.8.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    item: DailyItem,
    globalMin: Double,
    globalMax: Double,
    rangeSpan: Double,
    isFahrenheit: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Day Label
        Text(
            text = item.dayLabel,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (item.dayLabel == "Today") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.widthIn(min = 84.dp),
            maxLines = 1,
            softWrap = false
        )

        // Weather Icon
        Icon(
            imageVector = item.condition.getIcon(),
            contentDescription = item.condition.displayName,
            tint = Color(0xFFFFD54F),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Low Temp
        Text(
            text = WeatherFormatters.formatTemp(item.tempMinC, isFahrenheit),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp)
        )

        // Visual Range Bar
        val startFrac = ((item.tempMinC - globalMin) / rangeSpan).toFloat().coerceIn(0f, 1f)
        val endFrac = ((item.tempMaxC - globalMin) / rangeSpan).toFloat().coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (startFrac > 0f) {
                    Spacer(modifier = Modifier.weight(startFrac.coerceAtLeast(0.001f)))
                }
                Box(
                    modifier = Modifier
                        .weight((endFrac - startFrac).coerceAtLeast(0.05f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF64B5F6), Color(0xFFFFB74D))
                            )
                        )
                )
                if ((1f - endFrac) > 0f) {
                    Spacer(modifier = Modifier.weight((1f - endFrac).coerceAtLeast(0.001f)))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // High Temp
        Text(
            text = WeatherFormatters.formatTemp(item.tempMaxC, isFahrenheit),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
    }
}
