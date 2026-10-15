package com.iqbalwork.capsule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iqbalwork.capsule.engine.preferences.CutoutPreferencesRepository
import com.iqbalwork.capsule.engine.state.CapsuleStateEngine
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.ui.companion.CompanionMainScreen
import com.iqbalwork.capsule.ui.theme.CapsuleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefsRepo = CutoutPreferencesRepository(this)
        val engine = CapsuleStateEngine.instance

        // Sync initial preferences into engine state
        engine.dispatch(UserAction.UpdateCutoutPreferences(prefsRepo.preferences.value))

        setContent {
            CapsuleTheme {
                CompanionMainScreen(
                    engine = engine,
                    prefsRepo = prefsRepo
                )
            }
        }
    }
}
