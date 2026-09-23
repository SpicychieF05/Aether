package com.example.ui.diorama

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherEffectType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Layer 2: Weather FX Overlay
 * Built in Compose Canvas so it scales dynamically with real data.
 */
@Composable
fun WeatherFxOverlay(
    condition: WeatherCondition,
    onThunderclap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_fx_anim")

    // Continuous linear time progress for particles
    val timeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_progress"
    )

    // Slow drifting offset for clouds and fog ribbons
    val driftOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_offset"
    )

    // Sun rays gentle rotation
    val sunRayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_ray_rotation"
    )

    // Thunderstorm state
    var lightningAlpha by remember { mutableFloatStateOf(0f) }
    var thunderDarkening by remember { mutableFloatStateOf(0f) }

    if (condition.isThunder) {
        LaunchedEffect(condition.code) {
            while (true) {
                // Irregular random interval between 4 to 9 seconds
                delay(Random.nextLong(4000, 9000))

                // Lightning flash burst!
                lightningAlpha = 0.95f
                onThunderclap()
                delay(60)
                lightningAlpha = 0.2f
                delay(40)
                lightningAlpha = 0.85f
                delay(120)
                lightningAlpha = 0f

                // Atmospheric rumble darkening
                thunderDarkening = 0.45f
                val steps = 15
                for (s in steps downTo 0) {
                    thunderDarkening = (s.toFloat() / steps) * 0.45f
                    delay(50)
                }
                thunderDarkening = 0f
            }
        }
    }

    // Seeded particle arrays to keep animations consistent across recompositions
    val rainDropOffsets = remember {
        List(80) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }

    val snowflakeOffsets = remember {
        List(55) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (condition.effectType) {
            WeatherEffectType.CLEAR -> {
                if (condition.isDay) {
                    drawSunnyBloom(w, h, sunRayRotation)
                }
            }
            WeatherEffectType.CLOUDY -> {
                drawCloudShadows(w, h, driftOffset)
            }
            WeatherEffectType.RAIN -> {
                drawCloudShadows(w, h, driftOffset, alpha = 0.25f)
                drawRainStreaks(w, h, timeProgress, condition.rainIntensity, rainDropOffsets)
                if (condition.rainIntensity > 0.6f) {
                    drawWetGlassDroplets(w, h, timeProgress)
                }
            }
            WeatherEffectType.THUNDERSTORM -> {
                drawCloudShadows(w, h, driftOffset, alpha = 0.45f)
                drawRainStreaks(w, h, timeProgress, intensity = 1.0f, rainDropOffsets)
                drawWetGlassDroplets(w, h, timeProgress)
            }
            WeatherEffectType.SNOW -> {
                drawCloudShadows(w, h, driftOffset, alpha = 0.2f)
                drawSnowflakes(w, h, timeProgress, condition.snowIntensity, snowflakeOffsets)
            }
            WeatherEffectType.FOG -> {
                drawFogRibbons(w, h, driftOffset, condition.fogIntensity)
            }
            WeatherEffectType.WINDY -> {
                drawCloudShadows(w, h, driftOffset * 2f, alpha = 0.25f)
                drawWindStreaks(w, h, timeProgress)
            }
        }

        // Thunderstorm full-screen lightning flash and rumble darkening
        if (thunderDarkening > 0f) {
            drawRect(
                color = Color.Black.copy(alpha = thunderDarkening),
                size = Size(w, h)
            )
        }
        if (lightningAlpha > 0f) {
            drawRect(
                color = Color(0xFFE0F7FA).copy(alpha = lightningAlpha),
                size = Size(w, h)
            )
        }
    }
}

