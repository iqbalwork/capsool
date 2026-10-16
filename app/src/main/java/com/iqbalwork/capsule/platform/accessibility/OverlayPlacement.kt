package com.iqbalwork.capsule.platform.accessibility

import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.PlacementMode
import kotlin.math.ceil
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
    val contentOffsetYPx: Int,
    /**
     * Extra height at the top of an opened card (camera mode): Android reserves the very top of the
     * screen for pulling down the notification shade, so interactive content starts below the status
     * bar. Zero for a collapsed capsule and in independent mode.
     */
    val contentTopInsetPx: Int = 0,
    /**
     * Width in dp of the strip at the middle of the pill that sits over the camera hole (the hole
     * plus a little room each side); 0 when there is no hole to avoid. Content must stay out of it.
     */
    val cameraGapDp: Int = 0
)

/** A rectangle in window pixels (a plain stand-in for android.graphics.Rect, for unit tests). */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Pure geometry for the overlay window (spec §2.4.2–2.4.3): the window hugs the capsule so
 * touches elsewhere reach the app below, and grows once to card size when expanding.
 */
object OverlayPlacement {
    const val MARGIN_DP = 8
    const val EXPANDED_MAX_WIDTH_DP = 360
    const val EXPANDED_HEIGHT_DP = 196

    /**
     * LIST view stacks the same cards vertically, so a row is a full card plus the gap to the
     * next one. The panel shows at most [MAX_LIST_ROWS] rows and the list scrolls beyond that;
     * without a cap the window would grow down over the screen for every card the user adds.
     */
    const val MAX_LIST_ROWS = 2
    const val LIST_ROW_DP = EXPANDED_HEIGHT_DP + 8

    /** Height of the LIST panel for [rows] cards, clamped to [MAX_LIST_ROWS]. */
    fun listHeightDp(rows: Int): Float = (LIST_ROW_DP * rows.coerceIn(1, MAX_LIST_ROWS)).toFloat()

    /**
     * Height of the expanded content area. Both the window controller and the Compose content
     * call this with the same row count, so the panel they draw cannot disagree with the window
     * it is drawn into. [listRows] is 0 whenever the carousel is used.
     */
    fun expandedContentHeightDp(listRows: Int): Float =
        if (listRows > 0) listHeightDp(listRows) else EXPANDED_HEIGHT_DP.toFloat()
    /** Mini events widen the pill so an icon, a title and a value fit (F-60..F-69). */
    const val MINI_EVENT_EXTRA_WIDTH_DP = 110

    /** Room kept clear on each side of the camera hole. */
    const val CAMERA_GAP_PADDING_DP = 4

    /** A pill split around the hole needs more room, or its two halves cannot hold a title and a value. */
    const val SPLIT_EXTRA_WIDTH_DP = 40

    /** The strip over the camera hole, from the hole's width in pixels; 0 when there is none. */
    fun cameraGapDp(cutoutWidthPx: Int, density: Float): Int =
        if (cutoutWidthPx <= 0) 0 else ceil(cutoutWidthPx / density).toInt() + 2 * CAMERA_GAP_PADDING_DP

    fun pillWidthDp(geometry: Geometry, miniEvent: Boolean, cameraGapDp: Int = 0): Int {
        if (!miniEvent) return geometry.widthDp
        val split = if (cameraGapDp > 0) cameraGapDp + SPLIT_EXTRA_WIDTH_DP else 0
        return geometry.widthDp + MINI_EVENT_EXTRA_WIDTH_DP + split
    }
    private const val SCREEN_GUTTER_DP = 8
    private const val NO_CUTOUT_TOP_DP = 6

    /** Space between the status bar and the capsule in independent mode. */
    const val INDEPENDENT_GAP_DP = 4

    /** The card's width: capped at [EXPANDED_MAX_WIDTH_DP], or the whole screen less the gutters when [full]. */
    fun expandedWidthDp(screenWidthDp: Float, full: Boolean = false): Float =
        if (full) screenWidthDp - 2 * SCREEN_GUTTER_DP
        else min(EXPANDED_MAX_WIDTH_DP.toFloat(), screenWidthDp - 2 * SCREEN_GUTTER_DP)

    /**
     * The part of a card-sized window the capsule covers, for the touchable region (API 34+):
     * the pill when collapsed, the whole card when expanded. Mirrors the layout in OverlayRoot
     * (top-center, one margin from the top, shifted by the content offsets).
     */
    fun capsuleRect(
        window: WindowPlacement,
        density: Float,
        geometry: Geometry,
        expanded: Boolean,
        miniEvent: Boolean = false
    ): PxRect {
        val margin = MARGIN_DP * density
        val width = if (expanded) window.width - 2 * margin else pillWidthDp(geometry, miniEvent, window.cameraGapDp) * density
        // Expanded: the whole window less its margins, so a taller card (list rows, the camera-mode
        // top inset) is touchable all the way down.
        val height = if (expanded) window.height - 2 * margin else geometry.heightDp * density
        val centerX = window.width / 2f + window.contentOffsetXPx
        val top = margin + window.contentOffsetYPx
        return PxRect(
            left = (centerX - width / 2f).roundToInt(),
            top = top.roundToInt(),
            right = (centerX + width / 2f).roundToInt(),
            bottom = (top + height).roundToInt()
        )
    }

    fun compute(
        screenWidthPx: Int,
        density: Float,
        cutout: CutoutBounds?,
        geometry: Geometry,
        expanded: Boolean,
        miniEvent: Boolean = false,
        listRows: Int = 0,
        mode: PlacementMode = PlacementMode.CAMERA,
        statusBarHeightPx: Int = 0,
        fullWidthCard: Boolean = false
    ): WindowPlacement {
        fun px(dp: Float) = dp * density
        val capsuleHeight = px(geometry.heightDp.toFloat())
        val independent = mode == PlacementMode.INDEPENDENT
        // Independent ignores the camera: centered on the screen, just under the status bar.
        val baseCenterX = if (independent) screenWidthPx / 2f else cutout?.centerX ?: (screenWidthPx / 2f)
        val centerX = baseCenterX + px(geometry.offsetXDp.toFloat())
        val defaultCenterY = when {
            independent -> statusBarHeightPx + px(INDEPENDENT_GAP_DP.toFloat()) + capsuleHeight / 2f
            else -> cutout?.centerY ?: (px(NO_CUTOUT_TOP_DP.toFloat()) + capsuleHeight / 2f)
        }
        val capsuleTop = defaultCenterY - capsuleHeight / 2f + px(geometry.offsetYDp.toFloat())

        // Independent mode is not over the camera, so there is nothing to keep clear.
        val cameraGap = if (independent || cutout == null) 0 else cameraGapDp(cutout.right - cutout.left, density)
        val pillWidth = px(pillWidthDp(geometry, miniEvent, cameraGap).toFloat())
        val contentWidth =
            if (expanded) maxOf(px(expandedWidthDp(screenWidthPx / density, fullWidthCard)), pillWidth) else pillWidth
        // The part of the status bar the capsule's top edge does not already clear (spec §2.4.3 point 6).
        val deadZone = if (expanded && !independent) (statusBarHeightPx - capsuleTop).coerceAtLeast(0f) else 0f
        val contentHeight = if (expanded) px(expandedContentHeightDp(listRows)) + deadZone else capsuleHeight
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
            contentOffsetYPx = idealY - y,
            contentTopInsetPx = deadZone.roundToInt(),
            cameraGapDp = cameraGap
        )
    }
}
