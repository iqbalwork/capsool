package com.iqbalf.capsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iqbalwork.capsule.engine.state.CapsuleStateEngine

/**
 * Parity forwarding facade for com.iqbalf.capsule.ui.CapsuleRootView.
 */
@Composable
fun CapsuleRootView(
    engine: CapsuleStateEngine = CapsuleStateEngine.instance,
    modifier: Modifier = Modifier
) {
    com.iqbalwork.capsule.ui.CapsuleRootView(
        engine = engine,
        modifier = modifier
    )
}
