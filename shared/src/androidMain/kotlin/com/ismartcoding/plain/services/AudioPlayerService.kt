package com.ismartcoding.plain.services

import com.ismartcoding.plain.preferences.*
import com.ismartcoding.plain.appContext

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.media.AndroidAudioPlayer
import com.ismartcoding.plain.features.audio.AudioCommands
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.features.audio.AudioServiceAction
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.platform.media.audioSeekTo
import com.ismartcoding.plain.platform.media.audioSetPlaybackSpeed

@OptIn(UnstableApi::class)
class AudioPlayerService : MediaLibraryService() {
    companion object {
        var nativePlayer: Player? = null
            private set
    }
    private lateinit var player: Player
    private lateinit var session: MediaLibrarySession

    private fun createPlayer(): Player {
        val attributes = AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build()
        val dataSourceFactory = DefaultDataSource.Factory(this, androidx.media3.datasource.DataSource.Factory {
            val output = com.ismartcoding.plain.features.audio.AudioCommands.output.value
            if (output.source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
                com.ismartcoding.plain.platform.media.PeerResourceDataSource(checkNotNull(output.resource))
            } else androidx.media3.datasource.DefaultHttpDataSource.Factory().createDataSource()
        })
        val extractorsFactory = DefaultExtractorsFactory()
        extractorsFactory.setConstantBitrateSeekingAlwaysEnabled(false)
        extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
        return ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                attributes, true
            )
            .setHandleAudioBecomingNoisy(true).setRenderersFactory(
                DefaultRenderersFactory(this).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            ).build()
    }

    override fun onCreate() {
        super.onCreate()
        if (::session.isInitialized) {
            LogCat.w("AudioPlayerService already created, skipping initialization.")
            return
        }
        player = createPlayer()
        nativePlayer = player
        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun play() = audioPlay()
            override fun pause() = audioPause()
            override fun seekTo(positionMs: Long) = audioSeekTo(positionMs)
            override fun setPlaybackSpeed(speed: Float) = audioSetPlaybackSpeed(speed)
            override fun stop() = audioPause()
            override fun seekToPrevious() {
                AudioCommands.submit(AudioCommand.PREVIOUS)
            }

            override fun getAvailableCommands(): Player.Commands {
                // make sure play next is always visible in notification center
                return MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            }

            override fun isCommandAvailable(command: Int): Boolean {
                // make sure play next is always visible in notification center
                return true
            }

            override fun seekToNext() {
                AudioCommands.submit(AudioCommand.NEXT)
            }
        }
        val s = MediaLibrarySession.Builder(this, forwardingPlayer, object : MediaLibrarySession.Callback {
        }).setId(packageName)
        val mainActivityClass = Class.forName("com.ismartcoding.plain.MainActivity")
        val sessionIntent = Intent(this, mainActivityClass).apply {
            `package` = packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        s.setSessionActivity(
            PendingIntent.getActivity(
                this,
                0,
                sessionIntent,
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        session = s.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action != null) {
            LogCat.d("onStartCommand: ${intent.action}")
        }
        when (intent?.action) {
            AudioServiceAction.QUIT.name -> {
                audioPause()
            }

        }
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        releaseMediaSession()
        super.onDestroy()
    }

    private fun releaseMediaSession() {
        AndroidAudioPlayer.release()
        session.release()
        player.stop()
        nativePlayer = null
        player.release()
    }
}


