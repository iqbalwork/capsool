package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.BounceLevel
import com.iqbalwork.capsule.core.settings.ExpandStyle
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.ui.theme.CapsuleMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsuleMotionTest {

    private fun motion(
        speed: Float = 1f,
        bounce: BounceLevel = BounceLevel.DEFAULT,
        style: ExpandStyle = ExpandStyle.ORGANIC,
        reduce: Boolean = false
    ) = CapsuleMotion.of(speed, bounce, style, reduce)

    @Test
    fun defaultsKeepTheLongStandingSprings() {
        val m = CapsuleMotion.of(Settings())
        assertEquals(0.72f, m.expand.dampingRatio, 0.001f)
        assertEquals(380f, m.expand.stiffness, 0.001f)
        assertEquals(0.8f, m.collapse.dampingRatio, 0.001f)
        assertEquals(260f, m.collapse.stiffness, 0.001f)
        assertTrue(m.slidesCards)
    }

    @Test
    fun defaultShrinkDelayStaysNearTheOldFixedValue() {
        assertEquals(750.0, motion().shrinkDelayMillis.toDouble(), 20.0)
    }

    @Test
    fun fasterSpeedStiffensTheSpringAndShortensTheShrinkDelay() {
        val slow = motion(speed = 0.5f)
        val fast = motion(speed = 2f)
        assertEquals(380f * 0.25f, slow.expand.stiffness, 0.01f)
        assertEquals(380f * 4f, fast.expand.stiffness, 0.01f)
        assertTrue(fast.shrinkDelayMillis < motion().shrinkDelayMillis)
        assertTrue(slow.shrinkDelayMillis > motion().shrinkDelayMillis)
    }

    @Test
    fun speedOutsideTheRangeIsClamped() {
        assertEquals(motion(speed = 2f), motion(speed = 9f))
        assertEquals(motion(speed = 0.5f), motion(speed = 0.01f))
    }

    @Test
    fun bounceLevelsGoFromNoOvershootToTheMost() {
        val damping = BounceLevel.entries.map { motion(bounce = it).expand.dampingRatio }
        assertEquals(1f, damping.first(), 0f)
        assertEquals(damping.sortedDescending(), damping)
        assertTrue(damping.last() < 0.72f)
        assertTrue(damping.all { it in 0.3f..1f })
    }

    @Test
    fun moreBounceNeedsALongerShrinkDelay() {
        assertTrue(motion(bounce = BounceLevel.MAX).shrinkDelayMillis > motion(bounce = BounceLevel.LOW).shrinkDelayMillis)
    }

    @Test
    fun rigidStyleUsesAFixedTimeEaseThatSpeedShortens() {
        val normal = motion(style = ExpandStyle.RIGID)
        assertEquals(200, normal.expand.tweenMillis)
        assertEquals(100, motion(speed = 2f, style = ExpandStyle.RIGID).expand.tweenMillis)
        // Bounce has no meaning for a tween.
        assertEquals(normal, motion(bounce = BounceLevel.MAX, style = ExpandStyle.RIGID))
    }

    @Test
    fun reduceAnimationsIgnoresSpeedAndBounceAndStopsSliding() {
        val reduced = motion(speed = 2f, bounce = BounceLevel.MAX, reduce = true)
        assertFalse(reduced.slidesCards)
        assertNotNull(reduced.expand.tweenMillis)
        assertEquals(1f, reduced.expand.dampingRatio, 0f)
        assertEquals(motion(speed = 0.5f, reduce = true), reduced)
        assertTrue(reduced.shrinkDelayMillis >= 200L)
    }

    @Test
    fun springsHaveNoTweenDuration() {
        assertNull(motion().expand.tweenMillis)
    }
}
