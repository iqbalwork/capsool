package com.iqbalwork.capsule

import android.content.Context
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsRepository
import com.iqbalwork.capsule.core.store.CapsuleStore
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.platform.PlatformEffectHandler
import com.iqbalwork.capsule.platform.controls.ControlsSource
import com.iqbalwork.capsule.debugtools.DiagnosticsPanel
import com.iqbalwork.capsule.platform.media.MediaSource
import com.iqbalwork.capsule.platform.notification.NotificationSource
import com.iqbalwork.capsule.platform.system.BatterySource
import com.iqbalwork.capsule.platform.system.SystemEventSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Process-wide singletons shared by the accessibility service and the companion app. */
class AppGraph(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settingsRepository = SettingsRepository(context)
    // The lambda reads `effects` lazily, which breaks the store → effects → media → store cycle.
    val store: CapsuleStore = CapsuleStore(appScope) { effects.handle(it) }
    val mediaSource: MediaSource = MediaSource(context.applicationContext, appScope, store::dispatch)
    val notificationSource: NotificationSource = NotificationSource(
        context = context.applicationContext,
        scope = appScope,
        settings = { store.state.value.settings },
        knownKeys = { store.state.value.notifications.mapTo(HashSet()) { it.key } },
        dispatch = store::dispatch,
        // Debug builds post their own test notifications (DiagnosticsPanel).
        ignoreOwnPackage = !DiagnosticsPanel.isAvailable
    )
    val controls: ControlsSource = ControlsSource(context.applicationContext, { store.state.value.settings.brightnessMax }, store::dispatch)
    val effects: PlatformEffectHandler = PlatformEffectHandler(
        context.applicationContext, mediaSource, notificationSource, controls,
        settings = { store.state.value.settings },
        dispatch = store::dispatch
    )
    val batterySource = BatterySource(context.applicationContext, { store.state.value.settings }, store::dispatch)
    val systemEvents = SystemEventSource(context.applicationContext, controls::isOwnRingerChange, store::dispatch)

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _notificationAccess = MutableStateFlow(false)
    val notificationAccess: StateFlow<Boolean> = _notificationAccess.asStateFlow()

    init {
        appScope.launch {
            settingsRepository.settings.collect {
                store.dispatch(SystemEvent.SettingsChanged(it))
                controls.refresh() // brightness position depends on the calibrated maximum
            }
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        appScope.launch { settingsRepository.update(transform) }
    }

    /** Battery and system events only matter while the capsule can be shown. */
    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
        if (running) {
            batterySource.start()
            systemEvents.start()
            controls.start()
        } else {
            batterySource.stop()
            systemEvents.stop()
            controls.stop()
        }
    }

    /** Called by the notification listener; media sessions are only readable while it is bound. */
    fun onNotificationListenerConnected(connected: Boolean) {
        _notificationAccess.value = connected
        if (connected) mediaSource.start() else mediaSource.stop()
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as CapsoolApp).graph
