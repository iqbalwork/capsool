package com.iqbalwork.capsule.platform.accessibility

import android.accessibilityservice.AccessibilityService
import com.iqbalwork.capsule.platform.ScreenSize
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.iqbalwork.capsule.AppGraph
import com.iqbalwork.capsule.appGraph
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.VisibilityContext

/**
 * Hosts the capsule overlay. Uses TYPE_ACCESSIBILITY_OVERLAY, which the accessibility
 * binding authorizes on its own, so there is no Settings.canDrawOverlays check.
 * Configuration lives in res/xml/accessibility_service_config.xml and is not overridden here.
 */
class CapsoolAccessibilityService : AccessibilityService() {

    private lateinit var graph: AppGraph
    private var overlay: OverlayWindowController? = null
    private var visibility = VisibilityContext()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> updateVisibility { it.copy(screenOn = false, locked = isLocked()) }
                Intent.ACTION_SCREEN_ON -> updateVisibility { it.copy(screenOn = true, locked = isLocked()) }
                Intent.ACTION_USER_PRESENT -> updateVisibility { it.copy(locked = false) }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        graph = appGraph
        graph.effects.accessibilityService = this
        graph.setServiceRunning(true)

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        updateVisibility {
            VisibilityContext(
                screenOn = getSystemService(PowerManager::class.java).isInteractive,
                locked = isLocked(),
                orientation = orientationOf(resources.configuration),
                screenWidthPx = ScreenSize.of(this).first,
                screenHeightPx = ScreenSize.of(this).second
            )
        }

        overlay = OverlayWindowController(
            context = this,
            store = graph.store,
            scope = graph.appScope,
            onStatusBarVisibilityChanged = { visible -> updateVisibility { it.copy(fullscreen = !visible) } }
        ).also { it.attach() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::graph.isInitialized || event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                // Only real app windows count (the companion app included): the keyboard,
                // system UI and our overlay also fire window-state events.
                val window = windows.firstOrNull { it.id == event.windowId }
                if (window?.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    updateVisibility { it.copy(foregroundPackage = pkg) }
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val imeVisible = windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                updateVisibility { it.copy(imeVisible = imeVisible) }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::graph.isInitialized) return
        val (width, height) = ScreenSize.of(this)
        updateVisibility {
            it.copy(orientation = orientationOf(newConfig), screenWidthPx = width, screenHeightPx = height)
        }
        overlay?.refresh()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::graph.isInitialized) {
            overlay?.detach()
            overlay = null
            runCatching { unregisterReceiver(screenReceiver) }
            graph.effects.accessibilityService = null
            graph.setServiceRunning(false)
        }
        super.onDestroy()
    }

    private fun updateVisibility(transform: (VisibilityContext) -> VisibilityContext) {
        val updated = transform(visibility)
        if (updated == visibility) return
        visibility = updated
        graph.store.dispatch(SystemEvent.VisibilityChanged(updated))
    }

    private fun isLocked(): Boolean = getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun orientationOf(config: Configuration): Orientation =
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) Orientation.LANDSCAPE else Orientation.PORTRAIT
}
