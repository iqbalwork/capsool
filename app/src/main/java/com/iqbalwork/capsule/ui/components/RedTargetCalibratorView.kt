package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

/**
 * Interactive Red Target Calibrator (#FF3B30).
 * Displays a crosshair target circle directly above the front camera cutout
 * for pixel-perfect physical alignment.
 */
@Composable
fun RedTargetCalibratorView(
    targetSizeDp: Int = 36,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(targetSizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(targetSizeDp.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // Outer Target Ring
            drawCircle(
                color = CapsuleTokens.CalibratorRed,
                radius = radius - 2f,
                style = Stroke(width = 3f)
            )

            // Inner Center Ring
            drawCircle(
                color = CapsuleTokens.CalibratorRed,
                radius = radius * 0.45f,
                style = Stroke(width = 2f)
            )

            // Center Pin Point
            drawCircle(
                color = CapsuleTokens.CalibratorRed,
                radius = 3.5f
            )

            // Crosshairs
            drawLine(
                color = CapsuleTokens.CalibratorRed,
                start = Offset(centerOffset.x, 0f),
                end = Offset(centerOffset.x, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = CapsuleTokens.CalibratorRed,
                start = Offset(0f, centerOffset.y),
                end = Offset(size.width, centerOffset.y),
                strokeWidth = 2f
            )
        }
    }
}
