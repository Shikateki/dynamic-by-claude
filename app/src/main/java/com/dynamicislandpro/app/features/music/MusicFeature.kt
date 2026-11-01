package com.dynamicislandpro.app.features.music

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.ThumbnailUtils
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandAction
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.IslandFeature
import com.dynamicislandpro.app.services.IslandNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Informasi musik lewat MediaSessionManager (resmi). Android mensyaratkan
 * Notification Access agar getActiveSessions() diizinkan; tanpa itu fitur ini
 * diam saja dan mencoba lagi saat izin diberikan.
 */
class MusicFeature(
    private val context: Context,
    private val scope: CoroutineScope
) : IslandFeature {

    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(context, IslandNotificationListener::class.java)

    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var controller: MediaController? = null
    private var callback: MediaController.Callback? = null
    private var retryJob: Job? = null

    override fun start() {
        register()
        retryJob = scope.launch {
            DynamicIslandEngine.notificationAccessTick.drop(1).collect { register() }
        }
    }

    override fun stop() {
        retryJob?.cancel()
        retryJob = null
        unregisterSessions()
        detach()
        DynamicIslandEngine.remove(ID)
    }

    private fun register() {
        try {
            unregisterSessions()
            val l = MediaSessionManager.OnActiveSessionsChangedListener { list -> select(list) }
            manager.addOnActiveSessionsChangedListener(l, component)
            sessionsListener = l
            select(manager.getActiveSessions(component))
        } catch (se: SecurityException) {
            Log.i(TAG, "Notification access not granted yet; music island idle")
        } catch (t: Throwable) {
            Log.w(TAG, "register failed", t)
        }
    }

    private fun unregisterSessions() {
        sessionsListener?.let {
            try { manager.removeOnActiveSessionsChangedListener(it) } catch (t: Throwable) { Log.w(TAG, "remove failed", t) }
        }
        sessionsListener = null
    }

    private fun select(list: List<MediaController>?) {
        val pick = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (pick == null) {
            detach()
            DynamicIslandEngine.remove(ID)
            return
        }
        if (controller?.sessionToken != pick.sessionToken) {
            detach()
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                override fun onSessionDestroyed() {
                    detach()
                    DynamicIslandEngine.remove(ID)
                }
            }
            pick.registerCallback(cb)
            controller = pick
            callback = cb
        }
        publish()
    }

    private fun detach() {
        val c = controller
        val cb = callback
        if (c != null && cb != null) {
            try { c.unregisterCallback(cb) } catch (t: Throwable) { Log.w(TAG, "unregister cb failed", t) }
        }
        controller = null
        callback = null
    }

    private fun publish() {
        val c = controller ?: return
        val md = c.metadata
        val st = c.playbackState
        val state = st?.state ?: PlaybackState.STATE_NONE
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (title.isNullOrBlank() || state == PlaybackState.STATE_NONE || state == PlaybackState.STATE_STOPPED) {
            DynamicIslandEngine.remove(ID)
            return
        }
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        val duration = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val art: Bitmap? = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val icon = art?.let {
            // Perkecil agar hemat RAM (HP 4 GB), lalu bulatkan sudutnya.
            val thumb = ThumbnailUtils.extractThumbnail(it, ART_PX, ART_PX)
            RoundedBitmapDrawableFactory.create(context.resources, thumb).apply { cornerRadius = ART_PX / 5f }
        }

        val progressProvider: () -> Float = provider@{
            if (duration <= 0L) return@provider -1f
            val s = c.playbackState ?: return@provider -1f
            val pos = if (s.state == PlaybackState.STATE_PLAYING) {
                s.position + ((SystemClock.elapsedRealtime() - s.lastPositionUpdateTime) * s.playbackSpeed).toLong()
            } else s.position
            (pos.toFloat() / duration).coerceIn(0f, 1f)
        }

        val controls = c.transportControls
        DynamicIslandEngine.post(
            IslandActivity(
                id = ID, type = IslandType.MUSIC, priority = 50,
                title = title, subtitle = artist, glyph = "🎵", icon = icon,
                playing = playing, timeoutMs = if (playing) 0L else 8000L,
                liveProgress = progressProvider,
                actions = listOf(
                    IslandAction("⏮") { controls.skipToPrevious() },
                    IslandAction(if (playing) "⏸" else "▶") { if (playing) controls.pause() else controls.play() },
                    IslandAction("⏭") { controls.skipToNext() }
                )
            )
        )
    }

    companion object {
        private const val TAG = "MusicFeature"
        private const val ID = "music"
        private const val ART_PX = 96
    }
}
