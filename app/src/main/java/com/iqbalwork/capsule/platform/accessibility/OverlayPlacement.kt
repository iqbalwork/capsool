package com.iqbalwork.capsule.platform.accessibility

import com.iqbalwork.capsule.core.settings.Geometry
import kotlin.math.min
import kotlin.math.roundToInt

/** Camera cutout bounds in screen pixels (a plain copy of `DisplayCutout.boundingRectTop`). */
data class CutoutBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Where the overlay window goes, in screen pixels. The content offsets shift the capsule
 * inside the window when the window had to be clamped to the screen edges.
 */
data class WindowPlacement(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val contentOffsetXPx: Int,
    val contentOffsetYPx: Int
)

/**
 * Pure geometry for the overlay window (spec §2.4.2–2.4.3): the window hugs the capsule so
 * touches elsewhere reach the app below, and grows once to card size when expanding.
 */
object OverlayPlacement {
    const val MARGIN_DP = 8
    const val EXPANDED_MAX_WIDTH_DP = 360
    const val EXPANDED_HEIGHT_DP = 196
    private const val SCREEN_GUTTER_DP = 8
    private const val NO_CUTOUT_TOP_DP = 6

    fun expandedWidthDp(screenWidthDp: Float): Float =
        min(EXPANDED_MAX_WIDTH_DP.toFloat(), screenWidthDp - 2 * SCREEN_GUTTER_DP)

    fun compute(
        screenWidthPx: Int,
        density: Float,
        cutout: CutoutBounds?,
        geometry: Geometry,
        expanded: Boolean
    ): WindowPlacement {
        fun px(dp: Float) = dp * density
        val capsuleHeight = px(geometry.heightDp.toFloat())
        val centerX = (cutout?.centerX ?: (screenWidthPx / 2f)) + px(geometry.offsetXDp.toFloat())
        val defaultCenterY = cutout?.centerY ?: (px(NO_CUTOUT_TOP_DP.toFloat()) + capsuleHeight / 2f)
        val capsuleTop = defaultCenterY - capsuleHeight / 2f + px(geometry.offsetYDp.toFloat())

        val contentWidth = if (expanded) {
            maxOf(px(expandedWidthDp(screenWidthPx / density)), px(geometry.widthDp.toFloat()))
        } else {
            px(geometry.widthDp.toFloat())
        }
        val contentHeight = if (expanded) px(EXPANDED_HEIGHT_DP.toFloat()) else capsuleHeight
        val margin = px(MARGIN_DP.toFloat())

        val width = (contentWidth + 2 * margin).roundToInt()
        val height = (contentHeight + 2 * margin).roundToInt()
        val idealX = (centerX - width / 2f).roundToInt()
        val x = idealX.coerceIn(0, (screenWidthPx - width).coerceAtLeast(0))
        val idealY = (capsuleTop - margin).roundToInt()
        val y = idealY.coerceAtLeast(0)

        return WindowPlacement(
            x = x,
            y = y,
            width = width,
            height = height,
            contentOffsetXPx = idealX - x,
            contentOffsetYPx = idealY - y
        )
    }
}
