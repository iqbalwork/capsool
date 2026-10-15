package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.platform.accessibility.CutoutBounds
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPlacementTest {

    private val density = 3f
    private val screenWidth = 1080
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120) // 60 px hole centered at (540, 90)
    private val geometry = Geometry(widthDp = 120, heightDp = 36)
    private val margin = OverlayPlacement.MARGIN_DP * density

    @Test
    fun collapsedWindowIsCenteredOnCutout() {
        val p = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = false)
        val width = (120 * density + 2 * margin).toInt()
        assertEquals(width, p.width)
        assertEquals((36 * density + 2 * margin).toInt(), p.height)
        assertEquals(540 - width / 2, p.x)
        // capsule top = cutout center - half capsule height; window starts one margin above
        assertEquals((90 - 18 * density - margin).toInt(), p.y)
        assertEquals(0, p.contentOffsetYPx)
        assertEquals(0, p.contentOffsetXPx)
    }

    @Test
    fun offsetsMoveTheWindow() {
        val moved = OverlayPlacement.compute(screenWidth, density, cutout, geometry.copy(offsetXDp = 10, offsetYDp = 4), false)
        val base = OverlayPlacement.compute(screenWidth, density, cutout, geometry, false)
        assertEquals(base.x + 30, moved.x)
        assertEquals(base.y + 12, moved.y)
    }

    @Test
    fun windowClampedAtScreenTopShiftsContentUp() {
        val high = CutoutBounds(left = 510, top = 0, right = 570, bottom = 40)
        val p = OverlayPlacement.compute(screenWidth, density, high, geometry, false)
        assertEquals(0, p.y)
        assertEquals((20 - 18 * density - margin).toInt(), p.contentOffsetYPx)
    }

    @Test
    fun noCutoutFallsBackToTopCenter() {
        val p = OverlayPlacement.compute(screenWidth, density, null, geometry, false)
        assertEquals(540 - p.width / 2, p.x)
        assertEquals((6 * density - margin).toInt().coerceAtLeast(0), p.y)
    }

    @Test
    fun expandedWindowFitsNarrowScreens() {
        // 360 dp wide screen: card is clamped to 360 - 16 dp
        val narrow = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true)
        assertEquals(((360 - 16) * density + 2 * margin).toInt(), narrow.width)
        assertEquals((OverlayPlacement.EXPANDED_HEIGHT_DP * density + 2 * margin).toInt(), narrow.height)
    }

    @Test
    fun windowClampedAtScreenEdgeShiftsContent() {
        val p = OverlayPlacement.compute(screenWidth, density, cutout, geometry.copy(offsetXDp = -200), false)
        assertEquals(0, p.x)
        assertEquals(true, p.contentOffsetXPx < 0)
    }
}
