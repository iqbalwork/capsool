package com.iqbalwork.capsule.platform.controls

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The system brightness slider is not linear: Android maps its position through a gamma
 * curve (the same Hybrid Log-Gamma constants as AOSP BrightnessUtils), so 50% on our slider
 * must mean what 50% means on the system one (spec §2.5.5). Pure, unit-tested.
 */
object BrightnessCurve {
    private const val R = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f

    /** Raw setting value ([min]..[max]) → slider position 0..1. */
    fun positionFor(value: Int, min: Int, max: Int): Float {
        if (max <= min) return 0f
        val normalized = ((value - min).toFloat() / (max - min)).coerceIn(0f, 1f) * 12f
        val position = if (normalized <= 1f) sqrt(normalized) * R else A * ln(normalized - B) + C
        return position.coerceIn(0f, 1f)
    }

    /** Slider position 0..1 → raw setting value ([min]..[max]). */
    fun valueFor(position: Float, min: Int, max: Int): Int {
        val p = position.coerceIn(0f, 1f)
        val linear = if (p <= R) (p / R) * (p / R) else exp((p - C) / A) + B
        val fraction = (linear.coerceIn(0f, 12f) / 12f)
        return (min + fraction * (max - min)).roundToInt().coerceIn(min, max)
    }
}
