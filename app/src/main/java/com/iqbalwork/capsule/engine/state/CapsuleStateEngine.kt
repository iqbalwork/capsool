package com.iqbalwork.capsule.engine.state

import com.iqbalwork.capsule.engine.stack.CardStackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Reactive state coordinator for Capsool Island.
 * Dispatches actions through CapsuleStateReducer and manages auto-dismiss timeouts.
 */
class CapsuleStateEngine(
    val stackManager: CardStackManager = CardStackManager(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _state = MutableStateFlow(
        CapsuleState(
            availableCards = stackManager.getCards(),
            activeCard = stackManager.getActiveCard(),
            activeCardIndex = stackManager.getActiveIndex()
        )
    )
    val state: StateFlow<CapsuleState> = _state.asStateFlow()

    fun dispatch(action: UserAction) {
        val currentState = _state.value
        val newState = CapsuleStateReducer.reduce(currentState, action, stackManager)
        _state.value = newState

        if (action is UserAction.TriggerMiniEvent) {
            scope.launch {
                delay(action.event.durationMs)
                if (_state.value.activeMiniEvent == action.event) {
                    dispatch(UserAction.DismissMiniEvent)
                }
            }
        }
    }

    companion object {
        val instance: CapsuleStateEngine by lazy { CapsuleStateEngine() }
    }
}