// -------------------------------------------------------------
// 1. SUNNY BLOOM & LIGHT SHAFTS
// -------------------------------------------------------------
private fun DrawScope.drawSunnyBloom(width: Float, height: Float, rotationDegrees: Float) {
    val sunX = width * 0.78f
    val sunY = height * 0.16f

    // Warm radial bloom
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD54F).copy(alpha = 0.35f),
                Color(0xFFFFB300).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(sunX, sunY),
            radius = width * 0.75f
        ),
        radius = width * 0.75f,
        center = Offset(sunX, sunY)
    )

    // Subtle sun-ray light shafts
    val rayCount = 8
    val radAngle = rotationDegrees * (PI.toFloat() / 180f)
    for (i in 0 until rayCount) {
        val angle = radAngle + i * (2f * PI.toFloat() / rayCount)
        val shaftLength = width * 0.9f
        val rayPath = Path().apply {
            moveTo(sunX, sunY)
            lineTo(sunX + cos(angle - 0.08f) * shaftLength, sunY + sin(angle - 0.08f) * shaftLength)
            lineTo(sunX + cos(angle + 0.08f) * shaftLength, sunY + sin(angle + 0.08f) * shaftLength)
            close()
        }
        drawPath(
            rayPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF9C4).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(sunX, sunY),
                radius = shaftLength
            )
        )
    }
}

// -------------------------------------------------------------
// 2. CLOUD SHADOWS (Drifting overcast)
// -------------------------------------------------------------
private fun DrawScope.drawCloudShadows(width: Float, height: Float, driftOffset: Float, alpha: Float = 0.20f) {
    val shadowColor = Color(0xFF0F172A).copy(alpha = alpha)

    // 2 Soft elliptical drifting cloud shapes
    val c1X = ((driftOffset / (2f * PI.toFloat())) * (width + 300f)) - 150f
    val c1Y = height * 0.22f
    drawOval(
        color = shadowColor,
        topLeft = Offset(c1X - 140f, c1Y - 45f),
        size = Size(280f, 90f)
    )

    val c2X = (((driftOffset * 0.7f) / (2f * PI.toFloat())) * (width + 400f)) - 200f
    val c2Y = height * 0.38f
    drawOval(
        color = shadowColor.copy(alpha = alpha * 0.85f),
        topLeft = Offset(c2X - 180f, c2Y - 55f),
        size = Size(360f, 110f)
    )
}

// -------------------------------------------------------------
// 3. RAIN STREAKS (Density & Speed scale with intensity)
// -------------------------------------------------------------
private fun DrawScope.drawRainStreaks(
    width: Float,
    height: Float,
    timeProgress: Float,
    intensity: Float,
    particles: List<Triple<Float, Float, Float>>
) {
    val count = (particles.size * intensity.coerceIn(0.2f, 1.0f)).toInt()
    val streakLength = 22f + intensity * 20f
    val slant = 6f + intensity * 6f
    val strokeW = 1.2f + intensity * 0.8f

    val rainColor = Color(0xFFB3E5FC).copy(alpha = 0.55f + intensity * 0.35f)

    for (i in 0 until count) {
        val (initX, initY, speedMult) = particles[i]
        val progress = (initY + timeProgress * (1f + speedMult * 0.8f)) % 1f
        val px = (initX * width) - (progress * slant * 5f)
        val py = progress * height

        val start = Offset(px, py)
        val end = Offset(px - slant, py + streakLength)

        drawLine(
            color = rainColor,
            start = start,
            end = end,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Ground splash ring at the base of heavy droplets
        if (py > height * 0.80f && intensity > 0.5f) {
            val splashAlpha = (py - height * 0.80f) / (height * 0.20f)
            drawOval(
                color = Color.White.copy(alpha = 0.25f * splashAlpha),
                topLeft = Offset(px - 6f, py - 2f),
                size = Size(12f, 4f),
                style = Stroke(width = 1f)
            )
        }
    }
}

// -------------------------------------------------------------
// WET GLASS DROPLETS OVERLAY (Heavy Rain / Thunderstorm)
// -------------------------------------------------------------
private fun DrawScope.drawWetGlassDroplets(width: Float, height: Float, timeProgress: Float) {
    val droplets = listOf(
        Pair(width * 0.15f, (height * 0.25f + timeProgress * 25f) % height),
        Pair(width * 0.32f, (height * 0.55f + timeProgress * 40f) % height),
        Pair(width * 0.62f, (height * 0.18f + timeProgress * 20f) % height),
        Pair(width * 0.85f, (height * 0.68f + timeProgress * 35f) % height),
        Pair(width * 0.48f, (height * 0.82f + timeProgress * 15f) % height)
    )

    droplets.forEach { (dx, dy) ->
        // Outer refraction ring
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = 5.5f,
            center = Offset(dx, dy),
            style = Stroke(width = 1.2f)
        )
        // Core droplet
        drawCircle(
            color = Color(0xFFE1F5FE).copy(alpha = 0.55f),
            radius = 3.5f,
            center = Offset(dx, dy)
        )
    }
}

