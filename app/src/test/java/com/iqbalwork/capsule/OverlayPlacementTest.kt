package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.EventDisplay
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.PlacementMode
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

    @Test
    fun touchableRegionCoversPillWhenCollapsedAndCardWhenExpanded() {
        val window = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true)
        val pill = OverlayPlacement.capsuleRect(window, density, geometry, expanded = false)
        assertEquals((120 * density).toInt(), pill.right - pill.left)
        assertEquals((36 * density).toInt(), pill.bottom - pill.top)
        assertEquals(window.width / 2, (pill.left + pill.right) / 2)
        assertEquals(margin.toInt(), pill.top)

        val card = OverlayPlacement.capsuleRect(window, density, geometry, expanded = true)
        assertEquals(margin.toInt(), card.left)
        assertEquals(window.width - margin.toInt(), card.right)
        assertEquals((margin + OverlayPlacement.EXPANDED_HEIGHT_DP * density).toInt(), card.bottom)
    }

    // --- LIST mode panel height (M6c-3b) ---

    @Test
    fun oneListRowMakesAPanelTallerThanTheCollapsedPill() {
        val rows = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true, listRows = 1)
        assertEquals((OverlayPlacement.LIST_ROW_DP * density + 2 * margin).toInt(), rows.height)
        // The panel still hangs from the cutout, so its top edge does not move.
        assertEquals((90 - 18 * density - margin).toInt(), rows.y)
    }

    @Test
    fun twoListRowsAreTwiceAsTallAsOne() {
        val one = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true, listRows = 1)
        val two = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true, listRows = 2)
        assertEquals((OverlayPlacement.LIST_ROW_DP * density).toInt(), two.height - one.height)
    }

    @Test
    fun listHeightStopsGrowingAtTheRowCap() {
        val atCap = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true, listRows = OverlayPlacement.MAX_LIST_ROWS)
        val past = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true, listRows = OverlayPlacement.MAX_LIST_ROWS + 6)
        // The panel scrolls instead of growing down over the screen.
        assertEquals(atCap.height, past.height)
    }

    @Test
    fun listRowsZeroKeepsTheNormalExpandedPanel() {
        val without = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true)
        val zero = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = true, listRows = 0)
        assertEquals(without.height, zero.height)
        assertEquals((OverlayPlacement.EXPANDED_HEIGHT_DP * density + 2 * margin).toInt(), zero.height)
    }

    @Test
    fun listRowsAreIgnoredWhileThePillIsCollapsed() {
        val collapsed = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = false, listRows = 2)
        val base = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = false)
        assertEquals(base.height, collapsed.height)
    }

    // --- F-09 ---

    @Test
    fun independentModeSitsUnderTheStatusBarWhateverTheCutoutIs() {
        val statusBar = 90 // px
        val p = OverlayPlacement.compute(
            screenWidth, density, cutout.copy(left = 100, right = 160), geometry, expanded = false,
            mode = PlacementMode.INDEPENDENT, statusBarHeightPx = statusBar
        )
        val width = (120 * density + 2 * margin).toInt()
        // Centered on the screen, not on the (off-center) camera.
        assertEquals(screenWidth / 2 - width / 2, p.x)
        // Capsule top = status bar + gap; the window starts one margin above that.
        assertEquals((statusBar + OverlayPlacement.INDEPENDENT_GAP_DP * density - margin).toInt(), p.y)
    }

    @Test
    fun independentModeStillHonoursTheUsersOffsets() {
        val moved = geometry.copy(offsetXDp = 10, offsetYDp = 5)
        val base = OverlayPlacement.compute(
            screenWidth, density, null, geometry, expanded = false, mode = PlacementMode.INDEPENDENT, statusBarHeightPx = 90
        )
        val shifted = OverlayPlacement.compute(
            screenWidth, density, null, moved, expanded = false, mode = PlacementMode.INDEPENDENT, statusBarHeightPx = 90
        )
        assertEquals(base.x + (10 * density).toInt(), shifted.x)
        assertEquals(base.y + (5 * density).toInt(), shifted.y)
    }

    @Test
    fun cameraModeIgnoresTheStatusBarHeight() {
        val a = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = false)
        val b = OverlayPlacement.compute(screenWidth, density, cutout, geometry, expanded = false, statusBarHeightPx = 500)
        assertEquals(a, b)
    }

    @Test
    fun fullWidthCardSpansTheScreenLessTheGuttersWhileCompactStaysCapped() {
        val wide = 1440 // px, 480 dp: wider than the compact cap
        val wideDp = wide / density
        assertEquals(OverlayPlacement.EXPANDED_MAX_WIDTH_DP.toFloat(), OverlayPlacement.expandedWidthDp(wideDp), 0.01f)
        assertEquals(wideDp - 16, OverlayPlacement.expandedWidthDp(wideDp, full = true), 0.01f)
        val compact = OverlayPlacement.compute(wide, density, cutout, geometry, expanded = true)
        val full = OverlayPlacement.compute(wide, density, cutout, geometry, expanded = true, fullWidthCard = true)
        assertEquals(OverlayPlacement.EXPANDED_MAX_WIDTH_DP * density + 2 * margin, compact.width.toFloat(), 1f)
        assertEquals((wideDp - 16) * density + 2 * margin, full.width.toFloat(), 1f)
    }

    @Test
    fun fullWidthIsNoNarrowerThanCompactOnSmallScreens() {
        val narrow = 330f
        assertEquals(OverlayPlacement.expandedWidthDp(narrow), OverlayPlacement.expandedWidthDp(narrow, full = true), 0.01f)
    }
}

