package com.iqbalwork.capsule.platform.system

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType

/** Hidden Bluetooth battery broadcast (spec §2.5.4): stable key, best effort only. */
private const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

/** A battery report this soon after connecting updates the "connected" event with a level. */
private const val HEADPHONE_BATTERY_WINDOW_MS = 15_000L

/**
 * Ringer, airplane mode, unlock, Bluetooth headphones, Wi-Fi and auto-brightness changes,
 * reported as mini events (spec §2.5.4). Current states are read as a baseline on start and
 * never announced.
 */
class SystemEventSource(
    private val context: Context,
    /** True right after Capsool itself changed the ringer: no pop-up for our own change. */
    private val isOwnRingerChange: () -> Boolean,
    private val dispatch: (SystemEvent) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val wifiTracker = WifiTracker()
    private val headphonesConnectedAt = mutableMapOf<String, Long>()
    private var started = false
    private var bluetoothRegistered = false

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isInitialStickyBroadcast) return
            when (intent.action) {
                AudioManager.RINGER_MODE_CHANGED_ACTION ->
                    if (!isOwnRingerChange()) ringerChanged(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1))
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> emit(
                    if (intent.getBooleanExtra("state", false)) MiniEventType.AIRPLANE_ON else MiniEventType.AIRPLANE_OFF,
                    if (intent.getBooleanExtra("state", false)) "Airplane mode on" else "Airplane mode off"
                )
                Intent.ACTION_USER_PRESENT -> emit(MiniEventType.DEVICE_UNLOCKED, "Welcome back")
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.bluetoothDevice() ?: return
            if (!isHeadphones(device)) return
            val name = deviceName(device)
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    headphonesConnectedAt[device.address] = SystemClock.elapsedRealtime()
                    emit(MiniEventType.HEADPHONES_CONNECTED, name, "Connected")
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    headphonesConnectedAt.remove(device.address)
                    emit(MiniEventType.HEADPHONES_DISCONNECTED, name, "Disconnected")
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1).takeIf { it in 0..100 } ?: return
                    val connectedAt = headphonesConnectedAt[device.address] ?: return
                    if (SystemClock.elapsedRealtime() - connectedAt > HEADPHONE_BATTERY_WINDOW_MS) return
                    headphonesConnectedAt.remove(device.address)
                    emit(MiniEventType.HEADPHONES_CONNECTED, name, "$level%", level)
                }
            }
        }
    }

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handler.post { wifiTracker.onAvailable(network.toString(), SystemClock.elapsedRealtime())?.let(::wifiChanged) }
        }

        override fun onLost(network: Network) {
            handler.post { wifiTracker.onLost(network.toString())?.let(::wifiChanged) }
        }
    }

    private val brightnessObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            val auto = Settings.System.getInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, -1
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            emit(
                if (auto) MiniEventType.AUTO_BRIGHTNESS_ON else MiniEventType.AUTO_BRIGHTNESS_OFF,
                if (auto) "Auto-brightness on" else "Auto-brightness off"
            )
        }
    }

    fun start() {
        if (started) return
        started = true
        ContextCompat.registerReceiver(
            context,
            systemReceiver,
            IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        wifiTracker.reset(SystemClock.elapsedRealtime())
        connectivity.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            wifiCallback,
            handler
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, brightnessObserver
        )
        refreshBluetooth()
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { context.unregisterReceiver(systemReceiver) }
        runCatching { connectivity.unregisterNetworkCallback(wifiCallback) }
        context.contentResolver.unregisterContentObserver(brightnessObserver)
        if (bluetoothRegistered) runCatching { context.unregisterReceiver(bluetoothReceiver) }
        bluetoothRegistered = false
    }

    /** Headphone events need BLUETOOTH_CONNECT; call again after the user grants it. */
    fun refreshBluetooth() {
        if (!started || bluetoothRegistered || !hasBluetoothPermission()) return
        // Exported because the battery broadcast is not a protected one; requiring the sender
        // to hold BLUETOOTH_CONNECT keeps other apps from faking headphone events.
        ContextCompat.registerReceiver(
            context,
            bluetoothReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(ACTION_BATTERY_LEVEL_CHANGED)
            },
            Manifest.permission.BLUETOOTH_CONNECT,
            handler,
            ContextCompat.RECEIVER_EXPORTED
        )
        bluetoothRegistered = true
    }

    fun hasBluetoothPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun ringerChanged(mode: Int) = when (mode) {
        AudioManager.RINGER_MODE_NORMAL -> emit(MiniEventType.RINGER_NORMAL, "Ring")
        AudioManager.RINGER_MODE_VIBRATE -> emit(MiniEventType.RINGER_VIBRATE, "Vibrate")
        AudioManager.RINGER_MODE_SILENT -> emit(MiniEventType.RINGER_SILENT, "Silent")
        else -> Unit
    }

    private fun wifiChanged(connected: Boolean) = emit(
        if (connected) MiniEventType.WIFI_CONNECTED else MiniEventType.WIFI_DISCONNECTED,
        if (connected) "Wi-Fi connected" else "Wi-Fi disconnected"
    )

    private fun emit(type: MiniEventType, title: String, subtitle: String = "", level: Int? = null) {
        dispatch(SystemEvent.MiniEventStarted(MiniEvent(System.nanoTime(), type, title, subtitle, level)))
    }

    @Suppress("MissingPermission") // only registered once BLUETOOTH_CONNECT is granted
    private fun isHeadphones(device: BluetoothDevice): Boolean =
        runCatching { device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO }
            .getOrDefault(false)

    @Suppress("MissingPermission")
    private fun deviceName(device: BluetoothDevice): String =
        runCatching { device.alias ?: device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Headphones"

    private fun Intent.bluetoothDevice(): BluetoothDevice? =
        IntentCompat.getParcelableExtra(this, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
}

/**
 * Wi-Fi connectivity edges from NetworkCallback (pure). Right after registering, the callback
 * reports networks that were already connected; those within [BASELINE_WINDOW_MS] are the
 * baseline, not a new connection.
 */
class WifiTracker {
    private val networks = mutableSetOf<String>()
    private var registeredAt = 0L

    fun reset(nowMs: Long) {
        networks.clear()
        registeredAt = nowMs
    }

    /** Returns true when Wi-Fi just became connected, null when nothing to announce. */
    fun onAvailable(id: String, nowMs: Long): Boolean? {
        val wasConnected = networks.isNotEmpty()
        networks += id
        if (nowMs - registeredAt < BASELINE_WINDOW_MS) return null
        return if (!wasConnected) true else null
    }

    /** Returns false when the last Wi-Fi network went away, null otherwise. */
    fun onLost(id: String): Boolean? {
        if (!networks.remove(id)) return null
        return if (networks.isEmpty()) false else null
    }

    companion object {
        const val BASELINE_WINDOW_MS = 1_500L
    }
}
