package com.example.ui.diorama

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.TerrainCategory
import com.example.data.model.WeatherCondition

/**
 * MiniatureDioramaView:
 * The living two-layer diorama engine:
 * Layer 1 — Base Terrain Art (Changes with location & day/night)
 * Layer 2 — Weather FX Overlay (Animated particles based on live conditions)
 */
@Composable
fun MiniatureDioramaView(
    terrain: TerrainCategory,
    condition: WeatherCondition,
    isDay: Boolean,
    onBackgroundClick: () -> Unit,
    onThunderclap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Smooth transition between Day and Night (1.5s smooth crossfade)
    val targetDayFactor = if (isDay) 1.0f else 0.0f
    val animatedDayFactor by animateFloatAsState(
        targetValue = targetDayFactor,
        animationSpec = tween(durationMillis = 1500),
        label = "day_night_crossfade"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onBackgroundClick
            )
    ) {
        // Layer 1: Base Terrain Canvas with smooth terrain crossfade
        Crossfade(
            targetState = terrain,
            animationSpec = tween(1200),
            label = "terrain_crossfade"
        ) { currentTerrain ->
            DioramaCanvas(
                terrain = currentTerrain,
                dayFactor = animatedDayFactor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2: Real-time Weather FX Simulation
        WeatherFxOverlay(
            condition = condition,
            onThunderclap = onThunderclap,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle adaptive scrim overlay so data cards remain legible over any terrain/weather
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            ),
                            startY = 0f,
                            endY = size.height
                        )
                    )
                }
        )
    }
}
