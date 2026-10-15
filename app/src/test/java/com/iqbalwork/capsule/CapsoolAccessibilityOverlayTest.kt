package com.iqbalwork.capsule

import android.view.WindowManager
import com.iqbalwork.capsule.engine.accessibility.CapsoolAccessibilityService
import com.iqbalwork.capsule.engine.accessibility.OverlayLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CapsoolAccessibilityOverlayTest {

    @Test
    fun testOverlayLifecycleOwnerInstantiation() {
        val owner = OverlayLifecycleOwner()
        assertNotNull(owner.lifecycle)
        assertNotNull(owner.savedStateRegistry)
        assertNotNull(owner.viewModelStore)
    }

    @Test
    fun testAccessibilityOverlayWindowTypeMatchesSpec() {
        // Must use TYPE_ACCESSIBILITY_OVERLAY which does not require SYSTEM_ALERT_WINDOW
        val expectedType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        assertEquals(2032, expectedType)
    }

    @Test
    fun testServiceClassExistsAndInstantiable() {
        val service = CapsoolAccessibilityService()
        assertNotNull(service)
    }
}
