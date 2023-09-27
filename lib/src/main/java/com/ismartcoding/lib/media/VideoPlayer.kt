package com.ismartcoding.lib.media

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.Surface
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.ismartcoding.lib.logcat.LogCat

class VideoPlayer : IVideoPlayer {
    private var mMediaPlayer: ExoPlayer? = null

    /** 当前播放器状态 */
    var currentState = STATE_UNKNOWN
        protected set

    /** 目标状态 */
    var targetState = STATE_UNKNOWN
        protected set

    protected var videoModel: VideoModel? = null

    protected var mAudioManager: AudioManager? = null
    protected var mSurface: Surface? = null

    /** 缓冲标记 */
    protected var buffering: Boolean = false

    /** 当前缓冲进度 0-100 */
    protected var bufferPercentage = 0

    private var mPlayerCallback: IVideoPlayer.PlayerCallback? = null

    override var isPausedByTransientLossOfFocus = false
    private val audioFocusRequest = AudioFocusHelper.createRequest(this)

    override fun setMediaSource(
        context: Context,
        videoModel: VideoModel,
    ) {
        this.videoModel = videoModel
        if (mAudioManager == null) {
            mAudioManager =
                context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        mMediaPlayer.let { player ->
            if (player == null) {
                mMediaPlayer =
                    createPlayer(context).also {
                        setMediaSource(context, it, videoModel)
                    }
            } else {
                setMediaSource(context, player, videoModel)
            }
        }
    }

    private fun createPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            addListener(mPlayerEventListener)
        }
    }

    private fun setMediaSource(
        context: Context,
        player: ExoPlayer,
        videoModel: VideoModel,
    ) {
        mSurface?.let {
            player.setVideoSurface(it)
        }
        if (videoModel.looping) {
            player.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            player.repeatMode = Player.REPEAT_MODE_OFF
        }
        if (videoModel.isMute) {
            player.volume = 0f
        } else {
            player.volume = 1f
        }
        val mediaItemBuilder = MediaItem.Builder()
        mediaItemBuilder.setUri(videoModel.uri)
        mediaItemBuilder.setMimeType(
            Util.getAdaptiveMimeTypeForContentType(
                Util.inferContentType(
                    videoModel.uri,
                ),
            ),
        )
        val factory = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
        val source = factory.createMediaSource(mediaItemBuilder.build())
        player.setMediaSource(source)
        currentState = STATE_IDLE
    }

    override fun prepareAsync() {
        mAudioManager?.let {
            AudioManagerCompat.requestAudioFocus(
                it,
                audioFocusRequest,
            )
        }
        mMediaPlayer?.let {
            notifyOnPreparing()
            it.prepare()
            currentState = STATE_PREPARING
        }
    }

    override fun play() {
        targetState = STATE_PLAYING
        if (currentState == STATE_PLAYBACK_COMPLETED) {
            mMediaPlayer?.apply {
                seekTo(0)
            }
            currentState = STATE_PLAYING
        } else {
            if (isInPlaybackState()) {
                mMediaPlayer?.play()
                currentState = STATE_PLAYING
            }
        }
    }

    override fun pause() {
        targetState = STATE_PAUSED
        if (isInPlaybackState()) {
            mMediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    currentState = STATE_PAUSED
                }
            }
        }
    }

    override fun stop() {
        abandonFocus()
        mMediaPlayer?.stop()
        currentState = STATE_STOP
        targetState = STATE_STOP
    }

    override fun release() {
        abandonFocus()
        mAudioManager = null
        mMediaPlayer?.let {
            it.release()
            currentState = STATE_RELEASE
        }
        targetState = STATE_RELEASE
        mMediaPlayer = null
        bufferPercentage = 0
        buffering = false
        notifyOnBufferingUpdate()
    }

    override fun seekTo(pos: Long) {
        seekTo(pos, SeekParameters.EXACT)
    }

    fun quickSeekTo(pos: Long) {
        seekTo(pos, SeekParameters.PREVIOUS_SYNC)
    }

    fun seekTo(
        pos: Long,
        mode: SeekParameters,
    ) {
        if (isInPlaybackState()) {
            mMediaPlayer?.setSeekParameters(mode)
            mMediaPlayer?.seekTo(pos)
            videoModel?.seekPosition = 0
        } else {
            videoModel?.seekPosition = pos
        }
    }

    override fun isPlaying(): Boolean {
        return mMediaPlayer?.isPlaying ?: false
    }

    override fun getCurrentPosition(): Long {
        mMediaPlayer?.let {
            if (it.currentPosition > 0) {
                return it.currentPosition
            }
        }
        return 0
    }

    override fun getDuration(): Long {
        if (isInPlaybackState()) {
            return mMediaPlayer?.duration ?: 0
        }
        return 0
    }

    override fun getNetSpeed(): Long {
        return 0
    }

    override fun getVideoWidth(): Int {
        return mMediaPlayer?.videoSize?.width ?: 0
    }

    override fun getVideoHeight(): Int {
        return mMediaPlayer?.videoSize?.height ?: 0
    }

    override fun setSurface(surface: Surface?) {
        mSurface = surface
        mMediaPlayer?.setVideoSurface(surface)
    }

    override fun releaseSurface(surface: Surface?) {
        if (surface == null || surface == mSurface) {
            mSurface = null
            mMediaPlayer?.setVideoSurface(null)
        }
    }

    override fun setVolume(volume: Float) {
        mMediaPlayer?.volume = volume
    }

    private val mPlayerEventListener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        buffering = false
                        notifyOnBufferingUpdate()
                        if (!isInPlaybackState()) {
                            currentState = STATE_PREPARED
                            notifyOnPrepared()
                            // 获取视频跳转位置, 这个跳转位置需要设置完视频地址后马上调用 seekTo() 方法
                            val seekToPosition = videoModel?.seekPosition ?: 0
                            if (seekToPosition != 0L && seekToPosition < (
                                    mMediaPlayer?.duration
                                        ?: 0
                                )
                            ) {
                                seekTo(seekToPosition)
                            }
                        }
                        if (targetState == STATE_PLAYING) {
                            play()
                        }
                    }

                    Player.STATE_ENDED -> {
                        buffering = false
                        notifyOnBufferingUpdate()
                        currentState = STATE_PLAYBACK_COMPLETED
                        notifyOnCompletion()
                    }
                    Player.STATE_BUFFERING -> {
                        buffering = true
                        notifyOnBufferingUpdate()
                    }
                    Player.STATE_IDLE -> {
                        currentState = STATE_IDLE
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                LogCat.e(error.message ?: "")
                currentState = STATE_ERROR
                notifyOnError(error.errorCode, 0)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                notifyOnSeekComplete()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    currentState = STATE_PLAYING
                } else {
                    if (currentState == STATE_PLAYING) {
                        currentState = STATE_PAUSED
                    }
                    notifyOnBufferingUpdate()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                notifyOnVideoSizeChanged(videoSize.width, videoSize.height)
            }
        }

    /** 所有有可能的播放状态 */
    companion object {
        /** 未知状态 */
        const val STATE_UNKNOWN = -2

        /** 错误 */
        const val STATE_ERROR = -1

        /** 初始化 */
        const val STATE_IDLE = 0

        /** 预处理中 */
        const val STATE_PREPARING = 1

        /** 预处理完成 */
        const val STATE_PREPARED = 2

        /** 播放中 */
        const val STATE_PLAYING = 3

        /** 暂停 */
        const val STATE_PAUSED = 4

        /** 播放完成 */
        const val STATE_PLAYBACK_COMPLETED = 5

        /** 播放停止 */
        const val STATE_STOP = 6

        /** 播放已释放 */
        const val STATE_RELEASE = 7
    }

    private fun abandonFocus() {
        mAudioManager?.let {
            AudioManagerCompat.abandonAudioFocusRequest(it, audioFocusRequest)
        }
    }

    override fun getPlayUri(): Uri? {
        return videoModel?.uri
    }

    /**
     * @return 是否在播放状态
     */
    fun isInPlaybackState(): Boolean {
        return mMediaPlayer != null && currentState >= STATE_PREPARED && currentState <= STATE_PLAYBACK_COMPLETED
    }

    fun setPlayerCallback(callback: IVideoPlayer.PlayerCallback) {
        mPlayerCallback = callback
    }

    fun removePlayerCallback() {
        mPlayerCallback = null
    }

    override fun isBuffering(): Boolean {
        return buffering
    }

    override fun getBufferedPercentage(): Int {
        return bufferPercentage
    }

    private fun notifyOnPreparing() {
        mPlayerCallback?.onPreparing(this)
    }

    private fun notifyOnPrepared() {
        mPlayerCallback?.onPrepared(this)
    }

    private fun notifyOnCompletion() {
        mPlayerCallback?.onCompletion(this)
    }

    private fun notifyOnBufferingUpdate() {
        mPlayerCallback?.onBufferingUpdate(this)
    }

    private fun notifyOnSeekComplete() {
        mPlayerCallback?.onSeekComplete(this)
    }

    private fun notifyOnVideoSizeChanged(
        width: Int,
        height: Int,
    ) {
        mPlayerCallback?.onVideoSizeChanged(this, width, height)
    }

    private fun notifyOnError(
        what: Int,
        extra: Int,
    ): Boolean {
        return mPlayerCallback?.onError(this, what, extra) ?: false
    }

    private fun notifyOnInfo(
        what: Int,
        extra: Int,
    ): Boolean {
        return mPlayerCallback?.onInfo(this, what, extra) ?: false
    }
}
