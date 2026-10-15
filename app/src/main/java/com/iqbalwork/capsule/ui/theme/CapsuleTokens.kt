package com.iqbalwork.capsule.ui.theme

import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

object CapsuleTokens {
    // Warm Dark Brown M3 Expressive Palette
    val Surface = Color(0xFF1A1615)
    val SurfaceContainerLowest = Color(0xFF120F0E)
    val SurfaceContainer = Color(0xFF231F1E)
    val SurfaceContainerHigh = Color(0xFF292423)
    val SurfaceContainerHighest = Color(0xFF342E2D)
    val Primary = Color(0xFFFFB690)             // Warm Peach
    val OnPrimary = Color(0xFF542200)
    val PrimaryContainer = Color(0xFF733512)
    val OnSurface = Color(0xFFEDE0DC)
    val OnSurfaceVariant = Color(0xFFD0C4BE)
    val Outline = Color(0xFF998E89)
    val OutlineVariant = Color(0xFF4E4440)
    val CalibratorRed = Color(0xFFFF3B30)

    // Motion Springs
    val FruitSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
    val FruitSpringDp = spring<Dp>(dampingRatio = 0.72f, stiffness = 380f)
    val FruitSpringIntOffset = spring<IntOffset>(dampingRatio = 0.72f, stiffness = 380f)
    val M3ElasticMotion = spring<Float>(dampingRatio = 0.62f, stiffness = 520f)

    // Capsule shell: pure black so the pill blends with the camera hole.
    val Shell = Color(0xFF000000)
    val ExpandedCornerRadius = 32.dp
}
