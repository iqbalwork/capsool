package com.iqbalwork.capsule.core.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Collapses the expanded card after this long without interaction (spec §2.3). */
private const val EXPANDED_IDLE_TIMEOUT_MS = 8_000L

/**
 * Holds [CapsuleState], runs actions through [CapsuleReducer] and hands effects to
 * [effectHandler]. Must be dispatched to from the main thread.
 */
class CapsuleStore(
    private val scope: CoroutineScope,
    private val effectHandler: (Effect) -> Unit
) {
    private val _state = MutableStateFlow(CapsuleState())
    val state: StateFlow<CapsuleState> = _state.asStateFlow()

    private var expandedTimeout: Job? = null
    private var mediaPauseTimeout: Job? = null
    private var mediaPauseKey: Pair<String, String>? = null

    fun dispatch(action: Action) {
        val reduction = CapsuleReducer.reduce(_state.value, action)
        _state.value = reduction.state
        reduction.effects.forEach(effectHandler)

        if (action is SystemEvent.MiniEventStarted) {
            scope.launch {
                delay(action.event.durationMs)
                dispatch(SystemEvent.MiniEventFinished(action.event.id))
            }
        }
        if (action is UserIntent) restartExpandedTimeout()
        scheduleMediaPauseTimeout()
    }

    /** Parks a paused player after the user's collapse delay (F-12). */
    private fun scheduleMediaPauseTimeout() {
        val state = _state.value
        val media = state.media
        val pausedAndShown = media != null && !media.isPlaying && !media.hiddenAfterPause
        if (!pausedAndShown) {
            mediaPauseTimeout?.cancel()
            mediaPauseKey = null
            return
        }
        val key = media.packageName to media.title
        if (key == mediaPauseKey) return
        mediaPauseTimeout?.cancel()
        mediaPauseKey = key
        val delayMs = state.settings.mediaCollapseDelay.millis ?: return
        mediaPauseTimeout = scope.launch {
            delay(delayMs)
            dispatch(SystemEvent.MediaPauseTimedOut(key.first, key.second))
        }
    }

    private fun restartExpandedTimeout() {
        expandedTimeout?.cancel()
        if (!_state.value.isExpanded) return
        expandedTimeout = scope.launch {
            delay(EXPANDED_IDLE_TIMEOUT_MS)
            dispatch(SystemEvent.ExpandedTimedOut)
        }
    }
}
