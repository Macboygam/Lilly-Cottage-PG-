package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "backdrop")
    
    // Smooth angle animation from 0 to 2*PI
    val angleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val isDark = isSystemInDarkTheme()

    val baseBg = if (isDark) Color(0xFF040614) else Color(0xFFF0F4FC)
    val colorOrb1 = if (isDark) Color(0x333F51B5) else Color(0x1F5C6BC0) // indigo soft
    val colorOrb2 = if (isDark) Color(0x229C27B0) else Color(0x1FBBDEFB) // purple / light blue
    val colorOrb3 = if (isDark) Color(0x1A00BCD4) else Color(0x1580DEEA) // cyan soft

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width == 0f || height == 0f) return@Canvas

            // Drift Orb 1
            val orb1X = width * 0.3f + width * 0.15f * sin(angleProgress.toDouble()).toFloat()
            val orb1Y = height * 0.4f + height * 0.2f * cos(angleProgress.toDouble()).toFloat()
            val rad1 = width * 0.35f

            // Drift Orb 2
            val orb2X = width * 0.7f + width * 0.15f * cos((angleProgress + 1.5).toDouble()).toFloat()
            val orb2Y = height * 0.3f + height * 0.15f * sin((angleProgress + 1.5).toDouble()).toFloat()
            val rad2 = width * 0.45f

            // Drift Orb 3
            val orb3X = width * 0.5f + width * 0.2f * sin((angleProgress * 1.5).toDouble()).toFloat()
            val orb3Y = height * 0.8f + height * 0.1f * cos((angleProgress * 1.5).toDouble()).toFloat()
            val rad3 = width * 0.4f

            // Draw Orb 1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorOrb1, Color.Transparent),
                    center = Offset(orb1X, orb1Y),
                    radius = rad1
                ),
                center = Offset(orb1X, orb1Y),
                radius = rad1
            )

            // Draw Orb 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorOrb2, Color.Transparent),
                    center = Offset(orb2X, orb2Y),
                    radius = rad2
                ),
                center = Offset(orb2X, orb2Y),
                radius = rad2
            )

            // Draw Orb 3
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorOrb3, Color.Transparent),
                    center = Offset(orb3X, orb3Y),
                    radius = rad3
                ),
                center = Offset(orb3X, orb3Y),
                radius = rad3
            )
        }
        content()
    }
}
