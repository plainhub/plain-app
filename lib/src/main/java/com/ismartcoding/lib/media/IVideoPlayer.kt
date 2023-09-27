package com.ismartcoding.lib.media

import android.content.Context
import android.net.Uri
import android.view.Surface

interface IVideoPlayer : IMediaPlayer {
    /**
     * 设置数据源
     *
     * @param context
     * @param videoModel
     */
    fun setMediaSource(
        context: Context,
        videoModel: VideoModel,
    )

    /**
     * 异步加载
     */
    fun prepareAsync()

    /**
     * 释放内核
     */
    fun release()

    /**
     * 跳转
     *
     * @param pos
     */
    fun seekTo(pos: Long)

    /**
     * 播放地址
     */
    fun getPlayUri(): Uri?

    /**
     * 是否正在缓冲
     */
    fun isBuffering(): Boolean

    /**
     * 获取当前播放时间
     */
    fun getCurrentPosition(): Long

    /**
     * 获取时长
     */
    fun getDuration(): Long

    /**
     * 缓存进度百分比
     */
    fun getBufferedPercentage(): Int

    /**
     * 网络速度
     */
    fun getNetSpeed(): Long

    /**
     * 获取视频宽
     */
    fun getVideoWidth(): Int

    /**
     * 获取视频高
     */
    fun getVideoHeight(): Int

    /**
     * 设置视频输出
     *
     * @param surface
     */
    fun setSurface(surface: Surface?)

    /**
     * 释放渲染
     *
     * @param surface
     */
    fun releaseSurface(surface: Surface?)

    // -------- 播放器回调 --------
    interface PlayerCallback {
        fun onPreparing(mp: IVideoPlayer)

        fun onPrepared(mp: IVideoPlayer)

        fun onCompletion(mp: IVideoPlayer)

        fun onBufferingUpdate(mp: IVideoPlayer)

        fun onSeekComplete(mp: IVideoPlayer)

        fun onVideoSizeChanged(
            mp: IVideoPlayer,
            width: Int,
            height: Int,
        )

        fun onError(
            mp: IVideoPlayer,
            what: Int,
            extra: Int,
        ): Boolean

        fun onInfo(
            mp: IVideoPlayer,
            what: Int,
            extra: Int,
        ): Boolean
    }
}
