package com.iqbalwork.capsule

import com.iqbalwork.capsule.platform.Oem
import com.iqbalwork.capsule.platform.OemGuides
import com.iqbalwork.capsule.platform.detectOem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OemGuideTest {

    @Test
    fun subBrandsOfXiaomiAreDetectedFromManufacturerOrBrand() {
        assertEquals(Oem.XIAOMI, detectOem("Xiaomi", "Redmi"))
        assertEquals(Oem.XIAOMI, detectOem("Xiaomi", "POCO"))
        assertEquals(Oem.XIAOMI, detectOem(null, "redmi"))
    }

    @Test
    fun otherMakersAreDetectedCaseInsensitively() {
        assertEquals(Oem.SAMSUNG, detectOem("samsung", "samsung"))
        assertEquals(Oem.OPPO, detectOem("OnePlus", "OnePlus"))
        assertEquals(Oem.VIVO, detectOem("vivo", "iQOO"))
    }

    @Test
    fun realmeWinsEvenWhenItReportsOppoAsManufacturer() {
        assertEquals(Oem.REALME, detectOem("OPPO", "realme"))
        assertEquals(Oem.OPPO, detectOem("OPPO", "OPPO"))
    }

    @Test
    fun unknownOrMissingMakersFallBackToOther() {
        assertEquals(Oem.OTHER, detectOem("Google", "google"))
        assertEquals(Oem.OTHER, detectOem(null, null))
    }

    @Test
    fun everyBrandHasAGuideEndingWithTheVersionNote() {
        Oem.entries.forEach { oem ->
            val guide = OemGuides.guideFor(oem)
            assertEquals(oem, guide.oem)
            assertTrue("$oem has steps", guide.steps.size >= 2)
            assertTrue("$oem note last", guide.steps.last().startsWith("Menu names differ"))
        }
    }

    @Test
    fun theUniversalBatteryStepIsPresentForEveryBrand() {
        Oem.entries.forEach { oem ->
            assertTrue(OemGuides.guideFor(oem).steps.any { it.contains("Unrestricted") })
        }
    }
}
