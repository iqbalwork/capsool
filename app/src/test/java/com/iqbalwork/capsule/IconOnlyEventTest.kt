package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.EventDisplay
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.platform.accessibility.CutoutBounds
import com.iqbalwork.capsule.platform.accessibility.MiniContent
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

/** An icon-only pop-up event is the capsule itself: nothing grows, nothing moves. */
class IconOnlyEventTest {
    private val density = 3f
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120)
    private val geometry = Geometry(widthDp = 120, heightDp = 36, miniExtraWidthDp = 150, miniRowDp = 40, miniOffsetXDp = 25)

    private fun compute(miniEvent: Boolean, display: EventDisplay = EventDisplay.ICON_ONLY, content: MiniContent? = MiniContent(140, 60)) =
        OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, miniEvent = miniEvent, eventDisplay = display, miniContent = content)

    @Test
    fun theEventPillIsExactlyTheIdleCapsule() {
        val idle = compute(miniEvent = false)
        val event = compute(miniEvent = true)
        assertEquals(idle.width, event.width)
        assertEquals(idle.height, event.height)
        assertEquals(idle.x, event.x)
        assertEquals(idle.y, event.y)
        assertEquals(0, event.eventRowDp)
        assertEquals(0, event.cameraGapDp)
    }

    @Test
    fun theMiniShiftDoesNotMoveAnIconOnlyEventOffTheCamera() {
        assertEquals(compute(miniEvent = false).x, compute(miniEvent = true).x)
        // The other modes still honour it.
        val below = compute(miniEvent = true, display = EventDisplay.BELOW_CAMERA)
        val belowPlain = OverlayPlacement.compute(
            1080, density, cutout, geometry.copy(miniOffsetXDp = 0), expanded = false, miniEvent = true,
            eventDisplay = EventDisplay.BELOW_CAMERA, miniContent = MiniContent(140, 60)
        )
        assertEquals(belowPlain.x + (25 * density).toInt(), below.x)
    }

    @Test
    fun pillWidthIgnoresTheExtraWidthWhenIconOnly() {
        assertEquals(120, OverlayPlacement.pillWidthDp(geometry, miniEvent = true, iconOnly = true))
        assertEquals(120 + 150, OverlayPlacement.pillWidthDp(geometry, miniEvent = true, iconOnly = false))
    }

    @Test
    fun theTouchableRegionOfAnIconOnlyEventIsTheIdleOne() {
        val window = compute(miniEvent = true)
        val rect = OverlayPlacement.capsuleRect(window, density, geometry, expanded = false, miniEvent = true)
        assertEquals(120 * density, (rect.right - rect.left).toFloat(), 1f)
        assertEquals(36 * density, (rect.bottom - rect.top).toFloat(), 1f)
    }
}
