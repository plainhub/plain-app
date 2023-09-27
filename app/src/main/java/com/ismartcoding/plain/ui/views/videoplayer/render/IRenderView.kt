package com.ismartcoding.plain.ui.views.videoplayer.render

import android.view.Surface
import android.view.View

interface IRenderView {
    companion object {
        /** 适应最长边 */
        const val AR_ASPECT_FIT_PARENT = 0

        /** 适应最短边，裁剪长边 */
        const val AR_ASPECT_FILL_PARENT = 1
    }

    fun getView(): View

    fun getSurface(): Surface?

    /**
     * 设置显示比例模式
     */
    fun setAspectRatio(aspectRatio: Int)

    fun setVideoSize(
        videoWidth: Int,
        videoHeight: Int,
    )

    fun setVideoRotation(degree: Int)

    fun setRenderCallback(callback: IRenderCallback?)

    fun removeRenderCallback()

    fun releaseSurface()  {}

    interface IRenderCallback {
        fun onSurfaceCreated(
            surface: Surface?,
            width: Int,
            height: Int,
        )

        fun onSurfaceChanged(
            surface: Surface?,
            width: Int,
            height: Int,
        )

        fun onSurfaceDestroyed(surface: Surface?)
    }
}