class DeadZoneTest {
    private val density = 3f
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120) // capsule top = 90 - 54 = 36 px
    private val geometry = Geometry(widthDp = 120, heightDp = 36)

    @Test
    fun anOpenedCardInCameraModeStartsBelowTheStatusBar() {
        val p = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true, statusBarHeightPx = 110)
        assertEquals(110 - 36, p.contentTopInsetPx)
        val plain = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true, statusBarHeightPx = 0)
        assertEquals(0, plain.contentTopInsetPx)
        assertEquals(plain.height + 74, p.height)
    }

    @Test
    fun noInsetWhenTheCapsuleAlreadyClearsTheStatusBar() {
        val p = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true, statusBarHeightPx = 20)
        assertEquals(0, p.contentTopInsetPx)
    }

    @Test
    fun noInsetForACollapsedCapsuleOrIndependentMode() {
        assertEquals(
            0,
            OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, statusBarHeightPx = 110).contentTopInsetPx
        )
        assertEquals(
            0,
            OverlayPlacement.compute(
                1080, density, cutout, geometry, expanded = true, statusBarHeightPx = 110,
                mode = com.iqbalwork.capsule.core.settings.PlacementMode.INDEPENDENT
            ).contentTopInsetPx
        )
    }

    @Test
    fun theTouchableRegionOfAnOpenCardCoversTheInsetToo() {
        val window = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true, statusBarHeightPx = 110)
        val rect = OverlayPlacement.capsuleRect(window, density, geometry, expanded = true)
        val margin = (OverlayPlacement.MARGIN_DP * density).toInt()
        assertEquals(window.height - 2 * margin, rect.bottom - rect.top)
    }
}

