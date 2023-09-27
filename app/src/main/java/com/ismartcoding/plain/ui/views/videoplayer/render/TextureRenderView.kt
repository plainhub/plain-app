package com.ismartcoding.plain.ui.views.videoplayer.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View

class TextureRenderView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : TextureView(context, attrs, defStyleAttr), IRenderView {
        private val mMeasureHelper = MeasureHelper()
        private var aspectRatio = IRenderView.AR_ASPECT_FIT_PARENT
        private var mIRenderCallback: IRenderView.IRenderCallback? = null

        private var mSurface: Surface? = null

        private val mSurfaceTextureListener: SurfaceTextureListener =
            object : SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    mSurface = Surface(surface)
                    mIRenderCallback?.onSurfaceCreated(mSurface, width, height)
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    mIRenderCallback?.onSurfaceChanged(mSurface, width, height)
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    mIRenderCallback?.onSurfaceDestroyed(mSurface)
                    releaseSurface()
                    surface.release() // If returns false, the client needs to call SurfaceTexture#release()
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) { // 纹理更新
                }
            }

        init {
            surfaceTextureListener = mSurfaceTextureListener
        }

        override fun releaseSurface() {
            mSurface?.release()
            mSurface = null
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
            if (mSurface?.isValid == true) {
                return mSurface
            }
            surfaceTexture?.let {
                return Surface(it).apply {
                    mSurface = this
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
                requestLayout()
            }
        }

        override fun setVideoRotation(degree: Int) {
            mMeasureHelper.setVideoRotationDegree(degree)
            rotation = degree.toFloat()
        }

        override fun setRenderCallback(callback: IRenderView.IRenderCallback?) {
            mIRenderCallback = callback
        }

        override fun removeRenderCallback() {
            mIRenderCallback = null
        }
    }
