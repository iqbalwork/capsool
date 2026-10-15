package com.iqbalwork.capsule.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Name of the SharedPreferences file written by the pre-v3 CutoutPreferencesRepository. */
private const val LEGACY_PREFS_NAME = "capsool_cutout_prefs"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME) { legacy, current ->
            migrateLegacyPrefs(legacy, current)
        })
    }
)

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = dataStore.data
        .map { SettingsMapper.read(it) }
        .distinctUntilChanged()

    suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            SettingsMapper.write(prefs, transform(SettingsMapper.read(prefs)))
        }
    }
}

/**
 * Maps [Settings] to and from flat preference keys. Kept separate from the DataStore wiring
 * so the mapping can be unit-tested with plain [MutablePreferences].
 */
object SettingsMapper {
    private fun geometryKeys(orientation: Orientation): GeometryKeys {
        val prefix = "geometry.${orientation.name.lowercase()}"
        return GeometryKeys(
            offsetX = intPreferencesKey("$prefix.offsetX"),
            offsetY = intPreferencesKey("$prefix.offsetY"),
            width = intPreferencesKey("$prefix.width"),
            height = intPreferencesKey("$prefix.height"),
            corner = intPreferencesKey("$prefix.corner")
        )
    }

    private class GeometryKeys(
        val offsetX: Preferences.Key<Int>,
        val offsetY: Preferences.Key<Int>,
        val width: Preferences.Key<Int>,
        val height: Preferences.Key<Int>,
        val corner: Preferences.Key<Int>
    )

    private val IdleOpacity = floatPreferencesKey("appearance.idleOpacity")
    private val AlwaysShowPill = booleanPreferencesKey("appearance.alwaysShowPill")
    private val ShowCalibrationTarget = booleanPreferencesKey("geometry.showCalibrationTarget")
    private val ColorSourceKey = stringPreferencesKey("appearance.colorSource")
    private val Haptics = booleanPreferencesKey("haptics.enabled")
    private val HideFullscreen = booleanPreferencesKey("visibility.hideFullscreen")
    private val HideKeyboard = booleanPreferencesKey("visibility.hideIme")
    private val HideLandscape = booleanPreferencesKey("visibility.hideLandscape")
    private val MediaEnabled = booleanPreferencesKey("cards.media.enabled")
    private val MediaCollapseDelayKey = stringPreferencesKey("media.collapseDelay")
    private val MediaHideWhenAppInFront = booleanPreferencesKey("media.hideWhenAppForeground")
    private val DisclosureAccepted = booleanPreferencesKey("onboarding.disclosureAccepted")
    private fun gestureKey(type: GestureType) = stringPreferencesKey("gestures.${type.name.lowercase()}")

    fun read(prefs: Preferences): Settings {
        val defaults = Settings()
        fun readGeometry(orientation: Orientation, fallback: Geometry): Geometry {
            val keys = geometryKeys(orientation)
            return Geometry(
                offsetXDp = prefs[keys.offsetX] ?: fallback.offsetXDp,
                offsetYDp = prefs[keys.offsetY] ?: fallback.offsetYDp,
                widthDp = prefs[keys.width] ?: fallback.widthDp,
                heightDp = prefs[keys.height] ?: fallback.heightDp,
                cornerDp = prefs[keys.corner] ?: fallback.cornerDp
            )
        }
        return Settings(
            portrait = readGeometry(Orientation.PORTRAIT, defaults.portrait),
            landscape = readGeometry(Orientation.LANDSCAPE, defaults.landscape),
            idleOpacity = prefs[IdleOpacity] ?: defaults.idleOpacity,
            alwaysShowPill = prefs[AlwaysShowPill] ?: defaults.alwaysShowPill,
            showCalibrationTarget = prefs[ShowCalibrationTarget] ?: defaults.showCalibrationTarget,
            colorSource = enumOrDefault(prefs[ColorSourceKey], defaults.colorSource),
            hapticsEnabled = prefs[Haptics] ?: defaults.hapticsEnabled,
            hideInFullscreen = prefs[HideFullscreen] ?: defaults.hideInFullscreen,
            hideWithKeyboard = prefs[HideKeyboard] ?: defaults.hideWithKeyboard,
            hideInLandscape = prefs[HideLandscape] ?: defaults.hideInLandscape,
            gestures = GestureType.entries.associateWith { type ->
                enumOrDefault(prefs[gestureKey(type)], DefaultGestures.getValue(type))
            },
            mediaEnabled = prefs[MediaEnabled] ?: defaults.mediaEnabled,
            mediaCollapseDelay = enumOrDefault(prefs[MediaCollapseDelayKey], defaults.mediaCollapseDelay),
            hideMediaWhenAppInFront = prefs[MediaHideWhenAppInFront] ?: defaults.hideMediaWhenAppInFront,
            disclosureAccepted = prefs[DisclosureAccepted] ?: defaults.disclosureAccepted
        )
    }

