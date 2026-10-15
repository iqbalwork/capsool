package com.iqbalwork.capsule.platform.controls

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TorchState
import kotlin.math.roundToInt

private const val TAG = "CapsoolControls"

/** Ringer broadcasts this soon after our own change are echoes, not user actions (spec §2.5.4). */
private const val RINGER_ECHO_MS = 1_000L

/** Lowest raw brightness we write; 0 turns some panels fully off. */
private const val BRIGHTNESS_MIN = 1

/**
 * Reads and drives media volume, ringer mode, screen brightness and the flashlight for the
 * Controls card (spec §2.5.5), and reports every change, ours or external, as SlidersUpdated.
 */
class ControlsSource(
    private val context: Context,
    private val brightnessMax: () -> Int,
    private val dispatch: (SystemEvent) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val cameras = context.getSystemService(CameraManager::class.java)
    private val torchCamera: TorchCamera? by lazy { findTorchCamera() }
    private var torch = TorchState.NONE
    private var torchLevel = 1f
    private var lastRingerChangeAt = 0L
    private var started = false
    private var last: SliderState? = null

    private data class TorchCamera(val id: String, val maxLevel: Int)

    private val settingsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = publish()
    }

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publish()
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId != torchCamera?.id) return
            torch = if (enabled) TorchState.ON else TorchState.OFF
            publish()
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId != torchCamera?.id) return
            torch = TorchState.BUSY
            publish()
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            val camera = torchCamera ?: return
            if (cameraId != camera.id || camera.maxLevel <= 1) return
            torchLevel = newStrengthLevel.toFloat() / camera.maxLevel
            publish()
        }
    }

    fun start() {
        if (started) return
        started = true
        // Volume and brightness both live in Settings.System; one observer catches both.
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, settingsObserver)
        ContextCompat.registerReceiver(
            context,
            ringerReceiver,
            IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        torch = if (torchCamera != null) TorchState.OFF else TorchState.NONE
        if (torchCamera != null) cameras.registerTorchCallback(torchCallback, handler)
        publish()
    }

    fun stop() {
        if (!started) return
        started = false
        context.contentResolver.unregisterContentObserver(settingsObserver)
        runCatching { context.unregisterReceiver(ringerReceiver) }
        runCatching { cameras.unregisterTorchCallback(torchCallback) }
        last = null
    }

    /** True for a ringer broadcast caused by our own change, so no mini event is shown for it. */
    fun isOwnRingerChange(): Boolean = SystemClock.elapsedRealtime() - lastRingerChangeAt < RINGER_ECHO_MS

    /** Re-read state, e.g. after the user granted a permission in Settings. */
    fun refresh() = publish()

    fun execute(effect: Effect) {
        when (effect) {
            is Effect.Volume -> setVolume(effect.level)
            is Effect.Ringer -> setRinger(effect.mode)
            is Effect.Brightness -> setBrightness(effect.level)
            is Effect.AdaptiveBrightness -> setAdaptive(effect.on)
            is Effect.Torch -> setTorch(effect.on, effect.level)
            is Effect.OpenPermission -> openPermission(effect.screen)
            Effect.RefreshControls -> publish()
            else -> Unit
        }
    }

    private fun setVolume(level: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val index = (min + level * (max - min)).roundToInt().coerceIn(min, max)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0) }
            .onFailure { Log.w(TAG, "Volume change refused", it) }
    }

    private fun setRinger(mode: RingerMode) {
        lastRingerChangeAt = SystemClock.elapsedRealtime()
        val value = when (mode) {
            RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
        // Switching to or from silent while DND rules apply throws without policy access.
        runCatching { audio.ringerMode = value }.onFailure {
            Log.w(TAG, "Ringer change refused", it)
            publish()
        }
    }

    private fun setBrightness(position: Float) {
        if (!Settings.System.canWrite(context)) return
        val value = BrightnessCurve.valueFor(position, BRIGHTNESS_MIN, brightnessMax())
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }

    private fun setAdaptive(on: Boolean) {
        if (!Settings.System.canWrite(context)) return
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
    }

    private fun setTorch(on: Boolean, level: Float?) {
        val camera = torchCamera ?: return
        try {
            if (on && level != null && camera.maxLevel > 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val strength = (level * camera.maxLevel).roundToInt().coerceIn(1, camera.maxLevel)
                cameras.turnOnTorchWithStrengthLevel(camera.id, strength)
            } else {
                cameras.setTorchMode(camera.id, on)
            }
        } catch (e: CameraAccessException) {
            // Usually the camera app holds the flash; the card shows "Camera in use".
            Log.w(TAG, "Flashlight unavailable", e)
            torch = TorchState.BUSY
            publish()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Flashlight refused", e)
            publish()
        }
    }

    private fun openPermission(screen: Effect.PermissionScreen) {
        val intent = when (screen) {
            Effect.PermissionScreen.WRITE_SETTINGS ->
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", context.packageName, null))
            Effect.PermissionScreen.DND_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { Log.w(TAG, "Could not open $screen", it) }
    }

    private fun publish() {
        if (!started) return
        val state = read()
        if (state == last) return
        last = state
        dispatch(SystemEvent.SlidersUpdated(state))
    }

    private fun read(): SliderState {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val resolver = context.contentResolver
        val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 0)
        val adaptive = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) ==
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        return SliderState(
            volumeLevel = if (max > min) (volume - min).toFloat() / (max - min) else 0f,
            ringerMode = when (audio.ringerMode) {
                AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
                AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
                else -> RingerMode.NORMAL
            },
            canSilence = notifications.isNotificationPolicyAccessGranted,
            brightnessLevel = BrightnessCurve.positionFor(brightness, BRIGHTNESS_MIN, brightnessMax()),
            isAdaptiveBrightness = adaptive,
            canWriteBrightness = Settings.System.canWrite(context),
            torch = torch,
            torchStrengthLevel = torchLevel,
            torchStrengthSupported = (torchCamera?.maxLevel ?: 0) > 1
        )
    }

    private fun findTorchCamera(): TorchCamera? = runCatching {
        cameras.cameraIdList.firstNotNullOfOrNull { id ->
            val characteristics = cameras.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val back = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            if (!hasFlash || !back) return@firstNotNullOfOrNull null
            val maxLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } else {
                1
            }
            TorchCamera(id, maxLevel)
        }
    }.getOrNull()
}
