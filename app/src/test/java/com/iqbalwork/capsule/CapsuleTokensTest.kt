package com.iqbalwork.capsule

import androidx.compose.ui.graphics.Color
import com.iqbalwork.capsule.ui.theme.CapsuleTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class CapsuleTokensTest {

    @Test
    fun testMaterialDesignTokensParity() {
        assertEquals(Color(0xFF1A1615), CapsuleTokens.Surface)
        assertEquals(Color(0xFFFFB690), CapsuleTokens.Primary)
        assertEquals(Color(0xFFFF3B30), CapsuleTokens.CalibratorRed)
        assertEquals(Color(0xFF25D366), CapsuleTokens.WhatsAppBadge)
        assertEquals(Color(0xFF24A1DE), CapsuleTokens.TelegramBadge)
    }

    @Test
    fun testFruitSpringConfiguration() {
        // FruitSpring configuration from PRD Section 2.2: dampingRatio = 0.72f, stiffness = 380f
        assertEquals(0.72f, CapsuleTokens.FruitSpring.dampingRatio, 0.001f)
        assertEquals(380f, CapsuleTokens.FruitSpring.stiffness, 0.001f)
    }
}
