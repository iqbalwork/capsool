package com.iqbalwork.capsule.platform.media

/** What the selector needs to know about one active media session. */
data class SessionCandidate(val key: String, val isPlaying: Boolean)

/**
 * Picks the session the capsule follows (spec §2.5.1): a playing session wins, preferring the
 * one already followed; otherwise stay on the current session while it still exists, so a
 * pause does not jump to another app; otherwise the most recent session.
 */
object MediaSessionSelector {
    fun select(candidates: List<SessionCandidate>, currentKey: String?): String? {
        val playing = candidates.filter { it.isPlaying }
        if (playing.isNotEmpty()) {
            return playing.firstOrNull { it.key == currentKey }?.key ?: playing.first().key
        }
        return candidates.firstOrNull { it.key == currentKey }?.key ?: candidates.firstOrNull()?.key
    }
}