class CameraGapTest {
    private val density = 3f
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120) // 60 px = 20 dp hole
    private val geometry = Geometry(widthDp = 120, heightDp = 36)

    @Test
    fun theGapIsTheHoleAndALittleRoomEachSide() {
        assertEquals(20 + 2 * OverlayPlacement.CAMERA_GAP_PADDING_DP, OverlayPlacement.cameraGapDp(60, density))
        // Rounds up, so a hole of 20.3 dp is never under-covered.
        assertEquals(21 + 2 * OverlayPlacement.CAMERA_GAP_PADDING_DP, OverlayPlacement.cameraGapDp(61, density))
        assertEquals(0, OverlayPlacement.cameraGapDp(0, density))
    }

    @Test
    fun aMiniEventPillIsWiderBySplitRoomButOnlyWhenThereIsAHole() {
        val gap = OverlayPlacement.cameraGapDp(60, density)
        assertEquals(120 + OverlayPlacement.MINI_EVENT_EXTRA_WIDTH_DP, OverlayPlacement.pillWidthDp(geometry, miniEvent = true))
        assertEquals(
            120 + OverlayPlacement.MINI_EVENT_EXTRA_WIDTH_DP + gap + OverlayPlacement.SPLIT_EXTRA_WIDTH_DP,
            OverlayPlacement.pillWidthDp(geometry, miniEvent = true, cameraGapDp = gap)
        )
        // A pill that is not showing an event keeps its calibrated width whatever the hole.
        assertEquals(120, OverlayPlacement.pillWidthDp(geometry, miniEvent = false, cameraGapDp = gap))
    }

    @Test
    fun placementReportsTheGapForTheHoleItSitsOver() {
        val p = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, miniEvent = true, eventDisplay = EventDisplay.SPLIT)
        assertEquals(OverlayPlacement.cameraGapDp(60, density), p.cameraGapDp)
        val margin = OverlayPlacement.MARGIN_DP * density
        assertEquals((OverlayPlacement.pillWidthDp(geometry, true, p.cameraGapDp) * density + 2 * margin).toInt(), p.width)
    }

    @Test
    fun noGapWithoutAHoleOrInIndependentMode() {
        assertEquals(0, OverlayPlacement.compute(1080, density, null, geometry, expanded = false, miniEvent = true, eventDisplay = EventDisplay.SPLIT).cameraGapDp)
        assertEquals(
            0,
            OverlayPlacement.compute(
                1080, density, cutout, geometry, expanded = false, miniEvent = true,
                mode = com.iqbalwork.capsule.core.settings.PlacementMode.INDEPENDENT, statusBarHeightPx = 90,
                eventDisplay = EventDisplay.SPLIT
            ).cameraGapDp
        )
    }

    @Test
    fun theTouchableRegionMatchesTheWiderPill() {
        val window = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true, miniEvent = true, eventDisplay = EventDisplay.SPLIT)
        val rect = OverlayPlacement.capsuleRect(window, density, geometry, expanded = false, miniEvent = true)
        val expected = OverlayPlacement.pillWidthDp(geometry, true, window.cameraGapDp) * density
        assertEquals(expected, (rect.right - rect.left).toFloat(), 1f)
    }
}

class BelowCameraTest {
    private val density = 3f
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120)
    private val geometry = Geometry(widthDp = 120, heightDp = 36)
    private val margin = OverlayPlacement.MARGIN_DP * density

    @Test
    fun belowCameraIsTheDefaultAndAddsARowUnderTheHole() {
        val plain = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false)
        val event = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, miniEvent = true)
        assertEquals(OverlayPlacement.EVENT_ROW_DP, event.eventRowDp)
        assertEquals(0, event.cameraGapDp)
        assertEquals(plain.height + (OverlayPlacement.EVENT_ROW_DP * density).toInt(), event.height)
        // The top edge does not move: the pill only grows downward, so the hole stays inside it.
        assertEquals(plain.y, event.y)
    }

    @Test
    fun noExtraRowWithoutAnEventOrWithoutAHoleOrInIndependentMode() {
        assertEquals(0, OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false).eventRowDp)
        assertEquals(0, OverlayPlacement.compute(1080, density, null, geometry, expanded = false, miniEvent = true).eventRowDp)
        assertEquals(
            0,
            OverlayPlacement.compute(
                1080, density, cutout, geometry, expanded = false, miniEvent = true,
                mode = com.iqbalwork.capsule.core.settings.PlacementMode.INDEPENDENT, statusBarHeightPx = 90
            ).eventRowDp
        )
    }

    @Test
    fun splitModeKeepsTheHeightAndTheRowIsNotAdded() {
        val p = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, miniEvent = true, eventDisplay = EventDisplay.SPLIT)
        assertEquals(0, p.eventRowDp)
        assertEquals((36 * density + 2 * margin).toInt(), p.height)
    }

    @Test
    fun belowCameraKeepsTheNormalEventWidth() {
        val p = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, miniEvent = true)
        assertEquals(((120 + OverlayPlacement.MINI_EVENT_EXTRA_WIDTH_DP) * density + 2 * margin).toInt(), p.width)
    }

    @Test
    fun theTouchableRegionCoversTheTallerPill() {
        val window = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = true, miniEvent = true)
        val rect = OverlayPlacement.capsuleRect(window, density, geometry, expanded = false, miniEvent = true)
        assertEquals(((36 + OverlayPlacement.EVENT_ROW_DP) * density), (rect.bottom - rect.top).toFloat(), 1f)
        val idle = OverlayPlacement.capsuleRect(window, density, geometry, expanded = false, miniEvent = false)
        assertEquals(36 * density, (idle.bottom - idle.top).toFloat(), 1f)
    }
}
