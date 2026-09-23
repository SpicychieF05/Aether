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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.TerrainCategory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Layer 1: Base Terrain Art (Isometric / Tilt-Shift Miniature Diorama)
 * Procedurally rendered with day/night lighting factor:
 * dayFactor = 1.0f (Full Day), 0.0f (Full Night), 0.5f (Dusk/Dawn Golden Hour)
 */
@Composable
fun DioramaCanvas(
    terrain: TerrainCategory,
    dayFactor: Float, // 0.0 (night) to 1.0 (day)
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "diorama_motion")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waves"
    )

    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Sky Dome Background
        drawSkyDome(dayFactor, width, height)

        // 2. Celestial Bodies (Sun or Moon + Stars)
        if (dayFactor < 0.85f) {
            drawStarField(width, height, starTwinkle, (1f - dayFactor))
            drawCrescentMoon(width * 0.78f, height * 0.16f, width * 0.065f, (1f - dayFactor))
        }
        if (dayFactor > 0.15f) {
            drawMiniatureSun(width * 0.78f, height * 0.16f, width * 0.075f, dayFactor)
        }

        // 3. Terrain Base Architecture & Landscape
        when (terrain) {
            TerrainCategory.CITY_SKYLINE -> drawCitySkyline(width, height, dayFactor)
            TerrainCategory.COASTAL -> drawCoastalShore(width, height, dayFactor, waveOffset)
            TerrainCategory.DESERT -> drawDesertDunes(width, height, dayFactor)
            TerrainCategory.ALPINE -> drawAlpinePeaks(width, height, dayFactor)
            TerrainCategory.HILLS_PLAINS -> drawGreenHills(width, height, dayFactor, waveOffset)
            TerrainCategory.TROPICAL -> drawTropicalRainforest(width, height, dayFactor)
            TerrainCategory.VILLAGE -> drawRusticVillage(width, height, dayFactor)
            TerrainCategory.SUBURBAN -> drawSuburbanNeighborhood(width, height, dayFactor)
        }

        // 4. Subtle Miniature Tilt-Shift Vignette / Base Pedestal Shadow
        drawTiltShiftShadows(width, height)
    }
}

private fun DrawScope.drawSkyDome(dayFactor: Float, width: Float, height: Float) {
    // Dynamic Sky Gradient interpolation
    val dayTop = Color(0xFF64B5F6)
    val dayBottom = Color(0xFFFFE0B2) // Warm horizon
    val nightTop = Color(0xFF080B14)
    val nightBottom = Color(0xFF1A233A)

    val currentTop = lerpColor(nightTop, dayTop, dayFactor)
    val currentBottom = lerpColor(nightBottom, dayBottom, dayFactor)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(currentTop, currentBottom),
            startY = 0f,
            endY = height * 0.85f
        ),
        size = Size(width, height)
    )
}

private fun DrawScope.drawMiniatureSun(x: Float, y: Float, radius: Float, alphaFactor: Float) {
    // Radiant halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD54F).copy(alpha = 0.5f * alphaFactor),
                Color(0xFFFFA726).copy(alpha = 0.2f * alphaFactor),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = radius * 2.8f
        ),
        radius = radius * 2.8f,
        center = Offset(x, y)
    )
    // Golden core
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFB300)),
            start = Offset(x - radius, y - radius),
            end = Offset(x + radius, y + radius)
        ),
        radius = radius,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawCrescentMoon(x: Float, y: Float, radius: Float, alphaFactor: Float) {
    val moonGlow = Color(0xFFE0E6ED).copy(alpha = 0.25f * alphaFactor)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(moonGlow, Color.Transparent),
            center = Offset(x, y),
            radius = radius * 2.2f
        ),
        radius = radius * 2.2f,
        center = Offset(x, y)
    )

    // Base Moon Disc
    drawCircle(
        color = Color(0xFFECEFF1).copy(alpha = 0.9f * alphaFactor),
        radius = radius,
        center = Offset(x, y)
    )
    // Dark cutout to make crisp crescent
    drawCircle(
        color = Color(0xFF0D1220).copy(alpha = 0.95f * alphaFactor),
        radius = radius * 0.85f,
        center = Offset(x + radius * 0.45f, y - radius * 0.2f)
    )
}

