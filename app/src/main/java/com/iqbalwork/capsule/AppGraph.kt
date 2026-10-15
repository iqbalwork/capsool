package com.iqbalwork.capsule

import android.content.Context
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsRepository
import com.iqbalwork.capsule.core.store.CapsuleStore
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.platform.PlatformEffectHandler
import com.iqbalwork.capsule.platform.media.MediaSource
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
    val effects: PlatformEffectHandler = PlatformEffectHandler(mediaSource)

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _notificationAccess = MutableStateFlow(false)
    val notificationAccess: StateFlow<Boolean> = _notificationAccess.asStateFlow()

    init {
        appScope.launch {
            settingsRepository.settings.collect { store.dispatch(SystemEvent.SettingsChanged(it)) }
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        appScope.launch { settingsRepository.update(transform) }
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    /** Called by the notification listener; media sessions are only readable while it is bound. */
    fun onNotificationListenerConnected(connected: Boolean) {
        _notificationAccess.value = connected
        if (connected) mediaSource.start() else mediaSource.stop()
    }
}

val Context.appGraph: AppGraph
    get() = (applicationContext as CapsoolApp).graph
