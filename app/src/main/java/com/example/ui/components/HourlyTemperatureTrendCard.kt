package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyItem
import com.example.ui.util.WeatherFormatters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class SampledHourPoint(
    val item: HourlyItem,
    val originalIndex: Int
)

/**
 * Redesigned Hourly Temperature & Rain Trend Card
 * Matches the minimalist demo design:
 * - 3-hour fixed overview across the day (no horizontal scrolling required)
 * - Smooth cubic temperature bezier curve with floating temperature values
 * - Dynamic atmospheric vertical gradient beneath the curve (terracotta/amber for warm, coral for mild, cyan/teal for cool)
 * - Soft day/night horizon shading
 * - Rain probability details row with water drop icons and rain intensity highlights
 * - Crisp horizontal baseline with upward tick marks
 * - Uniform time labels (11 PM, 2 AM, 5 AM, etc.)
 * - Interactive touch scrubbing synced with Aether's 24-hour diorama and tactile haptics
 * - Dual metric toggle: Precipitation Chance (%) vs Volume (mm/in)
 */
@Composable
fun HourlyTemperatureTrendCard(
    hourlyList: List<HourlyItem>,
    isFahrenheit: Boolean,
    selectedIndex: Int? = null,
    onHourSelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    // Sample 8 points at 3-hour intervals spanning the 24-hour forecast
    val sampledPoints = remember(hourlyList) {
        val total = hourlyList.size
        val list = mutableListOf<SampledHourPoint>()
        if (total >= 22) {
            val stepIndices = listOf(0, 3, 6, 9, 12, 15, 18, 21)
            for (idx in stepIndices) {
                if (idx < total) {
                    list.add(SampledHourPoint(hourlyList[idx], idx))
                }
            }
        } else {
            val count = minOf(8, total)
            val step = (total / count.toFloat()).coerceAtLeast(1f)
            for (i in 0 until count) {
                val idx = (i * step).roundToInt().coerceIn(0, total - 1)
                if (list.none { it.originalIndex == idx }) {
                    list.add(SampledHourPoint(hourlyList[idx], idx))
                }
            }
        }
        list
    }

    if (sampledPoints.isEmpty()) return

    // Temperature bounds calculation
    val temps = sampledPoints.map { it.item.tempC }
    val minTemp = temps.minOrNull() ?: 20.0
    val maxTemp = temps.maxOrNull() ?: 30.0
    val avgTemp = if (temps.isNotEmpty()) temps.average() else 25.0
    val tempRange = (maxTemp - minTemp).coerceAtLeast(2.0)

    val minTempFormatted = WeatherFormatters.formatTemp(minTemp, isFahrenheit)
    val maxTempFormatted = WeatherFormatters.formatTemp(maxTemp, isFahrenheit)

    // Metric toggle state: Precipitation Probability (%) vs Volume (mm/in)
    var showVolumeMetric by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141A28).copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
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
                        text = "TEMPERATURE & RAIN TREND",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }

                // Dual Metric Toggle Pill (% Chance vs Volume)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showVolumeMetric = !showVolumeMetric }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showVolumeMetric) if (isFahrenheit) "in" else "mm" else "% Chance",
                            color = Color(0xFF81D4FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-header summary (Low / High)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Low: $minTempFormatted",
                    color = Color(0xFF81D4FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "  •  ",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Graph Canvas with Touch / Drag Gestures
            val nPoints = sampledPoints.size

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(sampledPoints) {
                        detectTapGestures { offset ->
                            val x = offset.x
                            val widthPx = size.width.toFloat()
                            val hPadding = 20.dp.toPx()
                            val usableWidth = (widthPx - 2 * hPadding).coerceAtLeast(1f)
                            val stepX = usableWidth / (nPoints - 1).coerceAtLeast(1)

                            var closestIndex = 0
                            var minDiff = Float.MAX_VALUE
                            for (i in 0 until nPoints) {
                                val ptX = hPadding + i * stepX
                                val diff = abs(ptX - x)
                                if (diff < minDiff) {
                                    minDiff = diff
                                    closestIndex = i
                                }
                            }
                            onHourSelected?.invoke(sampledPoints[closestIndex].originalIndex)
                        }
                    }
                    .pointerInput(sampledPoints) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val x = change.position.x
                            val widthPx = size.width.toFloat()
                            val hPadding = 20.dp.toPx()
                            val usableWidth = (widthPx - 2 * hPadding).coerceAtLeast(1f)
                            val stepX = usableWidth / (nPoints - 1).coerceAtLeast(1)

                            var closestIndex = 0
                            var minDiff = Float.MAX_VALUE
                            for (i in 0 until nPoints) {
                                val ptX = hPadding + i * stepX
                                val diff = abs(ptX - x)
                                if (diff < minDiff) {
                                    minDiff = diff
                                    closestIndex = i
                                }
                            }
                            onHourSelected?.invoke(sampledPoints[closestIndex].originalIndex)
                        }
                    }
            ) {
                val widthPx = size.width
                val heightPx = size.height

                val hPadding = 20.dp.toPx()
                val topPadding = 28.dp.toPx()
                val baselineY = heightPx - 26.dp.toPx()
                val rainRowY = baselineY - 14.dp.toPx()
                val curveBottomY = rainRowY - 14.dp.toPx()
                val usableCurveHeight = (curveBottomY - topPadding).coerceAtLeast(10f)

                val usableWidth = widthPx - 2 * hPadding
                val stepX = if (nPoints > 1) usableWidth / (nPoints - 1) else usableWidth

                // 1. Compute 2D points along the curve
                val points = sampledPoints.mapIndexed { index, sample ->
                    val x = hPadding + index * stepX
                    val fraction = ((sample.item.tempC - minTemp) / tempRange).coerceIn(0.0, 1.0)
                    val y = curveBottomY - (fraction.toFloat() * usableCurveHeight)
                    Offset(x, y)
                }

                // 2. Soft Day/Night horizon shading in background
                sampledPoints.forEachIndexed { index, sample ->
                    val x = points[index].x
                    val colWidth = stepX
                    val left = x - colWidth / 2f
                    val right = x + colWidth / 2f
                    val isNight = !sample.item.isDay

                    val shadeColor = if (isNight) {
                        Color(0xFF0F172A).copy(alpha = 0.35f)
                    } else {
                        Color(0xFFF59E0B).copy(alpha = 0.04f)
                    }
                    drawRoundRect(
                        color = shadeColor,
                        topLeft = Offset(left.coerceAtLeast(0f), 0f),
                        size = Size((right - left).coerceAtLeast(0f), baselineY),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // 3. Dynamic atmospheric gradient selection based on temperature
                val (fillBrush, strokeBrush) = when {
                    avgTemp >= 26.0 -> {
                        // Warm / Hot: Terracotta / amber / rust glow (matches demo screenshot)
                        Pair(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE57350).copy(alpha = 0.45f),
                                    Color(0xFFC75D38).copy(alpha = 0.22f),
                                    Color(0xFF381F1A).copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = baselineY
                            ),
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFB299),
                                    Color(0xFFF97316),
                                    Color(0xFFE57350),
                                    Color(0xFFD85A38)
                                )
                            )
                        )
                    }
                    avgTemp in 17.0..25.9 -> {
                        // Mild / Pleasant: Coral / Golden Amber glow
                        Pair(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFB74D).copy(alpha = 0.40f),
                                    Color(0xFFF57C00).copy(alpha = 0.18f),
                                    Color(0xFF281D1A).copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = baselineY
                            ),
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFE082),
                                    Color(0xFFFFB74D),
                                    Color(0xFFFFA726)
                                )
                            )
                        )
                    }
                    else -> {
                        // Cool / Cold: Crisp cyan / Teal / Ocean glow
                        Pair(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4FC3F7).copy(alpha = 0.42f),
                                    Color(0xFF0288D1).copy(alpha = 0.20f),
                                    Color(0xFF0C243B).copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = baselineY
                            ),
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF81D4FA),
                                    Color(0xFF29B6F6),
                                    Color(0xFF0288D1)
                                )
                            )
                        )
                    }
                }

                // 4. Construct smooth cubic bezier curve & fill path
                if (points.size >= 2) {
                    val curvePath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val dx = (p1.x - p0.x) * 0.5f
                            cubicTo(
                                p0.x + dx, p0.y,
                                p1.x - dx, p1.y,
                                p1.x, p1.y
                            )
                        }
                    }

                    val fillPath = Path().apply {
                        moveTo(points.first().x, baselineY)
                        lineTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val dx = (p1.x - p0.x) * 0.5f
                            cubicTo(
                                p0.x + dx, p0.y,
                                p1.x - dx, p1.y,
                                p1.x, p1.y
                            )
                        }
                        lineTo(points.last().x, baselineY)
                        close()
                    }

                    // Draw gradient atmospheric fill
                    drawPath(
                        path = fillPath,
                        brush = fillBrush
                    )

                    // Draw smooth stroke curve (no dots, matching demo design)
                    drawPath(
                        path = curvePath,
                        brush = strokeBrush,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // 5. Draw horizontal baseline axis
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(hPadding - 8.dp.toPx(), baselineY),
                    end = Offset(widthPx - hPadding + 8.dp.toPx(), baselineY),
                    strokeWidth = 1.dp.toPx()
                )

                // 6. Draw elements at each sample point
                points.forEachIndexed { index, pt ->
                    val sample = sampledPoints[index]
                    val item = sample.item
                    val isSelected = selectedIndex == sample.originalIndex

                    // --- Upward tick marks at each point ---
                    drawLine(
                        color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.45f),
                        start = Offset(pt.x, baselineY),
                        end = Offset(pt.x, baselineY - 5.dp.toPx()),
                        strokeWidth = if (isSelected) 1.8.dp.toPx() else 1.2.dp.toPx()
                    )

                    // --- Floating Temperature Label above curve ---
                    val tempLabel = WeatherFormatters.formatTemp(item.tempC, isFahrenheit)
                    val tempMeasured = textMeasurer.measure(
                        text = tempLabel,
                        style = TextStyle(
                            color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.95f),
                            fontSize = 11.sp,
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

                    // --- Selected State Highlight ---
                    if (isSelected) {
                        // Subtle vertical dashed accent guide
                        drawLine(
                            color = Color(0xFFFFD54F).copy(alpha = 0.5f),
                            start = Offset(pt.x, topPadding - 4.dp.toPx()),
                            end = Offset(pt.x, baselineY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Glowing accent circle on the curve
                        drawCircle(
                            color = Color(0xFFFFD54F).copy(alpha = 0.35f),
                            radius = 8.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }

                    // --- Rain Details Row (💧 24%) ---
                    val prob = item.precipProb
                    val isHeavyRain = prob >= 50 || item.condition.isRain
                    val rainColor = if (isHeavyRain) Color(0xFF00E5FF) else Color(0xFF29B6F6)

                    val rainText = if (showVolumeMetric) {
                        val estVolume = (prob / 100.0) * (if (item.condition.rainIntensity > 0f) item.condition.rainIntensity * 6.0 else 2.5)
                        if (isFahrenheit) {
                            String.format(Locale.US, "%.1f in", estVolume * 0.0393701)
                        } else {
                            String.format(Locale.US, "%.1f mm", estVolume)
                        }
                    } else {
                        "$prob%"
                    }

                    val rainMeasured = textMeasurer.measure(
                        text = rainText,
                        style = TextStyle(
                            color = rainColor,
                            fontSize = 10.sp,
                            fontWeight = if (isHeavyRain) FontWeight.Bold else FontWeight.Medium
                        )
                    )

                    val dropSizePx = 7.dp.toPx()
                    val totalRainWidth = dropSizePx + 3.dp.toPx() + rainMeasured.size.width
                    val dropCenterX = pt.x - (totalRainWidth / 2f) + (dropSizePx / 2f)
                    val dropCenterY = rainRowY

                    // Draw tiny crisp vector waterdrop icon
                    drawWaterDrop(
                        center = Offset(dropCenterX, dropCenterY),
                        sizePx = dropSizePx,
                        color = rainColor,
                        hasGlow = isHeavyRain
                    )

                    // Draw rain percentage text
                    drawText(
                        textLayoutResult = rainMeasured,
                        topLeft = Offset(
                            dropCenterX + (dropSizePx / 2f) + 3.dp.toPx(),
                            dropCenterY - (rainMeasured.size.height / 2f)
                        )
                    )

                    // --- Time Axis Label below baseline (11 PM, 2 AM, 5 AM, etc.) ---
                    val timeLabel = formatHourLabel(item)
                    val timeMeasured = textMeasurer.measure(
                        text = timeLabel,
                        style = TextStyle(
                            color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.70f),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    drawText(
                        textLayoutResult = timeMeasured,
                        topLeft = Offset(
                            pt.x - (timeMeasured.size.width / 2f),
                            baselineY + 6.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

/**
 * Draws a sharp, pixel-perfect waterdrop shape at the specified center.
 */
private fun DrawScope.drawWaterDrop(
    center: Offset,
    sizePx: Float,
    color: Color,
    hasGlow: Boolean = false
) {
    if (hasGlow) {
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = sizePx * 0.9f,
            center = center
        )
    }

    val dropPath = Path().apply {
        moveTo(center.x, center.y - sizePx * 0.55f)
        cubicTo(
            center.x + sizePx * 0.45f, center.y,
            center.x + sizePx * 0.45f, center.y + sizePx * 0.5f,
            center.x, center.y + sizePx * 0.55f
        )
        cubicTo(
            center.x - sizePx * 0.45f, center.y + sizePx * 0.5f,
            center.x - sizePx * 0.45f, center.y,
            center.x, center.y - sizePx * 0.55f
        )
        close()
    }
    drawPath(dropPath, color = color)
}

/**
 * Formats hour of day into clean 12-hour AM/PM label (e.g., 11 PM, 2 AM, 5 AM).
 */
private fun formatHourLabel(item: HourlyItem): String {
    val h = item.hourOfDay
    val hour12 = when (h) {
        0 -> 12
        in 1..12 -> h
        else -> h - 12
    }
    val amPm = if (h < 12) "AM" else "PM"
    return "$hour12 $amPm"
}