private fun DrawScope.drawStarField(width: Float, height: Float, twinkle: Float, alphaFactor: Float) {
    val starColor = Color.White.copy(alpha = 0.8f * twinkle * alphaFactor)
    val starDimColor = Color(0xFFB0BEC5).copy(alpha = 0.4f * (1.2f - twinkle) * alphaFactor)

    val fixedStars = listOf(
        Offset(width * 0.12f, height * 0.10f),
        Offset(width * 0.28f, height * 0.08f),
        Offset(width * 0.45f, height * 0.14f),
        Offset(width * 0.60f, height * 0.06f),
        Offset(width * 0.88f, height * 0.12f),
        Offset(width * 0.20f, height * 0.22f),
        Offset(width * 0.70f, height * 0.24f),
        Offset(width * 0.38f, height * 0.28f),
        Offset(width * 0.52f, height * 0.20f),
        Offset(width * 0.82f, height * 0.28f)
    )

    fixedStars.forEachIndexed { index, pos ->
        val c = if (index % 2 == 0) starColor else starDimColor
        val r = if (index % 3 == 0) 2.2f else 1.4f
        drawCircle(color = c, radius = r, center = pos)
    }
}

// -------------------------------------------------------------
// 1. DENSE MODERN CITY SKYLINE
// -------------------------------------------------------------
private fun DrawScope.drawCitySkyline(width: Float, height: Float, dayFactor: Float) {
    val horizonY = height * 0.52f

    // Distant background towers (darker silhouette)
    val distantColor = lerpColor(Color(0xFF141E34), Color(0xFF90CAF9), dayFactor)
    val distPath = Path().apply {
        moveTo(0f, height)
        lineTo(0f, horizonY + 20f)
        lineTo(width * 0.15f, horizonY + 20f)
        lineTo(width * 0.15f, horizonY - 40f)
        lineTo(width * 0.28f, horizonY - 40f)
        lineTo(width * 0.28f, horizonY - 10f)
        lineTo(width * 0.42f, horizonY - 60f)
        lineTo(width * 0.55f, horizonY - 60f)
        lineTo(width * 0.55f, horizonY + 10f)
        lineTo(width * 0.70f, horizonY - 80f)
        lineTo(width * 0.82f, horizonY - 80f)
        lineTo(width * 0.82f, horizonY - 30f)
        lineTo(width, horizonY - 30f)
        lineTo(width, height)
        close()
    }
    drawPath(distPath, distantColor)

    // Midground/Foreground Isometric Skyscrapers
    val buildingBaseColor = lerpColor(Color(0xFF1A2640), Color(0xFFE2E8F0), dayFactor)
    val buildingShadowColor = lerpColor(Color(0xFF111A2E), Color(0xFFCBD5E1), dayFactor)
    val windowLitColor = Color(0xFFFFD54F)
    val windowDayColor = Color(0xFF64B5F6)
    val currentWindowColor = lerpColor(windowLitColor, windowDayColor, dayFactor)

    // Centerpiece Tall Skyscraper
    val towerLeft = width * 0.35f
    val towerWidth = width * 0.28f
    val towerTop = horizonY - height * 0.24f

    // Front facade
    drawRect(
        color = buildingBaseColor,
        topLeft = Offset(towerLeft, towerTop),
        size = Size(towerWidth * 0.65f, height - towerTop)
    )
    // Side facade (isometric depth)
    drawRect(
        color = buildingShadowColor,
        topLeft = Offset(towerLeft + towerWidth * 0.65f, towerTop),
        size = Size(towerWidth * 0.35f, height - towerTop)
    )
    // Spire antenna
    drawLine(
        color = buildingBaseColor,
        start = Offset(towerLeft + towerWidth * 0.35f, towerTop),
        end = Offset(towerLeft + towerWidth * 0.35f, towerTop - 45f),
        strokeWidth = 3f
    )
    // Glowing spire beacon
    drawCircle(
        color = if (dayFactor < 0.5f) Color(0xFFFF5252) else Color(0xFFFF8A80),
        radius = 4f,
        center = Offset(towerLeft + towerWidth * 0.35f, towerTop - 45f)
    )

    // Lit windows on tower
    val cols = 4
    val rows = 9
    val winW = 8f
    val winH = 12f
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val winX = towerLeft + 12f + c * (winW + 8f)
            val winY = towerTop + 20f + r * (winH + 14f)
            if (winX + winW < towerLeft + towerWidth * 0.62f) {
                val isLit = (r + c * 2) % 3 != 0 || dayFactor > 0.5f
                if (isLit) {
                    drawRect(
                        color = currentWindowColor.copy(alpha = if (dayFactor < 0.5f) 0.9f else 0.4f),
                        topLeft = Offset(winX, winY),
                        size = Size(winW, winH)
                    )
                }
            }
        }
    }

    // Left flank Building
    drawRect(
        color = buildingShadowColor,
        topLeft = Offset(width * 0.08f, horizonY - height * 0.14f),
        size = Size(width * 0.22f, height)
    )
    drawRect(
        color = buildingBaseColor,
        topLeft = Offset(width * 0.08f, horizonY - height * 0.14f),
        size = Size(width * 0.15f, height)
    )

    // Right flank Building
    drawRect(
        color = buildingBaseColor,
        topLeft = Offset(width * 0.68f, horizonY - height * 0.18f),
        size = Size(width * 0.24f, height)
    )
    drawRect(
        color = buildingShadowColor,
        topLeft = Offset(width * 0.82f, horizonY - height * 0.18f),
        size = Size(width * 0.10f, height)
    )

    // City Ground Base Plane (Dark asphalt with glowing street traffic lines)
    val groundColor = lerpColor(Color(0xFF0D1322), Color(0xFF475569), dayFactor)
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, height * 0.76f),
        size = Size(width, height * 0.24f)
    )

    // Street traffic light streaks
    val streakAlpha = (1f - dayFactor).coerceIn(0.1f, 0.9f)
    drawLine(
        color = Color(0xFFFF5252).copy(alpha = streakAlpha),
        start = Offset(0f, height * 0.82f),
        end = Offset(width, height * 0.82f),
        strokeWidth = 3.5f
    )
    drawLine(
        color = Color(0xFFFFD54F).copy(alpha = streakAlpha),
        start = Offset(0f, height * 0.84f),
        end = Offset(width, height * 0.84f),
        strokeWidth = 3.5f
    )
}

