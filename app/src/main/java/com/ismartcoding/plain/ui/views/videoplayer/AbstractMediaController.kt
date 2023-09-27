package com.ismartcoding.plain.ui.views.videoplayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.Surface
import android.widget.FrameLayout
import com.ismartcoding.lib.media.IVideoPlayer
import com.ismartcoding.lib.media.VideoPlayer
import com.ismartcoding.plain.ui.views.videoplayer.render.IRenderView

abstract class AbstractMediaController
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr), IVideoPlayer.PlayerCallback {
        /** 播放器 */
        protected var mMediaPlayer: VideoPlayer? = null

        /** 渲染层 */
        protected var mRenderView: IRenderView? = null
            private set

        /** 播放信息的更新间隔（默认 200毫秒），防止短视频出现进度卡顿现象 */
        protected var updatePlayerInfoDelay = 200

        /** 更新播放器信息 */
        protected val MSG_UPDATE_PLAYER_INFO = 1

        private val mHandler: Handler =
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == MSG_UPDATE_PLAYER_INFO) {
                        mMediaPlayer?.let {
                            autoUpdateInfo(it)

                            val pos = it.getCurrentPosition()
                            val delayMillis = updatePlayerInfoDelay - pos % updatePlayerInfoDelay
                            if (hasMessages(MSG_UPDATE_PLAYER_INFO)) {
                                removeMessages(MSG_UPDATE_PLAYER_INFO)
                            }
                            sendMessageDelayed(obtainMessage(MSG_UPDATE_PLAYER_INFO), delayMillis)
                        }
                    } else {
                        onHandleMessage(msg)
                    }
                }
            }

        /**
         * 自动更新播放信息
         */
        protected open fun autoUpdateInfo(mp: VideoPlayer) {
            updateCurrentPosition(mp.getCurrentPosition())
        }

        /**
         * 定时更新播放信息
         */
        private fun handlerUpdatePlayerInfo() {
            stopUpdatePlayerInfo()
            mHandler.sendEmptyMessage(MSG_UPDATE_PLAYER_INFO)
        }

        /**
         * 取消定时更新播放信息
         */
        fun stopUpdatePlayerInfo() {
            if (mHandler.hasMessages(MSG_UPDATE_PLAYER_INFO)) {
                mHandler.removeMessages(MSG_UPDATE_PLAYER_INFO)
            }
        }

        /**
         * 子类处理handle消息
         */
        protected open fun onHandleMessage(msg: Message) {}

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            mHandler.removeCallbacksAndMessages(null)
        }

        /**
         * 初始化画面渲染层
         * 自动添加RenderCallback
         *
         * @param renderView
         */
        protected fun initRendView(renderView: IRenderView?) {
            if (renderView != null) {
                initRendView(
                    renderView,
                    object : IRenderView.IRenderCallback {
                        override fun onSurfaceCreated(
                            surface: Surface?,
                            width: Int,
                            height: Int,
                        ) {
                            keepScreenOn = true
                            mMediaPlayer?.let {
                                it.setSurface(surface)
                                val videoWidth = it.getVideoWidth()
                                val videoHeight = it.getVideoHeight()
                                if (videoWidth != 0 && videoHeight != 0) {
                                    renderView.setVideoSize(videoWidth, videoHeight)
                                }
                            }
                        }

                        override fun onSurfaceChanged(
                            surface: Surface?,
                            width: Int,
                            height: Int,
                        ) {
                        }

                        override fun onSurfaceDestroyed(surface: Surface?) {
                            keepScreenOn = false
                            mMediaPlayer?.releaseSurface(surface)
                        }
                    },
                )
            }
        }

        /**
         * 初始化画面渲染层
         *
         * @param renderView
         * @param renderCallback 自定义渲染回调
         */
        private fun initRendView(
            renderView: IRenderView?,
            renderCallback: IRenderView.IRenderCallback?,
        ) {
            if (renderView != null) {
                mRenderView = renderView
                if (renderCallback != null) {
                    renderView.setRenderCallback(renderCallback)
                }
            }
        }

        /**
         * 解除绑定播放器
         * 内部会置空播放器的 监听器 以及 surface
         */
        open fun unBindMediaPlayer() {
            mMediaPlayer?.let {
                it.removePlayerCallback()
                it.setSurface(null)
                mMediaPlayer = null
            }
            mHandler.removeCallbacksAndMessages(null)
        }

        /**
         * 绑定播放器
         *
         * @param mediaPlayer
         */
        open fun bindMediaPlayer(mediaPlayer: VideoPlayer) {
            mMediaPlayer = mediaPlayer
            mediaPlayer.setPlayerCallback(this)
            mRenderView?.getSurface()?.let {
                mediaPlayer.setSurface(it)
            } ?: mediaPlayer.releaseSurface(null)
        }

        /**
         * @return 是否已绑定播放器
         */
        fun hadBindMediaPlayer(): Boolean {
            return mMediaPlayer != null
        }

        protected open fun updatePlayerState(isPlaying: Boolean) {}

        protected open fun updateCurrentPosition(position: Long) {}

        protected open fun updateDuration(duration: Long) {}

        open fun play() {
            mMediaPlayer?.let {
                if (!it.isInPlaybackState()) {
                    it.prepareAsync()
                }
                it.play()
            }
            updatePlayerState(true)
            handlerUpdatePlayerInfo()
        }

        open fun pause() {
            mMediaPlayer?.pause()
            updatePlayerState(false)
            stopUpdatePlayerInfo()
        }

        open fun seekTo(pos: Long) {
            mMediaPlayer?.seekTo(pos)
        }

        open fun stop() {
            mMediaPlayer?.stop()
            updatePlayerState(false)
        }

        open fun release() {
            mMediaPlayer?.release()
            updatePlayerState(false)
        }

        override fun onPreparing(mp: IVideoPlayer) {}

        override fun onPrepared(mp: IVideoPlayer) {
            updateDuration(mp.getDuration())
        }

        override fun onCompletion(mp: IVideoPlayer) {
            stopUpdatePlayerInfo()

            updatePlayerState(false)
            updateCurrentPosition(mp.getDuration())
        }

        override fun onBufferingUpdate(mp: IVideoPlayer) {}

        override fun onSeekComplete(mp: IVideoPlayer) {}

        override fun onVideoSizeChanged(
            mp: IVideoPlayer,
            width: Int,
            height: Int,
        ) {
            if (width != 0 && height != 0) {
                mRenderView?.setVideoSize(width, height)
            }
        }

        override fun onError(
            mp: IVideoPlayer,
            what: Int,
            extra: Int,
        ): Boolean {
            updatePlayerState(false)
            return false
        }

        override fun onInfo(
            mp: IVideoPlayer,
            what: Int,
            extra: Int,
        ): Boolean {
            when (what) {
                3 -> { // MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START 开始渲染视频
                }
                10001 -> { // IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED 视频方向变化
                    mRenderView?.setVideoRotation(extra)
                }
            }
            return false
        }
    }
