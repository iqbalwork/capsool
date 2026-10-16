package com.iqbalwork.capsule.platform.accessibility

import com.iqbalwork.capsule.core.settings.EventLeftShape
import com.iqbalwork.capsule.core.settings.EventRightShape
import com.iqbalwork.capsule.core.settings.EventStyle
import com.iqbalwork.capsule.model.MiniEvent
import kotlin.math.ceil
import kotlin.math.max

/** How much room a pop-up event's two sides need, in dp: icon and title on the left, value and ring on the right. */
data class MiniContent(val leftDp: Int, val rightDp: Int)

/**
 * Estimates how wide an event's text needs its pill to be, so a short event ("Wi-Fi connected") is not
 * stretched to the width a long one needs. It is an estimate (no text is measured before the window is
 * placed), rounded up, and the pill is never wider than the user's "extra width" setting allows, so a
 * title that still does not fit is cut off with an ellipsis rather than growing the pill.
 */
object MiniEventWidth {
    /**
     * Average width of one character of the pill's semibold label text. Measured on a device: 7 dp cut
     * "Charger removed" short ("Charger remov..."), so this errs on the wide side.
     */
    private const val CHAR_DP = 7.6f
    private const val BADGE_DP = 32 // 24 dp circle and the 8 dp after it
    private const val VALUE_GAP_DP = 6
    private const val RING_DP = 24 // 6 dp and the 18 dp ring
    private const val SIDE_PADDING_DP = 14 // 7 dp each side
    private const val MIDDLE_GAP_DP = 8

    fun of(event: MiniEvent, style: EventStyle = EventStyle.Default): MiniContent {
        val badge = if (style.left == EventLeftShape.NONE) 0 else BADGE_DP
        val left = badge + ceil(event.title.length * CHAR_DP).toInt()
        val right = if (style.right == EventRightShape.NONE) {
            0
        } else {
            val value = if (event.subtitle.isNotEmpty()) VALUE_GAP_DP + ceil(event.subtitle.length * CHAR_DP).toInt() else 0
            val ring = if (event.level != null && style.right == EventRightShape.AUTO) RING_DP else 0
            value + ring
        }
        return MiniContent(left, right)
    }

    /** Width the pill needs: both sides in one row, or two equal halves around a camera gap of [gapDp]. */
    fun wantedDp(content: MiniContent, gapDp: Int): Int =
        if (gapDp > 0) 2 * max(content.leftDp, content.rightDp) + gapDp + SIDE_PADDING_DP
        else content.leftDp + content.rightDp + (if (content.rightDp > 0) MIDDLE_GAP_DP else 0) + SIDE_PADDING_DP
}