// -------------------------------------------------------------
// 2. COASTAL SHORE & BAY (Kolkata / Miami / Mumbai)
// -------------------------------------------------------------
private fun DrawScope.drawCoastalShore(width: Float, height: Float, dayFactor: Float, waveOffset: Float) {
    val horizonY = height * 0.48f

    // Sea Water Plane
    val deepSea = lerpColor(Color(0xFF0A1E38), Color(0xFF0288D1), dayFactor)
    val surfSea = lerpColor(Color(0xFF143860), Color(0xFF4DD0E1), dayFactor)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(deepSea, surfSea),
            startY = horizonY,
            endY = height * 0.72f
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, height * 0.35f)
    )

    // Gentle animated wave foam lines
    val waveColor = Color.White.copy(alpha = if (dayFactor > 0.4f) 0.45f else 0.25f)
    for (i in 0..3) {
        val waveY = horizonY + 35f + i * 40f
        val path = Path().apply {
            moveTo(0f, waveY)
            var curX = 0f
            while (curX < width) {
                val cy = waveY + sin(waveOffset + curX * 0.02f + i) * 6f
                lineTo(curX, cy)
                curX += 15f
            }
            lineTo(width, waveY)
        }
        drawPath(path, waveColor, style = Stroke(width = 2.5f))
    }

    // Sandy Shoreline Curved Beach Base
    val sandDay = Color(0xFFFFE082)
    val sandNight = Color(0xFF2C3242)
    val currentSand = lerpColor(sandNight, sandDay, dayFactor)

    val shorePath = Path().apply {
        moveTo(0f, height * 0.65f)
        cubicTo(
            width * 0.35f, height * 0.68f,
            width * 0.65f, height * 0.60f,
            width, height * 0.64f
        )
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(shorePath, currentSand)

    // Lighthouse on coastal headland (Left side)
    val lhBaseX = width * 0.18f
    val lhBaseY = height * 0.62f
    val lhWidth = 24f
    val lhHeight = 70f

    // Lighthouse tower
    val lhPath = Path().apply {
        moveTo(lhBaseX - lhWidth * 0.5f, lhBaseY)
        lineTo(lhBaseX + lhWidth * 0.5f, lhBaseY)
        lineTo(lhBaseX + lhWidth * 0.3f, lhBaseY - lhHeight)
        lineTo(lhBaseX - lhWidth * 0.3f, lhBaseY - lhHeight)
        close()
    }
    drawPath(lhPath, Color(0xFFECEFF1))
    // Red stripes on lighthouse
    drawRect(
        color = Color(0xFFE53935),
        topLeft = Offset(lhBaseX - lhWidth * 0.38f, lhBaseY - lhHeight * 0.65f),
        size = Size(lhWidth * 0.76f, 14f)
    )
    // Lantern dome
    drawCircle(
        color = Color(0xFF37474F),
        radius = 8f,
        center = Offset(lhBaseX, lhBaseY - lhHeight - 4f)
    )

    // Lighthouse glowing beam at night
    if (dayFactor < 0.6f) {
        val beamAlpha = (1f - dayFactor) * 0.7f
        val beamPath = Path().apply {
            moveTo(lhBaseX, lhBaseY - lhHeight - 4f)
            lineTo(width, lhBaseY - lhHeight - 70f)
            lineTo(width, lhBaseY - lhHeight + 10f)
            close()
        }
        drawPath(
            beamPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFF59D).copy(alpha = beamAlpha), Color.Transparent)
            )
        )
    }

    // Distant Sailboat Silhouette
    val boatX = width * 0.72f
    val boatY = horizonY + 40f
    val sailColor = Color.White.copy(alpha = if (dayFactor > 0.4f) 0.9f else 0.5f)
    val sailPath = Path().apply {
        moveTo(boatX, boatY)
        lineTo(boatX + 16f, boatY)
        lineTo(boatX + 12f, boatY + 6f)
        lineTo(boatX + 4f, boatY + 6f)
        close()
    }
    drawPath(sailPath, Color(0xFF455A64))
    // Triangular sail
    val canvasSail = Path().apply {
        moveTo(boatX + 10f, boatY - 2f)
        lineTo(boatX + 10f, boatY - 18f)
        lineTo(boatX + 18f, boatY - 2f)
        close()
    }
    drawPath(canvasSail, sailColor)
}

