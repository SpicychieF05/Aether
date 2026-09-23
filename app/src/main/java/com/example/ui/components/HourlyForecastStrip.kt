package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
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
import com.example.data.model.HourlyItem
import com.example.ui.util.WeatherFormatters

@Composable
fun HourlyForecastStrip(
    hourlyList: List<HourlyItem>,
    selectedIndex: Int?,
    isFahrenheit: Boolean,
    onHourSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Hourly Forecast",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.padding(3.dp))
                    Text(
                        text = "24-HOUR FORECAST • TAP TO SCRUB",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (selectedIndex != null) {
                    Text(
                        text = "Reset",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onHourSelected(null) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(hourlyList) { index, item ->
                    val isSelected = selectedIndex == index
                    val isNow = index == 0 && selectedIndex == null

                    val itemBg = when {
                        isSelected -> Color(0xFFFFD54F).copy(alpha = 0.35f)
                        isNow -> Color.White.copy(alpha = 0.16f)
                        else -> Color.Transparent
                    }

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(itemBg)
                            .clickable {
                                if (index == 0 && selectedIndex == null) {
                                    onHourSelected(null)
                                } else if (isSelected) {
                                    onHourSelected(null)
                                } else {
                                    onHourSelected(index)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.timeLabel,
                            color = if (isSelected) Color(0xFFFFE082) else Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Icon(
                            imageVector = item.condition.getIcon(),
                            contentDescription = item.condition.displayName,
                            tint = if (item.isDay) Color(0xFFFFD54F) else Color(0xFF90CAF9),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = WeatherFormatters.formatTemp(item.tempC, isFahrenheit),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (item.precipProb > 15) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.precipProb}%",
                                color = Color(0xFF64B5F6),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
