package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.EventLeftShape
import com.iqbalwork.capsule.core.settings.EventRightShape
import com.iqbalwork.capsule.core.settings.EventStyle
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.platform.accessibility.CutoutBounds
import com.iqbalwork.capsule.platform.accessibility.MiniContent
import com.iqbalwork.capsule.platform.accessibility.MiniEventWidth
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The event pill is only as wide as its text needs, within the user's allowance. */
class MiniEventWidthTest {
    private val geometry = Geometry(widthDp = 120, miniExtraWidthDp = 110)
    private val density = 3f
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120)

    private fun event(title: String, subtitle: String = "", level: Int? = null) =
        MiniEvent(1L, MiniEventType.WIFI_CONNECTED, title, subtitle, level)

    @Test
    fun aLongerTitleNeedsMoreRoom() {
        assertTrue(MiniEventWidth.of(event("Charger removed")).leftDp > MiniEventWidth.of(event("Wi-Fi")).leftDp)
    }

    @Test
    fun theValueAndRingOnlyCountWhenTheyAreShown() {
        val full = MiniEventWidth.of(event("Charging", "70%", level = 70))
        assertTrue(full.rightDp > MiniEventWidth.of(event("Charging", "70%")).rightDp)
        assertEquals(0, MiniEventWidth.of(event("Charging")).rightDp)
        assertEquals(0, MiniEventWidth.of(event("Charging", "70%", 70), EventStyle(right = EventRightShape.NONE)).rightDp)
        // The "text only" side drops the ring but keeps the value.
        val text = MiniEventWidth.of(event("Charging", "70%", 70), EventStyle(right = EventRightShape.TEXT))
        assertTrue(text.rightDp > 0 && text.rightDp < full.rightDp)
    }

    @Test
    fun noBadgeNeedsLessRoomOnTheLeft() {
        val withBadge = MiniEventWidth.of(event("Wi-Fi"))
        val without = MiniEventWidth.of(event("Wi-Fi"), EventStyle(left = EventLeftShape.NONE))
        assertTrue(without.leftDp < withBadge.leftDp)
    }

    @Test
    fun aShortEventGetsAShorterPillThanALongOne() {
        val short = OverlayPlacement.pillWidthDp(geometry, true, content = MiniEventWidth.of(event("Wi-Fi")))
        val long = OverlayPlacement.pillWidthDp(geometry, true, content = MiniEventWidth.of(event("Charger removed", "100%", 100)))
        assertTrue("short $short < long $long", short < long)
    }

    @Test
    fun theWidthStaysBetweenTheCapsuleAndTheUsersAllowance() {
        val tiny = OverlayPlacement.pillWidthDp(geometry, true, content = MiniContent(0, 0))
        assertEquals(120, tiny) // never narrower than the capsule over the camera hole
        val huge = OverlayPlacement.pillWidthDp(geometry, true, content = MiniContent(500, 500))
        assertEquals(120 + 110, huge) // never wider than "extra width" allows
        assertEquals(120 + 110, OverlayPlacement.pillWidthDp(geometry, true)) // unknown content: the full allowance
    }

    @Test
    fun splitNeedsTwoEqualHalvesAroundTheGap() {
        val content = MiniContent(leftDp = 90, rightDp = 40)
        assertEquals(2 * 90 + 28 + 14, MiniEventWidth.wantedDp(content, gapDp = 28))
        assertEquals(90 + 40 + 8 + 14, MiniEventWidth.wantedDp(content, gapDp = 0))
    }

    @Test
    fun placementUsesTheContentWidthAndReportsIt() {
        val content = MiniEventWidth.of(event("Wi-Fi"))
        val p = OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false, miniEvent = true, miniContent = content)
        val expected = OverlayPlacement.pillWidthDp(geometry, true, 0, content)
        assertEquals(expected, p.miniPillWidthDp)
        val margin = OverlayPlacement.MARGIN_DP * density
        assertEquals((expected * density + 2 * margin).toInt(), p.width)
        val rect = OverlayPlacement.capsuleRect(p, density, geometry, expanded = false, miniEvent = true)
        assertEquals(expected * density, (rect.right - rect.left).toFloat(), 1f)
        assertEquals(0, OverlayPlacement.compute(1080, density, cutout, geometry, expanded = false).miniPillWidthDp)
    }
}