// -------------------------------------------------------------
// 3. DESERT DUNES (Sahara / Jaisalmer / Phoenix)
// -------------------------------------------------------------
private fun DrawScope.drawDesertDunes(width: Float, height: Float, dayFactor: Float) {
    val horizonY = height * 0.48f

    val duneWarm = Color(0xFFFFB74D)
    val duneShadow = Color(0xFFF57C00)
    val duneNightWarm = Color(0xFF30243C)
    val duneNightShadow = Color(0xFF1B1425)

    val currentDuneLight = lerpColor(duneNightWarm, duneWarm, dayFactor)
    val currentDuneDark = lerpColor(duneNightShadow, duneShadow, dayFactor)

    // Distant Ridge Dunes
    val backDune = Path().apply {
        moveTo(0f, horizonY + 30f)
        quadraticTo(width * 0.3f, horizonY - 20f, width * 0.65f, horizonY + 35f)
        quadraticTo(width * 0.85f, horizonY - 10f, width, horizonY + 15f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(backDune, currentDuneDark.copy(alpha = 0.85f))

    // Midground Sweeping Sinuous Dune
    val midDune = Path().apply {
        moveTo(0f, horizonY + 80f)
        cubicTo(
            width * 0.25f, horizonY + 40f,
            width * 0.5f, horizonY + 120f,
            width, horizonY + 60f
        )
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(midDune, currentDuneLight)

    // Foreground sweeping dune crest
    val foreDune = Path().apply {
        moveTo(0f, horizonY + 150f)
        cubicTo(
            width * 0.45f, horizonY + 90f,
            width * 0.8f, horizonY + 170f,
            width, horizonY + 130f
        )
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(foreDune, currentDuneDark)

    // Desert Oasis Palms Silhouette
    val palmX = width * 0.32f
    val palmY = horizonY + 110f
    val palmColor = lerpColor(Color(0xFF111A1E), Color(0xFF2E7D32), dayFactor)

    drawLine(
        color = lerpColor(Color(0xFF212121), Color(0xFF6D4C41), dayFactor),
        start = Offset(palmX, palmY),
        end = Offset(palmX - 8f, palmY - 35f),
        strokeWidth = 3f
    )
    // Palm fronds
    val frondRadius = 16f
    for (angle in listOf(-40, -10, 20, 50, 80)) {
        val rad = angle * PI.toFloat() / 180f
        drawLine(
            color = palmColor,
            start = Offset(palmX - 8f, palmY - 35f),
            end = Offset(palmX - 8f + cos(rad) * frondRadius, palmY - 35f + sin(rad) * frondRadius),
            strokeWidth = 2.2f
        )
    }
}

// -------------------------------------------------------------
// 4. SNOWY MOUNTAINS / ALPINE (Zermatt / Alps / Denver)
// -------------------------------------------------------------
private fun DrawScope.drawAlpinePeaks(width: Float, height: Float, dayFactor: Float) {
    val horizonY = height * 0.54f

    val rockDay = Color(0xFF78909C)
    val rockNight = Color(0xFF1E293B)
    val snowDay = Color(0xFFFFFFFF)
    val snowNight = Color(0xFFB0BEC5)

    val currentRock = lerpColor(rockNight, rockDay, dayFactor)
    val currentSnow = lerpColor(snowNight, snowDay, dayFactor)

    // Distant background mountain ridge
    val backMountain = Path().apply {
        moveTo(0f, horizonY)
        lineTo(width * 0.18f, horizonY - 110f)
        lineTo(width * 0.42f, horizonY)
        lineTo(width * 0.65f, horizonY - 140f)
        lineTo(width * 0.88f, horizonY - 30f)
        lineTo(width, horizonY - 70f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(backMountain, currentRock.copy(alpha = 0.7f))

    // Main Iconic Alpine Matterhorn-like Peak
    val peakX = width * 0.52f
    val peakTopY = horizonY - 180f
    val peakLeftX = width * 0.18f
    val peakRightX = width * 0.85f

    val mainPeak = Path().apply {
        moveTo(peakLeftX, horizonY)
        lineTo(peakX, peakTopY)
        lineTo(peakRightX, horizonY)
        close()
    }
    drawPath(mainPeak, currentRock)

    // Snow Cap on Top
    val snowCap = Path().apply {
        moveTo(peakX - 45f, peakTopY + 55f)
        lineTo(peakX, peakTopY)
        lineTo(peakX + 50f, peakTopY + 65f)
        lineTo(peakX + 25f, peakTopY + 70f)
        lineTo(peakX + 8f, peakTopY + 52f)
        lineTo(peakX - 18f, peakTopY + 68f)
        close()
    }
    drawPath(snowCap, currentSnow)

    // Pine Forest Treeline at foothills
    val pineColor = lerpColor(Color(0xFF0F1E1B), Color(0xFF1B4332), dayFactor)
    for (i in 0..12) {
        val treeX = width * 0.08f + i * (width * 0.07f)
        val treeY = horizonY + (i % 3) * 12f
        val treePath = Path().apply {
            moveTo(treeX - 12f, treeY)
            lineTo(treeX, treeY - 26f)
            lineTo(treeX + 12f, treeY)
            close()
        }
        drawPath(treePath, pineColor)
    }

    // Cozy Alpine Chalet (Bottom right)
    val chaletX = width * 0.68f
    val chaletY = height * 0.74f
    val chaletW = 44f
    val chaletH = 26f

    // Chalet wall
    drawRect(
        color = lerpColor(Color(0xFF2E1C14), Color(0xFF6D4C41), dayFactor),
        topLeft = Offset(chaletX, chaletY),
        size = Size(chaletW, chaletH)
    )
    // Chalet pitched snow roof
    val roofPath = Path().apply {
        moveTo(chaletX - 8f, chaletY)
        lineTo(chaletX + chaletW * 0.5f, chaletY - 18f)
        lineTo(chaletX + chaletW + 8f, chaletY)
        close()
    }
    drawPath(roofPath, currentSnow)

    // Warm lit window at night
    val windowGlow = if (dayFactor < 0.6f) Color(0xFFFFD54F) else Color(0xFF81D4FA)
    drawRect(
        color = windowGlow,
        topLeft = Offset(chaletX + 12f, chaletY + 8f),
        size = Size(10f, 10f)
    )
}

// -------------------------------------------------------------
// 5. GREEN HILLS / COUNTRYSIDE / PLAINS (Kyoto / Cotswolds)
// -------------------------------------------------------------
private fun DrawScope.drawGreenHills(width: Float, height: Float, dayFactor: Float, waveOffset: Float) {
    val horizonY = height * 0.52f

    val hillFarDay = Color(0xFF81C784)
    val hillFarNight = Color(0xFF1B2E24)
    val hillNearDay = Color(0xFF4CAF50)
    val hillNearNight = Color(0xFF13231A)

    val currentFar = lerpColor(hillFarNight, hillFarDay, dayFactor)
    val currentNear = lerpColor(hillNearNight, hillNearDay, dayFactor)

    // Far undulating hill
    val farHill = Path().apply {
        moveTo(0f, horizonY + 20f)
        quadraticTo(width * 0.35f, horizonY - 35f, width * 0.7f, horizonY + 30f)
        quadraticTo(width * 0.85f, horizonY + 5f, width, horizonY + 15f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(farHill, currentFar)

    // Near rolling terrace hill
    val nearHill = Path().apply {
        moveTo(0f, horizonY + 90f)
        cubicTo(
            width * 0.25f, horizonY + 40f,
            width * 0.65f, horizonY + 110f,
            width, horizonY + 70f
        )
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(nearHill, currentNear)

    // Gentle Wind Turbine (Mid-left)
    val turbineX = width * 0.24f
    val turbineY = horizonY + 40f
    val turbineColor = Color(0xFFECEFF1)

    // Pole
    drawLine(
        color = turbineColor,
        start = Offset(turbineX, turbineY),
        end = Offset(turbineX, turbineY - 60f),
        strokeWidth = 3f
    )
    // Rotating blades
    val bladeLen = 26f
    for (i in 0..2) {
        val angle = waveOffset * 0.8f + i * (2f * PI.toFloat() / 3f)
        drawLine(
            color = turbineColor,
            start = Offset(turbineX, turbineY - 60f),
            end = Offset(turbineX + cos(angle) * bladeLen, turbineY - 60f + sin(angle) * bladeLen),
            strokeWidth = 2.2f
        )
    }

    // Country Cottage (Right hill)
    val cottageX = width * 0.74f
    val cottageY = horizonY + 80f
    drawRect(
        color = lerpColor(Color(0xFF2C241E), Color(0xFFECEFF1), dayFactor),
        topLeft = Offset(cottageX, cottageY),
        size = Size(36f, 22f)
    )
    val cottageRoof = Path().apply {
        moveTo(cottageX - 4f, cottageY)
        lineTo(cottageX + 18f, cottageY - 14f)
        lineTo(cottageX + 40f, cottageY)
        close()
    }
    drawPath(cottageRoof, lerpColor(Color(0xFF1E1410), Color(0xFFB71C1C), dayFactor))

    // Glowing cottage window
    drawRect(
        color = if (dayFactor < 0.6f) Color(0xFFFFD54F) else Color(0xFF90CAF9),
        topLeft = Offset(cottageX + 8f, cottageY + 6f),
        size = Size(8f, 8f)
    )
}

// -------------------------------------------------------------
// 6. TROPICAL RAINFOREST (Bali / Amazon / Kerala)
// -------------------------------------------------------------
private fun DrawScope.drawTropicalRainforest(width: Float, height: Float, dayFactor: Float) {
    val horizonY = height * 0.50f

    val deepJungleDay = Color(0xFF1B5E20)
    val deepJungleNight = Color(0xFF0A1F0D)
    val lushDay = Color(0xFF2E7D32)
    val lushNight = Color(0xFF102814)

    val currentDeep = lerpColor(deepJungleNight, deepJungleDay, dayFactor)
    val currentLush = lerpColor(lushNight, lushDay, dayFactor)

    // Deep Canopy Ridge
    val backCanopy = Path().apply {
        moveTo(0f, horizonY + 20f)
        var x = 0f
        while (x < width) {
            val r = 25f + (x % 30)
            quadraticTo(x + r * 0.5f, horizonY - 20f, x + r, horizonY + 20f)
            x += r
        }
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(backCanopy, currentDeep)

    // Tiered Waterfall River stream
    val waterColor = lerpColor(Color(0xFF102A43), Color(0xFF4DD0E1), dayFactor)
    val waterPath = Path().apply {
        moveTo(width * 0.48f, horizonY + 15f)
        lineTo(width * 0.54f, horizonY + 15f)
        lineTo(width * 0.60f, height)
        lineTo(width * 0.42f, height)
        close()
    }
    drawPath(waterPath, waterColor)

    // Foreground Dense Broadleaf Canopy
    val foreCanopy = Path().apply {
        moveTo(0f, horizonY + 85f)
        cubicTo(width * 0.3f, horizonY + 40f, width * 0.7f, horizonY + 110f, width, horizonY + 70f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(foreCanopy, currentLush)

    // Exotic Palm silhouettes in corners
    val palmColor = lerpColor(Color(0xFF071409), Color(0xFF388E3C), dayFactor)
    val leftPalmX = width * 0.12f
    val leftPalmY = height * 0.65f

    for (angle in listOf(-60, -30, 0, 30, 60)) {
        val rad = angle * PI.toFloat() / 180f
        drawLine(
            color = palmColor,
            start = Offset(leftPalmX, leftPalmY),
            end = Offset(leftPalmX + cos(rad) * 45f, leftPalmY + sin(rad) * 35f),
            strokeWidth = 3f
        )
    }
}

// -------------------------------------------------------------
// 7. RUSTIC VILLAGE (Hallstatt / Shirakawa / Cotswolds)
// -------------------------------------------------------------
private fun DrawScope.drawRusticVillage(width: Float, height: Float, dayFactor: Float) {
    val horizonY = height * 0.53f

    // Soft hillside background
    val hillColor = lerpColor(Color(0xFF14201A), Color(0xFF66BB6A), dayFactor)
    val hillPath = Path().apply {
        moveTo(0f, horizonY + 10f)
        quadraticTo(width * 0.5f, horizonY - 40f, width, horizonY)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(hillPath, hillColor)

    // Cobblestone ground
    val roadColor = lerpColor(Color(0xFF1C1917), Color(0xFF78716C), dayFactor)
    drawRect(
        color = roadColor,
        topLeft = Offset(0f, height * 0.72f),
        size = Size(width, height * 0.28f)
    )

    // Village Houses with Gable terracotta roofs
    val wallColor = lerpColor(Color(0xFF262626), Color(0xFFF5F5F4), dayFactor)
    val roofColor = lerpColor(Color(0xFF1C120C), Color(0xFFC2410C), dayFactor)

    // Clock Tower in Center
    val towerX = width * 0.44f
    val towerY = horizonY - 80f
    val towerW = 32f
    val towerH = 110f

    drawRect(
        color = wallColor,
        topLeft = Offset(towerX, towerY),
        size = Size(towerW, towerH)
    )
    val steeple = Path().apply {
        moveTo(towerX - 4f, towerY)
        lineTo(towerX + towerW * 0.5f, towerY - 40f)
        lineTo(towerX + towerW + 4f, towerY)
        close()
    }
    drawPath(steeple, roofColor)
    // Clock face
    drawCircle(
        color = if (dayFactor < 0.5f) Color(0xFFFFD54F) else Color(0xFFECEFF1),
        radius = 7f,
        center = Offset(towerX + towerW * 0.5f, towerY + 18f)
    )

    // Staggered houses left & right
    val house1X = width * 0.14f
    val house1Y = horizonY + 20f
    drawRect(color = wallColor, topLeft = Offset(house1X, house1Y), size = Size(45f, 35f))
    val h1Roof = Path().apply {
        moveTo(house1X - 4f, house1Y)
        lineTo(house1X + 22f, house1Y - 20f)
        lineTo(house1X + 49f, house1Y)
        close()
    }
    drawPath(h1Roof, roofColor)

    val house2X = width * 0.72f
    val house2Y = horizonY + 15f
    drawRect(color = wallColor, topLeft = Offset(house2X, house2Y), size = Size(50f, 40f))
    val h2Roof = Path().apply {
        moveTo(house2X - 4f, house2Y)
        lineTo(house2X + 25f, house2Y - 22f)
        lineTo(house2X + 54f, house2Y)
        close()
    }
    drawPath(h2Roof, roofColor)

    // Warm glowing lanterns along village lane
    val lanternGlow = if (dayFactor < 0.6f) Color(0xFFFFB300) else Color(0xFFFFE082).copy(alpha = 0.5f)
    listOf(width * 0.32f, width * 0.64f).forEach { lx ->
        val ly = height * 0.78f
        drawLine(
            color = Color(0xFF262626),
            start = Offset(lx, ly),
            end = Offset(lx, ly - 32f),
            strokeWidth = 2.5f
        )
        drawCircle(
            color = lanternGlow,
            radius = 6f,
            center = Offset(lx, ly - 32f)
        )
    }
}

// -------------------------------------------------------------
// 8. SUBURBAN NEIGHBORHOOD
// -------------------------------------------------------------
private fun DrawScope.drawSuburbanNeighborhood(width: Float, height: Float, dayFactor: Float) {
    val horizonY = height * 0.54f

    // Soft sky gradient transition
    val lawnDay = Color(0xFF66BB6A)
    val lawnNight = Color(0xFF16251B)
    val currentLawn = lerpColor(lawnNight, lawnDay, dayFactor)

    drawRect(
        color = currentLawn,
        topLeft = Offset(0f, horizonY),
        size = Size(width, height - horizonY)
    )

    // Residential Road
    val roadColor = lerpColor(Color(0xFF18181B), Color(0xFF52525B), dayFactor)
    val roadPath = Path().apply {
        moveTo(0f, height * 0.78f)
        lineTo(width, height * 0.74f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(roadPath, roadColor)

    // Cozy Suburban Houses
    val houseColor = lerpColor(Color(0xFF27272A), Color(0xFFFAFAFA), dayFactor)
    val roofBlue = lerpColor(Color(0xFF1E293B), Color(0xFF334155), dayFactor)

    for (i in 0..2) {
        val hx = width * 0.12f + i * (width * 0.32f)
        val hy = horizonY + 30f
        val hw = width * 0.22f
        val hh = 38f

        // Base walls
        drawRect(
            color = houseColor,
            topLeft = Offset(hx, hy),
            size = Size(hw, hh)
        )
        // Roof
        val rPath = Path().apply {
            moveTo(hx - 6f, hy)
            lineTo(hx + hw * 0.5f, hy - 20f)
            lineTo(hx + hw + 6f, hy)
            close()
        }
        drawPath(rPath, roofBlue)

        // Windows
        val winGlow = if (dayFactor < 0.6f) Color(0xFFFFD54F) else Color(0xFFBAE6FD)
        drawRect(
            color = winGlow,
            topLeft = Offset(hx + 8f, hy + 8f),
            size = Size(10f, 12f)
        )
        drawRect(
            color = winGlow,
            topLeft = Offset(hx + hw - 18f, hy + 8f),
            size = Size(10f, 12f)
        )
    }

    // Streetlamps with warm light cones at night
    if (dayFactor < 0.7f) {
        val lampX = width * 0.48f
        val lampY = height * 0.76f
        val conePath = Path().apply {
            moveTo(lampX, lampY - 40f)
            lineTo(lampX - 35f, lampY + 15f)
            lineTo(lampX + 35f, lampY + 15f)
            close()
        }
        drawPath(
            conePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.45f * (1f - dayFactor)), Color.Transparent)
            )
        )
    }
}

// -------------------------------------------------------------
// TILT SHIFT MINIATURE BLUR & DEPTH VIGNETTE
// -------------------------------------------------------------
private fun DrawScope.drawTiltShiftShadows(width: Float, height: Float) {
    // Top Miniature lens blur gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.35f),
                Color.Transparent
            ),
            startY = 0f,
            endY = height * 0.18f
        ),
        size = Size(width, height * 0.18f)
    )

    // Bottom grounding shadow / glass diorama base
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.45f)
            ),
            startY = height * 0.82f,
            endY = height
        ),
        topLeft = Offset(0f, height * 0.82f),
        size = Size(width, height * 0.18f)
    )
}

private fun lerpColor(c1: Color, c2: Color, factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = c1.red + (c2.red - c1.red) * f,
        green = c1.green + (c2.green - c1.green) * f,
        blue = c1.blue + (c2.blue - c1.blue) * f,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * f
    )
}
