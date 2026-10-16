package com.iqbalwork.capsule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.CapsuleShapeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Collapsed corner radius for [style]. PILL ignores the radius setting and rounds off completely;
 * the other two honour it, which is what existing users already have configured.
 */
fun collapsedCornerDp(style: CapsuleShapeStyle, cornerDp: Int, heightDp: Int): Dp =
    if (style == CapsuleShapeStyle.PILL) (heightDp / 2f).dp else cornerDp.dp

/** Shape for [style] at the given (animated) [corner] radius. */
fun capsuleShape(style: CapsuleShapeStyle, corner: Dp): Shape =
    if (style == CapsuleShapeStyle.SQUIRCLE) SquircleShape(corner) else RoundedCornerShape(corner)

/**
 * Rounded rectangle whose corners are quarter superellipses (|x|^4 + |y|^4 = r^4) rather than
 * circular arcs, so the edge curves into the corner gradually instead of at a fixed radius.
 */
class SquircleShape(private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(corner.value * density.density, min(size.width, size.height) / 2f).coerceAtLeast(0f)
        val path = Path()
        if (r <= 0f) {
            path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            return Outline.Generic(path)
        }
        val arc = squircleCorner(r, CORNER_STEPS)
        val w = size.width
        val h = size.height
        // Clockwise from the top edge. `arc` runs from a corner's side tangent (dx = r, dy = 0) to
        // its top/bottom tangent (dx = 0, dy = r), so corners entered from the top or bottom edge
        // walk it reversed.
        path.moveTo(r, 0f)
        path.lineTo(w - r, 0f)
        arc.asReversed().forEach { (dx, dy) -> path.lineTo(w - r + dx, r - dy) }
        path.lineTo(w, h - r)
        arc.forEach { (dx, dy) -> path.lineTo(w - r + dx, h - r + dy) }
        path.lineTo(r, h)
        arc.asReversed().forEach { (dx, dy) -> path.lineTo(r - dx, h - r + dy) }
        path.lineTo(0f, r)
        arc.forEach { (dx, dy) -> path.lineTo(r - dx, r - dy) }
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        const val CORNER_STEPS = 12
    }
}

/**
 * Offsets (dx, dy) from a corner's center along a quarter superellipse of radius [r], ordered from
 * the horizontal tangent (r, 0) to the vertical tangent (0, r). Always ends on both tangent points
 * exactly, so the straight edges join the corner without a kink.
 */
internal fun squircleCorner(r: Float, steps: Int): List<Pair<Float, Float>> =
    (0..steps).map { i ->
        val t = (PI / 2 * i / steps).toFloat()
        val dx = if (i == steps) 0f else r * cos(t).pow(SQUIRCLE_EXPONENT)
        val dy = if (i == 0) 0f else r * sin(t).pow(SQUIRCLE_EXPONENT)
        dx to dy
    }

/** 2 / n for n = 4. */
private const val SQUIRCLE_EXPONENT = 0.5f

private const val MAX_BACKGROUND_LUMINANCE = 0.3f

/** sRGB relative luminance (0 = black, 1 = white) of an ARGB color, ignoring alpha. */
internal fun luminance(argb: Long): Float {
    fun channel(shift: Int): Float {
        val c = ((argb shr shift) and 0xFF) / 255f
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(16) + 0.7152f * channel(8) + 0.0722f * channel(0)
}

/**
 * The cards draw white text on the capsule, so a light custom background would make them
 * unreadable. A color above [MAX_BACKGROUND_LUMINANCE] is scaled toward black until it is not,
 * keeping its hue, and is always returned fully opaque.
 */
fun readableShellColor(argb: Long): Long {
    val opaque = 0xFF000000L or (argb and 0xFFFFFFL)
    var scale = 1f
    var result = opaque
    // Luminance falls monotonically with the scale, so a few halvings converge.
    repeat(MAX_DARKEN_STEPS) {
        if (luminance(result) <= MAX_BACKGROUND_LUMINANCE) return result
        scale *= 0.85f
        result = 0xFF000000L or
            (scaleChannel(opaque, 16, scale) shl 16) or
            (scaleChannel(opaque, 8, scale) shl 8) or
            scaleChannel(opaque, 0, scale)
    }
    return result
}

private const val MAX_DARKEN_STEPS = 40

private fun scaleChannel(argb: Long, shift: Int, scale: Float): Long =
    (((argb shr shift) and 0xFF) * scale).toLong().coerceIn(0L, 255L)
