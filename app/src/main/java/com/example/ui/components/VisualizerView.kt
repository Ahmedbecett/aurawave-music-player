package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun VisualizerView(
    bars: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barHeight: Dp = 48.dp,
    activeColor1: Color = NeonCyan,
    activeColor2: Color = NeonPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vis_glow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val totalBars = bars.size
        if (totalBars == 0) return@Canvas

        val barSpacing = 4.dp.toPx()
        val availableWidth = size.width - (barSpacing * (totalBars - 1))
        val barWidth = (availableWidth / totalBars).coerceAtLeast(3.dp.toPx())
        val canvasHeight = size.height

        val gradient = Brush.verticalGradient(
            colors = listOf(activeColor1, activeColor2, ElectricBlue)
        )

        bars.forEachIndexed { index, rawAmp ->
            val amp = if (isPlaying) (rawAmp * glowPhase).coerceIn(0.08f, 1.0f) else 0.08f
            val currentBarHeight = (canvasHeight * amp).coerceAtLeast(4.dp.toPx())
            val x = index * (barWidth + barSpacing)
            val y = canvasHeight - currentBarHeight

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
