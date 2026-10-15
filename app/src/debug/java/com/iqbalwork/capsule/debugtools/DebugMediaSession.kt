package com.iqbalwork.capsule.debugtools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock

/**
 * Debug-only: a real MediaSession (no audio) so the full listener → MediaSource → card →
 * transport-controls path can be tested on devices without a player app.
 */
object DebugMediaSession {
    private const val DURATION_MS = 180_000L
    private val tracks = listOf("Debug Track One" to 0xFF6A1B9A.toInt(), "Debug Track Two" to 0xFF00897B.toInt())
    private var session: MediaSession? = null
    private var trackIndex = 0
    private var playing = false
    private var position = 0L
    private var updatedAt = 0L

    fun toggle(context: Context) {
        if (session == null) start(context) else stop()
    }

    private fun start(context: Context) {
        val newSession = MediaSession(context.applicationContext, "CapsoolDebug")
        newSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = update(play = true)
            override fun onPause() = update(play = false)
            override fun onSkipToNext() = changeTrack(1)
            override fun onSkipToPrevious() = changeTrack(-1)
            override fun onSeekTo(pos: Long) {
                position = pos
                update(play = playing)
            }
        })
        newSession.isActive = true
        session = newSession
        trackIndex = 0
        position = 0L
        publishMetadata()
        update(play = true)
    }

    private fun stop() {
        session?.release()
        session = null
    }

    private fun changeTrack(step: Int) {
        trackIndex = (trackIndex + step).mod(tracks.size)
        position = 0L
        updatedAt = SystemClock.elapsedRealtime()
        publishMetadata()
        update(play = true)
    }

    private fun update(play: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (playing) position += now - updatedAt
        playing = play
        updatedAt = now
        session?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO
                )
                .setState(if (play) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, position, 1f, now)
                .build()
        )
    }

    private fun publishMetadata() {
        val (title, color) = tracks[trackIndex]
        session?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Capsool Debug")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, DURATION_MS)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, cover(color))
                .build()
        )
    }

    private fun cover(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 256f, 256f, color, 0xFFFFC107.toInt(), Shader.TileMode.CLAMP)
        }
        Canvas(bitmap).drawRect(0f, 0f, 256f, 256f, paint)
        return bitmap
    }
}
