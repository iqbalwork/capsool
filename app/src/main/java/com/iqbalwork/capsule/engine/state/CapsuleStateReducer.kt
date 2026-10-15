package com.iqbalwork.capsule.engine.state

import com.iqbalwork.capsule.engine.stack.CardStackManager
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.VolumeSliderStyle

object CapsuleStateReducer {

    fun reduce(
        state: CapsuleState,
        action: UserAction,
        stackManager: CardStackManager
    ): CapsuleState {
        return when (action) {
            is UserAction.Tap -> {
                state.copy(
                    isExpanded = !state.isExpanded,
                    activeMiniEvent = if (!state.isExpanded) null else state.activeMiniEvent
                )
            }

            is UserAction.DoubleTap -> {
                // Quick shortcut: Toggle media playback or torch
                if (state.mediaState.isPlaying || state.activeCard == com.iqbalwork.capsule.model.CapsuleCardType.MEDIA) {
                    state.copy(
                        mediaState = state.mediaState.copy(isPlaying = !state.mediaState.isPlaying)
                    )
                } else {
                    state.copy(
                        sliderState = state.sliderState.copy(isTorchOn = !state.sliderState.isTorchOn)
                    )
                }
            }

            is UserAction.LongPress -> {
                // Expand to full card
                state.copy(
                    isExpanded = true,
                    activeMiniEvent = null
                )
            }

            is UserAction.SwipeLeft -> {
                val next = stackManager.nextCard()
                state.copy(
                    activeCard = next,
                    activeCardIndex = stackManager.getActiveIndex()
                )
            }

            is UserAction.SwipeRight -> {
                val prev = stackManager.previousCard()
                state.copy(
                    activeCard = prev,
                    activeCardIndex = stackManager.getActiveIndex()
                )
            }

            is UserAction.Dismiss -> {
                state.copy(isExpanded = false)
            }

            is UserAction.SelectCard -> {
                stackManager.setActiveCard(action.cardType)
                state.copy(
                    activeCard = action.cardType,
                    activeCardIndex = stackManager.getActiveIndex()
                )
            }

            is UserAction.ToggleCardEnabled -> {
                stackManager.setCardEnabled(action.cardType, action.enabled)
                state.copy(
                    availableCards = stackManager.getCards(),
                    activeCard = stackManager.getActiveCard(),
                    activeCardIndex = stackManager.getActiveIndex()
                )
            }

            is UserAction.MediaPlayPause -> {
                state.copy(
                    mediaState = state.mediaState.copy(isPlaying = !state.mediaState.isPlaying)
                )
            }

            is UserAction.MediaNext -> {
                state.copy(
                    mediaState = state.mediaState.copy(
                        title = "Next Track",
                        artist = state.mediaState.artist,
                        playbackPositionMs = 0L
                    )
                )
            }

            is UserAction.MediaPrevious -> {
                state.copy(
                    mediaState = state.mediaState.copy(
                        title = "Previous Track",
                        playbackPositionMs = 0L
                    )
                )
            }

            is UserAction.MediaSeek -> {
                state.copy(
                    mediaState = state.mediaState.copy(
                        playbackPositionMs = action.positionMs.coerceIn(0L, state.mediaState.durationMs)
                    )
                )
            }

            is UserAction.SetVolume -> {
                state.copy(
                    sliderState = state.sliderState.copy(
                        volumeLevel = action.level.coerceIn(0f, 1f)
                    )
                )
            }

            is UserAction.CycleVolumeStyle -> {
                val styles = VolumeSliderStyle.entries
                val nextIdx = (styles.indexOf(state.sliderState.volumeStyle) + 1) % styles.size
                state.copy(
                    sliderState = state.sliderState.copy(volumeStyle = styles[nextIdx])
                )
            }

            is UserAction.CycleRingerMode -> {
                val modes = RingerMode.entries
                val nextIdx = (modes.indexOf(state.sliderState.ringerMode) + 1) % modes.size
                state.copy(
                    sliderState = state.sliderState.copy(ringerMode = modes[nextIdx])
                )
            }

            is UserAction.SetBrightness -> {
                state.copy(
                    sliderState = state.sliderState.copy(
                        brightnessLevel = action.level.coerceIn(0f, 1f)
                    )
                )
            }

            is UserAction.ToggleTorch -> {
                state.copy(
                    sliderState = state.sliderState.copy(
                        isTorchOn = !state.sliderState.isTorchOn
                    )
                )
            }

            is UserAction.SetTorchLevel -> {
                state.copy(
                    sliderState = state.sliderState.copy(
                        torchStrengthLevel = action.level.coerceIn(0f, 1f),
                        isTorchOn = action.level > 0.05f
                    )
                )
            }

            is UserAction.ReplyNotification -> {
                state.copy(notificationState = null)
            }

            is UserAction.DismissNotification -> {
                state.copy(notificationState = null)
            }

            is UserAction.TimerAddMinute -> {
                state.copy(
                    timerState = state.timerState.copy(
                        remainingSeconds = state.timerState.remainingSeconds + 60,
                        totalSeconds = state.timerState.totalSeconds + 60
                    )
                )
            }

            is UserAction.TimerTogglePause -> {
                state.copy(
                    timerState = state.timerState.copy(
                        isRunning = !state.timerState.isRunning
                    )
                )
            }

            is UserAction.TimerReset -> {
                state.copy(
                    timerState = state.timerState.copy(
                        remainingSeconds = state.timerState.totalSeconds,
                        isRunning = false
                    )
                )
            }

            is UserAction.ExecuteShortcut -> {
                // Handled in UI/engine side effect; state remains consistent
                state
            }

            is UserAction.UpdateMedia -> {
                state.copy(mediaState = action.mediaState)
            }

            is UserAction.UpdateSliders -> {
                state.copy(sliderState = action.sliderState)
            }

            is UserAction.UpdateCharging -> {
                state.copy(chargingState = action.chargingState)
            }

            is UserAction.UpdateNotification -> {
                state.copy(notificationState = action.notificationState)
            }

            is UserAction.UpdateTimer -> {
                state.copy(timerState = action.timerState)
            }

            is UserAction.TriggerMiniEvent -> {
                state.copy(activeMiniEvent = action.event)
            }

            is UserAction.DismissMiniEvent -> {
                state.copy(activeMiniEvent = null)
            }

            is UserAction.UpdateCutoutPreferences -> {
                state.copy(cutoutPreferences = action.preferences)
            }
        }
    }
}
