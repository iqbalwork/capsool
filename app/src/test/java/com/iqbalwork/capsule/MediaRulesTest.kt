package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.core.store.VisibilityContext
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.platform.media.MediaSessionSelector
import com.iqbalwork.capsule.platform.media.SessionCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRulesTest {

    private val loaded = CapsuleState(settingsLoaded = true)
    private val playing = MediaState(packageName = "com.player", title = "Song", artist = "A", isPlaying = true)
    private val paused = playing.copy(isPlaying = false)

    private fun reduce(state: CapsuleState, vararg actions: Action) =
        actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }

    @Test
    fun sessionFirstSeenPausedStaysHidden() {
        val state = reduce(loaded, SystemEvent.MediaUpdated(paused))
        assertTrue(state.media!!.hiddenAfterPause)
        assertFalse(CapsuleCardType.MEDIA in state.activeCards)
    }

    @Test
    fun pauseKeepsCardUntilTimeout() {
        val state = reduce(loaded, SystemEvent.MediaUpdated(playing), SystemEvent.MediaUpdated(paused))
        assertTrue(CapsuleCardType.MEDIA in state.activeCards)
        val timedOut = reduce(state, SystemEvent.MediaPauseTimedOut("com.player", "Song"))
        assertFalse(CapsuleCardType.MEDIA in timedOut.activeCards)
    }

    @Test
    fun staleTimeoutIsIgnored() {
        val state = reduce(loaded, SystemEvent.MediaUpdated(playing), SystemEvent.MediaUpdated(paused))
        assertFalse(reduce(state, SystemEvent.MediaPauseTimedOut("com.player", "Other")).media!!.hiddenAfterPause)
        val resumed = reduce(state, SystemEvent.MediaUpdated(playing))
        assertFalse(reduce(resumed, SystemEvent.MediaPauseTimedOut("com.player", "Song")).media!!.hiddenAfterPause)
    }

    @Test
    fun hiddenTrackStaysHiddenOnRepeatedPausedUpdates() {
        val hidden = reduce(loaded, SystemEvent.MediaUpdated(paused))
        val again = reduce(hidden, SystemEvent.MediaUpdated(paused.copy(playbackPositionMs = 10)))
        assertTrue(again.media!!.hiddenAfterPause)
        assertFalse(reduce(again, SystemEvent.MediaUpdated(playing)).media!!.hiddenAfterPause)
    }

    @Test
    fun hiddenWhilePlayerAppIsInFront() {
        val inFront = reduce(
            loaded,
            SystemEvent.MediaUpdated(playing),
            SystemEvent.VisibilityChanged(VisibilityContext(foregroundPackage = "com.player"))
        )
        assertFalse(CapsuleCardType.MEDIA in inFront.activeCards)
        val allowed = inFront.copy(settings = Settings(hideMediaWhenAppInFront = false))
        assertTrue(CapsuleCardType.MEDIA in allowed.activeCards)
    }

    @Test
    fun disabledMediaCardNeverShows() {
        val state = reduce(loaded.copy(settings = Settings(mediaEnabled = false)), SystemEvent.MediaUpdated(playing))
        assertFalse(CapsuleCardType.MEDIA in state.activeCards)
    }

    @Test
    fun openAppCollapsesAndEmitsEffect() {
        val expanded = reduce(loaded, SystemEvent.MediaUpdated(playing)).copy(isExpanded = true)
        val reduction = CapsuleReducer.reduce(expanded, UserIntent.MediaOpenApp)
        assertFalse(reduction.state.isExpanded)
        assertEquals(listOf(com.iqbalwork.capsule.core.store.Effect.MediaOpenApp), reduction.effects)
    }

    @Test
    fun positionIsExtrapolatedWhilePlaying() {
        val media = playing.copy(playbackPositionMs = 10_000, positionUpdatedAtMs = 1_000, durationMs = 60_000, playbackSpeed = 2f)
        assertEquals(14_000, media.positionAt(3_000))
        assertEquals(60_000, media.positionAt(100_000))
        assertEquals(10_000, media.copy(isPlaying = false).positionAt(3_000))
    }

    @Test
    fun seekBarOnlyWhenSeekableWithDuration() {
        assertFalse(playing.copy(canSeek = true, durationMs = 0).showsSeekBar)
        assertFalse(playing.copy(canSeek = false, durationMs = 1000).showsSeekBar)
        assertTrue(playing.copy(canSeek = true, durationMs = 1000).showsSeekBar)
    }

    @Test
    fun selectorPrefersPlayingThenCurrent() {
        val a = SessionCandidate("a", isPlaying = false)
        val b = SessionCandidate("b", isPlaying = true)
        val c = SessionCandidate("c", isPlaying = true)
        assertEquals("b", MediaSessionSelector.select(listOf(a, b, c), currentKey = "a"))
        assertEquals("c", MediaSessionSelector.select(listOf(a, b, c), currentKey = "c"))
        assertEquals("a", MediaSessionSelector.select(listOf(SessionCandidate("x", false), a), currentKey = "a"))
        assertEquals("x", MediaSessionSelector.select(listOf(SessionCandidate("x", false), a), currentKey = null))
        assertNull(MediaSessionSelector.select(emptyList(), currentKey = "a"))
    }
}
