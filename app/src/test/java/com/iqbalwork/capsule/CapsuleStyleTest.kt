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

class AppearanceTest {
    private fun s(
        bg: Long? = null, night: Long? = null, border: Int = 0, shadow: Int = 0,
        outline: Boolean = false, shadowLight: Boolean = false, alpha: Float = 1f
    ) = com.iqbalwork.capsule.core.settings.Settings(
        customBackground = bg, customBackgroundNight = night, borderDp = border, shadowDp = shadow,
        outlineInDark = outline, shadowInLight = shadowLight, shellAlpha = alpha
    )

    @Test
    fun defaultsAreTheOpaqueBlackShellInBothModes() {
        assertEquals(0xFF000000L, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(), night = false).shellArgb)
        assertEquals(0xFF000000L, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(), night = true).shellArgb)
    }

    @Test
    fun nightBackgroundOnlyAppliesAtNightAndFallsBackToTheDayOne() {
        val day = 0xFF1B2A41L
        val night = 0xFF3B1F2B
        val both = s(bg = day, night = night)
        assertEquals(day, com.iqbalwork.capsule.ui.theme.resolveAppearance(both, night = false).shellArgb)
        assertEquals(night, com.iqbalwork.capsule.ui.theme.resolveAppearance(both, night = true).shellArgb)
        assertEquals(day, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(bg = day), night = true).shellArgb)
    }

    @Test
    fun transparencyOnlyChangesTheAlphaChannel() {
        val resolved = com.iqbalwork.capsule.ui.theme.resolveAppearance(s(bg = 0xFF1B2A41L, alpha = 0.5f), night = false)
        assertEquals(0x1B2A41L, resolved.shellArgb and 0xFFFFFFL)
        assertEquals(127L, resolved.shellArgb ushr 24)
    }

    @Test
    fun outlineInDarkAddsAtLeastOneDpOnlyAtNight() {
        assertEquals(1, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(outline = true), night = true).borderDp)
        assertEquals(0, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(outline = true), night = false).borderDp)
        // It never reduces a border the user already chose.
        assertEquals(3, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(border = 3, outline = true), night = true).borderDp)
    }

    @Test
    fun shadowInLightAddsAtLeastThreeDpOnlyInTheDay() {
        assertEquals(3, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(shadowLight = true), night = false).shadowDp)
        assertEquals(0, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(shadowLight = true), night = true).shadowDp)
        assertEquals(6, com.iqbalwork.capsule.ui.theme.resolveAppearance(s(shadow = 6, shadowLight = true), night = false).shadowDp)
    }

    @Test
    fun storedValuesAreClamped() {
        val prefs = androidx.datastore.preferences.core.mutablePreferencesOf(
            androidx.datastore.preferences.core.floatPreferencesKey("appearance.alpha") to 0f
        )
        assertEquals(
            com.iqbalwork.capsule.core.settings.MIN_SHELL_ALPHA,
            com.iqbalwork.capsule.core.settings.SettingsMapper.read(prefs).shellAlpha,
            0f
        )
    }
}
