package com.iqbalwork.capsule.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.iqbalwork.capsule.core.settings.BounceLevel
import com.iqbalwork.capsule.core.settings.ExpandStyle
import com.iqbalwork.capsule.core.settings.MAX_ANIMATION_SPEED
import com.iqbalwork.capsule.core.settings.MIN_ANIMATION_SPEED
import com.iqbalwork.capsule.core.settings.Settings
import kotlin.math.ln
import kotlin.math.sqrt

/** One animation curve: a spring, or a fixed-time ease when [tweenMillis] is set. */
data class MotionCurve(val dampingRatio: Float, val stiffness: Float, val tweenMillis: Int? = null) {
    fun <T> spec(): FiniteAnimationSpec<T> =
        if (tweenMillis != null) tween(tweenMillis, easing = FastOutSlowInEasing)
        else spring(dampingRatio = dampingRatio, stiffness = stiffness)

    /** Time until the motion is within ~2% of its target. */
    val settleMillis: Long
        get() {
            if (tweenMillis != null) return tweenMillis.toLong()
            val omega = sqrt(stiffness)
            val seconds = if (dampingRatio < 1f) SETTLE_LN / (dampingRatio * omega) else CRITICAL_SETTLE / omega
            return (seconds * 1000f).toLong()
        }

    private companion object {
        val SETTLE_LN = ln(50f)
        const val CRITICAL_SETTLE = 6.64f
    }
}

/**
 * F-82: every curve the capsule animates with, derived from the user's animation settings.
 * Pure data on purpose, so the values can be unit-tested without a Compose runtime.
 */
data class CapsuleMotion(
    /** Size and corner while the card opens. */
    val expand: MotionCurve,
    /** Size and corner while the card folds back into the pill. */
    val collapse: MotionCurve,
    /** Carousel page changes. */
    val content: MotionCurve,
    /** False under reduce-animations: pages cross-fade instead of sliding. */
    val slidesCards: Boolean
) {
    /**
     * How long the overlay window must stay card-sized after a collapse: shrinking earlier clips
     * the still-moving edge. 2.5x the settle time reproduces the long-standing 750 ms at defaults.
     */
    val shrinkDelayMillis: Long
        get() = (collapse.settleMillis * SHRINK_SAFETY).toLong().coerceIn(MIN_SHRINK_DELAY_MS, MAX_SHRINK_DELAY_MS)

    companion object {
        private const val BASE_EXPAND_DAMPING = 0.72f
        private const val BASE_EXPAND_STIFFNESS = 380f
        private const val BASE_COLLAPSE_DAMPING = 0.8f
        private const val BASE_COLLAPSE_STIFFNESS = 260f
        private const val BASE_CONTENT_DAMPING = 0.72f
        private const val BASE_CONTENT_STIFFNESS = 380f
        private const val RIGID_MILLIS = 200
        private const val REDUCED_MILLIS = 90
        private const val SHRINK_SAFETY = 2.5f
        private const val MIN_SHRINK_DELAY_MS = 200L
        private const val MAX_SHRINK_DELAY_MS = 3_000L
        private const val MIN_DAMPING = 0.3f

        /** Added to a spring's base damping: lower = more overshoot. NONE is handled separately. */
        private fun dampingOffset(bounce: BounceLevel) = when (bounce) {
            BounceLevel.NONE -> 0f
            BounceLevel.LOW -> 0.13f
            BounceLevel.DEFAULT -> 0f
            BounceLevel.HIGH -> -0.12f
            BounceLevel.MAX -> -0.27f
        }

        fun of(
            speed: Float,
            bounce: BounceLevel,
            style: ExpandStyle,
            reduce: Boolean
        ): CapsuleMotion {
            val s = speed.coerceIn(MIN_ANIMATION_SPEED, MAX_ANIMATION_SPEED)
            if (reduce) {
                // Reduced motion ignores speed and bounce: short, non-overshooting, no sliding.
                val curve = MotionCurve(1f, 0f, REDUCED_MILLIS)
                return CapsuleMotion(curve, curve, curve, slidesCards = false)
            }
            if (style == ExpandStyle.RIGID) {
                val millis = (RIGID_MILLIS / s).toInt().coerceAtLeast(1)
                val curve = MotionCurve(1f, 0f, millis)
                return CapsuleMotion(curve, curve, curve, slidesCards = true)
            }
            // A spring's frequency goes with sqrt(stiffness), so stiffness scales with speed^2.
            val stiffnessScale = s * s
            fun spring(baseDamping: Float, baseStiffness: Float): MotionCurve {
                val damping = if (bounce == BounceLevel.NONE) 1f
                else (baseDamping + dampingOffset(bounce)).coerceIn(MIN_DAMPING, 1f)
                return MotionCurve(damping, baseStiffness * stiffnessScale)
            }
            return CapsuleMotion(
                expand = spring(BASE_EXPAND_DAMPING, BASE_EXPAND_STIFFNESS),
                collapse = spring(BASE_COLLAPSE_DAMPING, BASE_COLLAPSE_STIFFNESS),
                content = spring(BASE_CONTENT_DAMPING, BASE_CONTENT_STIFFNESS),
                slidesCards = true
            )
        }

        fun of(settings: Settings): CapsuleMotion =
            of(settings.animationSpeed, settings.bounce, settings.expandStyle, settings.reduceAnimations)
    }
}