    fun write(prefs: MutablePreferences, settings: Settings) {
        fun writeGeometry(orientation: Orientation, geometry: Geometry) {
            val keys = geometryKeys(orientation)
            prefs[keys.offsetX] = geometry.offsetXDp
            prefs[keys.offsetY] = geometry.offsetYDp
            prefs[keys.width] = geometry.widthDp
            prefs[keys.height] = geometry.heightDp
            prefs[keys.corner] = geometry.cornerDp
        }
        writeGeometry(Orientation.PORTRAIT, settings.portrait)
        writeGeometry(Orientation.LANDSCAPE, settings.landscape)
        prefs[IdleOpacity] = settings.idleOpacity
        prefs[AlwaysShowPill] = settings.alwaysShowPill
        prefs[ShowCalibrationTarget] = settings.showCalibrationTarget
        prefs[ColorSourceKey] = settings.colorSource.name
        prefs[Haptics] = settings.hapticsEnabled
        prefs[HideFullscreen] = settings.hideInFullscreen
        prefs[HideKeyboard] = settings.hideWithKeyboard
        prefs[HideLandscape] = settings.hideInLandscape
        GestureType.entries.forEach { type -> prefs[gestureKey(type)] = settings.gestureAction(type).name }
        prefs[MediaEnabled] = settings.mediaEnabled
        prefs[MediaCollapseDelayKey] = settings.mediaCollapseDelay.name
        prefs[MediaHideWhenAppInFront] = settings.hideMediaWhenAppInFront
        prefs[DisclosureAccepted] = settings.disclosureAccepted
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
}

/**
 * Converts the pre-v3 cutout preferences. The old alignment presets moved the capsule
 * ±100 dp from the center; the old vertical offset was measured from the screen top rather
 * than from the cutout, so it is dropped and the capsule re-centers on the camera.
 */
internal fun migrateLegacyPrefs(legacy: SharedPreferencesView, current: Preferences): Preferences {
    val prefs = current.toMutablePreferences()
    val offsetX = when (legacy.getString("alignment", "CENTER")) {
        "LEFT" -> -100
        "RIGHT" -> 100
        "CUSTOM" -> legacy.getInt("offsetX", 0)
        else -> 0
    }
    val legacyGeometry = Geometry(
        offsetXDp = offsetX,
        widthDp = legacy.getInt("capsuleWidthDp", Geometry().widthDp),
        heightDp = legacy.getInt("capsuleHeightDp", Geometry().heightDp),
        cornerDp = legacy.getInt("cornerRadiusDp", Geometry().cornerDp)
    )
    val settings = SettingsMapper.read(prefs).copy(
        portrait = legacyGeometry,
        idleOpacity = legacy.getFloat("idleOpacity", 1f),
        showCalibrationTarget = legacy.getBoolean("showRedTarget", false)
    )
    SettingsMapper.write(prefs, settings)
    return prefs
}
