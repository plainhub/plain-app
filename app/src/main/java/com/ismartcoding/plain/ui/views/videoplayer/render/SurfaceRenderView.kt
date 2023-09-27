package com.ismartcoding.plain.ui.views.videoplayer.render

import android.content.Context
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

class SurfaceRenderView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : SurfaceView(context, attrs, defStyleAttr), IRenderView {
        private val mMeasureHelper = MeasureHelper()
        private var aspectRatio = IRenderView.AR_ASPECT_FIT_PARENT
        private var mIRenderCallback: IRenderView.IRenderCallback? = null

        private val mCallback: SurfaceHolder.Callback =
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mIRenderCallback?.onSurfaceCreated(holder.surface, width, height)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    mIRenderCallback?.onSurfaceChanged(holder.surface, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mIRenderCallback?.onSurfaceDestroyed(holder.surface)
                }
            }

        init {
            holder.addCallback(mCallback)
        }

        override fun setAspectRatio(aspectRatio: Int) {
            this.aspectRatio = aspectRatio
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            mMeasureHelper.onMeasure(widthMeasureSpec, heightMeasureSpec, aspectRatio)
            setMeasuredDimension(mMeasureHelper.getMeasureWidth(), mMeasureHelper.getMeasureHeight())
        }

        override fun getView(): View {
            return this
        }

        override fun getSurface(): Surface? {
            holder?.surface?.let {
                if (it.isValid) {
                    return it
                }
            }
            return null
        }

        override fun setVideoSize(
            videoWidth: Int,
            videoHeight: Int,
        ) {
            if (videoWidth > 0 && videoHeight > 0) {
                mMeasureHelper.setVideoSize(videoWidth, videoHeight)
                holder.setFixedSize(videoWidth, videoHeight)
                requestLayout()
            }
        }

        override fun setVideoRotation(degree: Int) {
        }

        override fun setRenderCallback(callback: IRenderView.IRenderCallback?) {
            mIRenderCallback = callback
        }

        override fun removeRenderCallback() {
            mIRenderCallback = null
        }
    }
