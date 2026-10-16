package com.iqbalwork.capsule.platform

import java.util.Locale

/** Phone makers whose battery management is aggressive enough to stop the accessibility service (F-92). */
enum class Oem(val displayName: String) {
    XIAOMI("Xiaomi / Redmi / POCO"),
    SAMSUNG("Samsung"),
    OPPO("Oppo / OnePlus"),
    VIVO("Vivo / iQOO"),
    REALME("Realme"),
    OTHER("Other (Pixel and stock Android)")
}

/** How to keep Capsool alive on one [Oem]; [steps] are in the order to follow them. */
data class OemGuide(val oem: Oem, val steps: List<String>)

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

private const val NAMES_VARY = "Menu names differ a little between software versions."

object OemGuides {
    private val common = listOf(
        "Open Capsool's App info, then Battery, and choose Unrestricted (sometimes called \"No restrictions\" or \"Allow background activity\")."
    )

    private val guides: Map<Oem, List<String>> = mapOf(
        Oem.XIAOMI to listOf(
            "Settings > Apps > Manage apps > Capsool > Autostart: turn it on.",
            "On the same screen, open Battery saver and choose No restrictions.",
            "Open the recent apps screen and lock the Capsool card (long press, then the padlock) so \"clear all\" skips it.",
            NAMES_VARY
        ),
        Oem.SAMSUNG to listOf(
            "Settings > Battery > Background usage limits: turn off \"Put unused apps to sleep\".",
            "On that screen remove Capsool from \"Sleeping apps\" and \"Deep sleeping apps\", and add it to \"Never sleeping apps\".",
            NAMES_VARY
        ),
        Oem.OPPO to listOf(
            "Capsool's App info > Battery usage: turn on \"Allow background activity\" and \"Allow auto-launch\".",
            "Settings > Battery: turn off any option that optimises or freezes apps in the background.",
            "Lock the Capsool card in the recent apps screen so \"clear all\" skips it.",
            NAMES_VARY
        ),
        Oem.VIVO to listOf(
            "Settings > Battery > Background power consumption: set Capsool to \"Allow high background power consumption\".",
            "Settings > Apps > Autostart (or iManager > App manager): turn Capsool on.",
            "Lock the Capsool card in the recent apps screen so \"clear all\" skips it.",
            NAMES_VARY
        ),
        Oem.REALME to listOf(
            "Capsool's App info > Battery usage: turn on \"Allow background activity\" and \"Allow auto-launch\".",
            "Settings > Battery: turn off app freezing and sleep standby optimisation.",
            "Lock the Capsool card in the recent apps screen so \"clear all\" skips it.",
            NAMES_VARY
        ),
        Oem.OTHER to listOf(
            "If Capsool stops after a while, look for any battery option that limits or sleeps background apps and exempt Capsool.",
            NAMES_VARY
        )
    )

    /** The brand-specific steps first, then the ones that apply to every phone. */
    fun guideFor(oem: Oem): OemGuide = OemGuide(oem, guides.getValue(oem).let { specific ->
        if (oem == Oem.OTHER) common + specific else specific.dropLast(1) + common + specific.last()
    })
}
