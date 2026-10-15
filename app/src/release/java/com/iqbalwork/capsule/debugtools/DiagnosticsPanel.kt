package com.iqbalwork.capsule.debugtools

import androidx.compose.runtime.Composable
import com.iqbalwork.capsule.core.store.Action

/** Release builds ship no simulators (spec §2.2 rule 4). */
object DiagnosticsPanel {
    const val isAvailable = false

    @Composable
    fun Content(@Suppress("UNUSED_PARAMETER") dispatch: (Action) -> Unit) = Unit
}
