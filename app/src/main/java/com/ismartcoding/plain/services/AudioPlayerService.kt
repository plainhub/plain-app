package com.ismartcoding.plain.services

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.features.audio.AudioServiceAction

@OptIn(UnstableApi::class)
class AudioPlayerService : MediaLibraryService() {

    lateinit var player: Player
    lateinit var session: MediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setRenderersFactory(
                DefaultRenderersFactory(this).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            )
            .build()

        val s = MediaLibrarySession.Builder(this, player,
            object : MediaLibrarySession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    val updatedMediaItems = mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()
                    return Futures.immediateFuture(updatedMediaItems)
                }
            })
            .setId(packageName)
        packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            s.setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    sessionIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        session = s.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        LogCat.d("onStartCommand: ${intent?.action}")
        when (intent?.action) {
            AudioServiceAction.QUIT.name -> {
                AudioPlayer.ignoreStateEnd = true
                AudioPlayer.pause()
            }

            AudioServiceAction.PENDING_QUIT.name -> {
                AudioPlayer.pendingQuit = true
            }
        }
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaSession()
    }

    private fun releaseMediaSession() {
        session.run {
            release()
            player.stop()
            player.release()
            AudioPlayer.release()
        }
    }
}
