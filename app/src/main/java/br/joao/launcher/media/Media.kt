package br.joao.launcher.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import androidx.compose.ui.graphics.asImageBitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Serviço vazio: só existe para o Android liberar o acesso às sessões de mídia (player). */
class MediaListenerService : NotificationListenerService()

data class NowPlaying(
    val title: String,
    val artist: String,
    val app: String,
    /** Álbum e código da música no app (no YouTube Music pode ser o código do vídeo). */
    val album: String,
    val mediaId: String,
    val packageName: String,
    val playing: Boolean,
    val durationMs: Long,
    private val positionMs: Long,
    private val updatedAt: Long,
    private val speed: Float,
    val canLike: Boolean,
    /** Capa do álbum que o app de música manda junto (pode não vir). */
    val art: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    fun positionNow(): Long {
        if (!playing) return positionMs
        val elapsed = SystemClock.elapsedRealtime() - updatedAt
        val pos = positionMs + (elapsed * speed).toLong()
        return if (durationMs > 0) pos.coerceIn(0, durationMs) else pos
    }
}

class MediaMonitor(private val context: Context) {
    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private var controller: MediaController? = null
    private var started = false

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() {
            pick(runCatching { manager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList()))
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list ?: emptyList())
    }

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun start() {
        if (!hasAccess()) return
        if (!started) {
            runCatching { manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent) }
            started = true
        }
        pick(runCatching { manager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList()))
    }

    fun stop() {
        if (started) runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
        started = false
    }

    private fun pick(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull { it.metadata != null }
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val md = c?.metadata
        if (c == null || md == null) {
            _state.value = null
            return
        }
        val ps = c.playbackState
        val appLabel = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault("")
        _state.value = NowPlaying(
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            app = appLabel,
            album = md.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
            mediaId = md.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: "",
            packageName = c.packageName,
            playing = ps?.state == PlaybackState.STATE_PLAYING,
            durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = ps?.position ?: 0,
            updatedAt = ps?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = ps?.playbackSpeed ?: 1f,
            canLike = likeAction(ps) != null,
            art = artOf(md),
        )
    }

    /** Reaproveita a mesma imagem enquanto a música não muda (o player manda o estado várias vezes). */
    private var lastArtKey: String? = null
    private var lastArt: androidx.compose.ui.graphics.ImageBitmap? = null

    private fun artOf(md: MediaMetadata): androidx.compose.ui.graphics.ImageBitmap? {
        val key = (md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "") + "|" + (md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "")
        val bmp = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bmp == null) {
            // a capa às vezes chega um instante depois do título
            return if (key == lastArtKey) lastArt else null
        }
        if (key == lastArtKey && lastArt != null && lastArt!!.width == bmp.width) return lastArt
        val img = runCatching {
            val small = if (bmp.width > 360) android.graphics.Bitmap.createScaledBitmap(bmp, 360, 360 * bmp.height / bmp.width, true) else bmp
            small.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        }.getOrNull()?.asImageBitmap()
        lastArtKey = key
        lastArt = img
        return img
    }

    private fun likeAction(ps: PlaybackState?): PlaybackState.CustomAction? =
        ps?.customActions?.firstOrNull { a ->
            val n = "${a.action} ${a.name}".lowercase()
            listOf("like", "thumb", "gostei", "curtir", "favorit").any { n.contains(it) } && !n.contains("dislike")
        }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun pause() = controller?.transportControls?.pause()

    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()
    fun like() {
        val c = controller ?: return
        likeAction(c.playbackState)?.let { c.transportControls.sendCustomAction(it, null) }
    }

    fun openApp(): String? = controller?.packageName
}
