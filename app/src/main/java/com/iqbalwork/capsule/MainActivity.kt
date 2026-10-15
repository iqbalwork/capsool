package com.iqbalwork.capsule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbalwork.capsule.ui.companion.CompanionApp
import com.iqbalwork.capsule.ui.theme.CapsuleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = appGraph

        setContent {
            val state by graph.store.state.collectAsStateWithLifecycle()
            val serviceRunning by graph.serviceRunning.collectAsStateWithLifecycle()
            val notificationAccess by graph.notificationAccess.collectAsStateWithLifecycle()
            CapsuleTheme(colorSource = state.settings.colorSource) {
                CompanionApp(
                    state = state,
                    serviceRunning = serviceRunning,
                    notificationAccess = notificationAccess,
                    dispatch = graph.store::dispatch,
                    onUpdateSettings = graph::updateSettings
                )
            }
        }
    }
}