// -------------------------------------------------------------
// 4. SNOW PARTICLES (Gentle floating with horizontal drift)
// -------------------------------------------------------------
private fun DrawScope.drawSnowflakes(
    width: Float,
    height: Float,
    timeProgress: Float,
    intensity: Float,
    particles: List<Triple<Float, Float, Float>>
) {
    val count = (particles.size * intensity.coerceIn(0.25f, 1.0f)).toInt()
    val snowColor = Color.White.copy(alpha = 0.85f)

    for (i in 0 until count) {
        val (initX, initY, sizeSeed) = particles[i]
        val progress = (initY + timeProgress * 0.35f * (1f + sizeSeed * 0.4f)) % 1f
        val sway = sin(timeProgress * 2f * PI.toFloat() + i) * 14f

        val sx = (initX * width) + sway
        val sy = progress * height
        val radius = 2f + sizeSeed * 3.5f

        drawCircle(
            color = snowColor.copy(alpha = 0.6f + sizeSeed * 0.35f),
            radius = radius,
            center = Offset(sx, sy)
        )
    }

    // Heavy snow accumulation haze at ground level
    if (intensity > 0.5f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f * intensity)),
                startY = height * 0.78f,
                endY = height
            ),
            topLeft = Offset(0f, height * 0.78f),
            size = Size(width, height * 0.22f)
        )
    }
}

// -------------------------------------------------------------
// 5. FOG / MIST (Translucent sinusoidal flowing ribbons)
// -------------------------------------------------------------
private fun DrawScope.drawFogRibbons(width: Float, height: Float, driftOffset: Float, intensity: Float) {
    val fogColor = Color(0xFFECEFF1).copy(alpha = (0.28f * intensity.coerceIn(0.4f, 1.0f)))

    for (layer in 0..2) {
        val baseY = height * (0.35f + layer * 0.18f)
        val layerSpeed = (layer + 1) * 0.5f

        val fogPath = Path().apply {
            moveTo(0f, baseY + 60f)
            lineTo(0f, baseY)
            var curX = 0f
            while (curX <= width + 40f) {
                val waveY = baseY + sin(driftOffset * layerSpeed + curX * 0.01f + layer) * 22f
                lineTo(curX, waveY)
                curX += 30f
            }
            lineTo(width, baseY + 60f)
            close()
        }
        drawPath(fogPath, fogColor)
    }
}

// -------------------------------------------------------------
// 6. WIND STREAKS (High-speed horizontal breeze)
// -------------------------------------------------------------
private fun DrawScope.drawWindStreaks(width: Float, height: Float, timeProgress: Float) {
    val windColor = Color.White.copy(alpha = 0.35f)

    for (i in 0..6) {
        val progress = (timeProgress * 2.2f + i * 0.15f) % 1f
        val wx = progress * width
        val wy = height * (0.25f + (i * 0.10f))
        val len = 60f + (i % 3) * 25f

        drawLine(
            color = windColor,
            start = Offset(wx, wy),
            end = Offset(wx + len, wy),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
}
