package com.iqbalwork.capsule

import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.CapsuleShapeStyle
import com.iqbalwork.capsule.ui.theme.collapsedCornerDp
import com.iqbalwork.capsule.ui.theme.luminance
import com.iqbalwork.capsule.ui.theme.readableShellColor
import com.iqbalwork.capsule.ui.theme.squircleCorner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsuleStyleTest {

    @Test
    fun pillIgnoresTheCornerSettingAndRoundsOffCompletely() {
        assertEquals(18.dp, collapsedCornerDp(CapsuleShapeStyle.PILL, cornerDp = 4, heightDp = 36))
    }

    @Test
    fun otherShapesKeepTheConfiguredCornerRadius() {
        assertEquals(4.dp, collapsedCornerDp(CapsuleShapeStyle.ROUNDED_RECT, cornerDp = 4, heightDp = 36))
        assertEquals(4.dp, collapsedCornerDp(CapsuleShapeStyle.SQUIRCLE, cornerDp = 4, heightDp = 36))
    }

    @Test
    fun squircleCornerJoinsBothStraightEdgesExactly() {
        val r = 20f
        val arc = squircleCorner(r, steps = 12)
        assertEquals(13, arc.size)
        assertEquals(r to 0f, arc.first())
        assertEquals(0f to r, arc.last())
    }

    @Test
    fun squircleCornerBulgesOutsideACircleOfTheSameRadius() {
        val r = 20f
        val mid = squircleCorner(r, steps = 12)[6]
        // On a circle the 45-degree point sits at r * 0.707 from each axis; a squircle is further out.
        assertTrue(mid.first > r * 0.7071f && mid.second > r * 0.7071f)
        assertTrue(mid.first <= r && mid.second <= r)
    }

    @Test
    fun darkColorsAreKeptExactlyAndMadeOpaque() {
        assertEquals(0xFF1B2A41L, readableShellColor(0xFF1B2A41L))
        assertEquals(0xFF1B2A41L, readableShellColor(0x801B2A41L))
    }

    @Test
    fun lightColorsAreDarkenedUntilWhiteTextIsReadable() {
        val result = readableShellColor(0xFFFFFFFFL)
        assertTrue(luminance(result) <= 0.3f)
        assertTrue(luminance(result) > 0f)
        val yellow = readableShellColor(0xFFFFEB3BL)
        assertTrue(luminance(yellow) <= 0.3f)
        // Hue survives: red and green stay above blue.
        val red = (yellow shr 16) and 0xFF
        val blue = yellow and 0xFF
        assertTrue(red > blue)
    }
}
