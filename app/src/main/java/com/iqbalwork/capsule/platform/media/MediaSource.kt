package com.iqbalwork.capsule.platform.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.platform.notification.CapsoolNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private const val TAG = "CapsoolMedia"

/** Coalesces bursts of metadata/playback callbacks when a track changes (spec §2.5.1). */
private const val PUBLISH_DEBOUNCE_MS = 120L

/** Artwork is downscaled before palette extraction and before it is held in state. */
private const val ARTWORK_MAX_PX = 256

/**
 * Follows the active media sessions (needs the notification listener to be enabled) and
 * reports the chosen one as [SystemEvent.MediaUpdated]. Also executes media effects.
 */
class MediaSource(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dispatch: (SystemEvent) -> Unit
) {
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, CapsoolNotificationListener::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var publishJob: Job? = null
    private var started = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        onSessionsChanged(sessions.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshSelection()
        override fun onMetadataChanged(metadata: MediaMetadata?) = schedulePublish()
        override fun onSessionDestroyed() = refreshSelection()
    }

    fun start() {
        if (started) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            started = true
            refreshSelection()
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification access missing; media card disabled", e)
        }
    }

    fun stop() {
        if (!started) return
        started = false
        sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        follow(null)
        dispatch(SystemEvent.MediaUpdated(null))
    }

    fun execute(effect: Effect) {
        val current = controller ?: return
        val controls = current.transportControls
        when (effect) {
            is Effect.Media -> when (effect.command) {
                Effect.MediaCommand.PLAY_PAUSE ->
                    if (current.playbackState.isPlaying()) controls.pause() else controls.play()
                Effect.MediaCommand.NEXT -> controls.skipToNext()
                Effect.MediaCommand.PREVIOUS -> controls.skipToPrevious()
            }
            is Effect.MediaSeek -> controls.seekTo(effect.positionMs)
            Effect.MediaOpenApp -> runCatching { current.sessionActivity?.send() }
                .onFailure { Log.w(TAG, "Could not open player", it) }
            else -> Unit
        }
    }

    private fun refreshSelection() {
        if (!started) return
        val sessions = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification access revoked", e)
            emptyList()
        }
        onSessionsChanged(sessions)
    }

    private fun onSessionsChanged(sessions: List<MediaController>) {
        val candidates = sessions.map { SessionCandidate(it.sessionToken.toString(), it.playbackState.isPlaying()) }
        val chosenKey = MediaSessionSelector.select(candidates, controller?.sessionToken?.toString())
        follow(sessions.firstOrNull { it.sessionToken.toString() == chosenKey })
        schedulePublish()
    }

    private fun follow(next: MediaController?) {
        if (next?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(controllerCallback)
        controller = next
        next?.registerCallback(controllerCallback, mainHandler)
    }

    private fun schedulePublish() {
        publishJob?.cancel()
        publishJob = scope.launch {
            delay(PUBLISH_DEBOUNCE_MS)
            val current = controller
            val media = current?.let { withContext(Dispatchers.Default) { readState(it) } }
            dispatch(SystemEvent.MediaUpdated(media))
        }
    }

    private fun readState(controller: MediaController): MediaState? {
        val metadata = controller.metadata ?: return null
        val playback = controller.playbackState
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val artwork = (metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON))
            ?.let(::downscale)

        return MediaState(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            artwork = artwork,
            isPlaying = playback.isPlaying(),
            playbackPositionMs = playback?.position ?: 0L,
            positionUpdatedAtMs = playback?.lastPositionUpdateTime ?: 0L,
            playbackSpeed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
            canSeek = (playback?.actions ?: 0L) and PlaybackState.ACTION_SEEK_TO != 0L,
            dominantColor = artwork?.let(::coverColor) ?: MediaState().dominantColor
        )
    }

    private fun downscale(bitmap: Bitmap): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= ARTWORK_MAX_PX) return bitmap
        val ratio = ARTWORK_MAX_PX.toFloat() / largest
        return bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
    }

    private fun coverColor(bitmap: Bitmap): Long? {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: return null
        return swatch.rgb.toLong() and 0xFFFFFFFFL
    }

    private fun PlaybackState?.isPlaying(): Boolean =
        this?.state == PlaybackState.STATE_PLAYING || this?.state == PlaybackState.STATE_BUFFERING
}
