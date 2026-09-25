package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyItem
import com.example.ui.util.WeatherFormatters
import kotlin.math.roundToInt

/**
 * Hourly Temperature Trend Chart
 * Provides a fluid, Recharts-inspired smooth cubic bezier trend line
 * visualizing the temperature curve across the next 24 hours.
 */
@Composable
fun HourlyTemperatureTrendCard(
    hourlyList: List<HourlyItem>,
    isFahrenheit: Boolean,
    selectedIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    val items = hourlyList.take(24)
    val temps = items.map { it.tempC }
    val minTemp = temps.minOrNull() ?: 20.0
    val maxTemp = temps.maxOrNull() ?: 30.0
    val tempRange = (maxTemp - minTemp).coerceAtLeast(1.0)

    val minTempFormatted = WeatherFormatters.formatTemp(minTemp, isFahrenheit)
    val maxTempFormatted = WeatherFormatters.formatTemp(maxTemp, isFahrenheit)

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(16.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD54F).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TEMPERATURE TREND",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                // High / Low summary badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Low: $minTempFormatted",
                        color = Color(0xFF81D4FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "High: $maxTempFormatted",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable chart canvas spanning 24 hourly intervals with smooth cubic curves
            val pointWidthDp = 52.dp
            val totalWidthDp = pointWidthDp * items.size
            val chartHeightDp = 130.dp

            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .height(chartHeightDp)
                ) {
                    val widthPx = size.width
                    val heightPx = size.height

                    val topPadding = 24.dp.toPx()
                    val bottomPadding = 32.dp.toPx()
                    val usableHeight = heightPx - topPadding - bottomPadding

                    val stepX = widthPx / items.size

                    // Compute points
                    val points = items.mapIndexed { index, item ->
                        val x = index * stepX + (stepX / 2f)
                        val fraction = (item.tempC - minTemp) / tempRange
                        // Invert Y so high temps are near the top
                        val y = topPadding + (1f - fraction.toFloat()) * usableHeight
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        // Build smooth cubic bezier curve
                        val curvePath = Path()
                        val fillPath = Path()

                        curvePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, heightPx - bottomPadding + 10f)
                        fillPath.lineTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]

                            val cx = (p0.x + p1.x) / 2f
                            curvePath.cubicTo(
                                cx, p0.y,
                                cx, p1.y,
                                p1.x, p1.y
                            )
                            fillPath.cubicTo(
                                cx, p0.y,
                                cx, p1.y,
                                p1.x, p1.y
                            )
                        }

                        fillPath.lineTo(points.last().x, heightPx - bottomPadding + 10f)
                        fillPath.close()

                        // 1. Draw gradient area underneath the curve (Recharts style)
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.28f),
                                    Color(0xFF64B5F6).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = heightPx - bottomPadding + 10f
                            )
                        )

                        // 2. Draw smooth stroke curve
                        drawPath(
                            path = curvePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F),
                                    Color(0xFFFFB74D),
                                    Color(0xFF81D4FA),
                                    Color(0xFF64B5F6)
                                )
                            ),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 3. Draw points, temperature labels and hour labels
                    points.forEachIndexed { index, pt ->
                        val item = items[index]
                        val isSelected = selectedIndex == index
                        val tempVal = WeatherFormatters.formatTemp(item.tempC, isFahrenheit)

                        // Glow circle if selected
                        if (isSelected) {
                            drawCircle(
                                color = Color(0xFFFFD54F).copy(alpha = 0.35f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                        }

                        // Outer ring & dot
                        drawCircle(
                            color = if (isSelected) Color(0xFFFFD54F) else Color(0xFF1E2638),
                            radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = if (isSelected) Color.White else Color(0xFFFFD54F),
                            radius = if (isSelected) 3.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )

                        // Temperature text above the point
                        val tempMeasured = textMeasurer.measure(
                            text = tempVal,
                            style = TextStyle(
                                color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        drawText(
                            textLayoutResult = tempMeasured,
                            topLeft = Offset(
                                pt.x - (tempMeasured.size.width / 2f),
                                pt.y - tempMeasured.size.height - 4.dp.toPx()
                            )
                        )

                        // Hour time label below
                        val timeMeasured = textMeasurer.measure(
                            text = item.timeLabel,
                            style = TextStyle(
                                color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                        drawText(
                            textLayoutResult = timeMeasured,
                            topLeft = Offset(
                                pt.x - (timeMeasured.size.width / 2f),
                                heightPx - bottomPadding + 8.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}
