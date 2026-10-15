package com.iqbalwork.capsule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.iqbalwork.capsule.core.settings.ColorSource

/** Fallback palette (spec §2.7) used when the user picks the brand colors. */
private val BrandDarkColorScheme = darkColorScheme(
    primary = CapsuleTokens.Primary,
    onPrimary = CapsuleTokens.OnPrimary,
    primaryContainer = CapsuleTokens.PrimaryContainer,
    surface = CapsuleTokens.Surface,
    onSurface = CapsuleTokens.OnSurface,
    onSurfaceVariant = CapsuleTokens.OnSurfaceVariant,
    surfaceContainerLowest = CapsuleTokens.SurfaceContainerLowest,
    surfaceContainer = CapsuleTokens.SurfaceContainer,
    surfaceContainerHigh = CapsuleTokens.SurfaceContainerHigh,
    surfaceContainerHighest = CapsuleTokens.SurfaceContainerHighest,
    background = CapsuleTokens.SurfaceContainerLowest,
    outline = CapsuleTokens.Outline,
    outlineVariant = CapsuleTokens.OutlineVariant
)

@Composable
fun capsuleColorScheme(colorSource: ColorSource, darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    return when (colorSource) {
        ColorSource.DYNAMIC -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        ColorSource.BRAND -> BrandDarkColorScheme
    }
}

/** Companion app theme: follows the system light/dark setting. */
@Composable
fun CapsuleTheme(
    colorSource: ColorSource = ColorSource.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = capsuleColorScheme(colorSource, darkTheme),
        typography = Typography,
        content = content
    )
}

/** Overlay theme: always dark so the capsule blends with the camera cutout. */
@Composable
fun OverlayTheme(colorSource: ColorSource, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = capsuleColorScheme(colorSource, darkTheme = true),
        typography = Typography,
        content = content
    )
}
