package com.iqbalwork.capsule.engine.preferences

import android.content.Context
import android.content.SharedPreferences
import com.iqbalwork.capsule.model.CutoutAlignment
import com.iqbalwork.capsule.model.CutoutPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CutoutPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("capsool_cutout_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<CutoutPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): CutoutPreferences {
        val alignmentStr = prefs.getString("alignment", CutoutAlignment.CENTER.name) ?: CutoutAlignment.CENTER.name
        val alignment = try {
            CutoutAlignment.valueOf(alignmentStr)
        } catch (_: Exception) {
            CutoutAlignment.CENTER
        }

        return CutoutPreferences(
            alignment = alignment,
            offsetX = prefs.getInt("offsetX", 0),
            offsetY = prefs.getInt("offsetY", 0),
            capsuleWidthDp = prefs.getInt("capsuleWidthDp", 120),
            capsuleHeightDp = prefs.getInt("capsuleHeightDp", 36),
            cornerRadiusDp = prefs.getInt("cornerRadiusDp", 18),
            screenCornerRadiusDp = prefs.getInt("screenCornerRadiusDp", 0),
            idleOpacity = prefs.getFloat("idleOpacity", 1.0f),
            showRedTarget = prefs.getBoolean("showRedTarget", false)
        )
    }

    fun updatePreferences(newPrefs: CutoutPreferences) {
        prefs.edit()
            .putString("alignment", newPrefs.alignment.name)
            .putInt("offsetX", newPrefs.offsetX)
            .putInt("offsetY", newPrefs.offsetY)
            .putInt("capsuleWidthDp", newPrefs.capsuleWidthDp)
            .putInt("capsuleHeightDp", newPrefs.capsuleHeightDp)
            .putInt("cornerRadiusDp", newPrefs.cornerRadiusDp)
            .putInt("screenCornerRadiusDp", newPrefs.screenCornerRadiusDp)
            .putFloat("idleOpacity", newPrefs.idleOpacity)
            .putBoolean("showRedTarget", newPrefs.showRedTarget)
            .apply()

        _preferences.value = newPrefs
    }
}
