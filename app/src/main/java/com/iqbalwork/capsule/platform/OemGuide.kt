package com.iqbalwork.capsule.platform

import androidx.annotation.StringRes
import com.iqbalwork.capsule.R
import java.util.Locale

/** Phone makers whose battery management is aggressive enough to stop the accessibility service (F-92). */
enum class Oem(@StringRes val labelRes: Int) {
    XIAOMI(R.string.oem_xiaomi),
    SAMSUNG(R.string.oem_samsung),
    OPPO(R.string.oem_oppo),
    VIVO(R.string.oem_vivo),
    REALME(R.string.oem_realme),
    OTHER(R.string.oem_other)
}

/** How to keep Capsool alive on one [Oem]; [steps] are string resources in the order to follow them. */
data class OemGuide(val oem: Oem, @StringRes val steps: List<Int>)

/**
 * Picks the guide to show first. Sub-brands report their parent as manufacturer (POCO and Redmi are
 * "Xiaomi") or only as brand, so both fields are checked; Realme is matched before Oppo since a
 * Realme phone may report either.
 */
fun detectOem(manufacturer: String?, brand: String?): Oem {
    val names = listOfNotNull(manufacturer, brand).map { it.lowercase(Locale.ROOT) }
    fun has(vararg keys: String) = names.any { name -> keys.any { name.contains(it) } }
    return when {
        has("xiaomi", "redmi", "poco") -> Oem.XIAOMI
        has("samsung") -> Oem.SAMSUNG
        has("realme") -> Oem.REALME
        has("oppo", "oneplus") -> Oem.OPPO
        has("vivo", "iqoo") -> Oem.VIVO
        else -> Oem.OTHER
    }
}

object OemGuides {
    /** Closes every guide: menu names change between software versions. */
    @StringRes
    val NOTE: Int = R.string.oem_note_names_vary

    /** Applies to every phone, so it comes after the brand-specific steps. */
    @StringRes
    val COMMON_BATTERY_STEP: Int = R.string.oem_step_common_battery

    private val guides: Map<Oem, List<Int>> = mapOf(
        Oem.XIAOMI to listOf(R.string.oem_xiaomi_1, R.string.oem_xiaomi_2, R.string.oem_xiaomi_3),
        Oem.SAMSUNG to listOf(R.string.oem_samsung_1, R.string.oem_samsung_2),
        Oem.OPPO to listOf(R.string.oem_oppo_1, R.string.oem_oppo_2, R.string.oem_oppo_3),
        Oem.VIVO to listOf(R.string.oem_vivo_1, R.string.oem_vivo_2, R.string.oem_vivo_3),
        Oem.REALME to listOf(R.string.oem_realme_1, R.string.oem_realme_2, R.string.oem_realme_3),
        Oem.OTHER to listOf(R.string.oem_other_1)
    )

    /** The brand-specific steps first, then the one that applies to every phone, then the note. */
    fun guideFor(oem: Oem): OemGuide = OemGuide(oem, guides.getValue(oem) + COMMON_BATTERY_STEP + NOTE)
}
