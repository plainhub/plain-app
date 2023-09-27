package com.ismartcoding.plain.ui.preview.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.ismartcoding.plain.ui.preview.PreviewViewModel
import com.ismartcoding.plain.ui.preview.utils.FakeDragHelper
import com.ismartcoding.plain.ui.preview.utils.ViewModelUtils.provideViewModel

class SubsamplingScaleImageView2
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : SubsamplingScaleImageView(context, attrs) {
        private val viewModel by lazy { provideViewModel(this, PreviewViewModel::class.java) }
        private var initScale: Float? = null
        private val fakeDrag = FakeDragHelper(context)
        private var singleTouch = true

        init {
            setOnImageEventListener(
                object : DefaultOnImageEventListener() {
                    override fun onImageLoaded() {
                        initScale = null
                    }
                },
            )
        }

        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
            handleDispatchTouchEvent(event)
            return super.dispatchTouchEvent(event)
        }

        private fun handleDispatchTouchEvent(event: MotionEvent?) {
            when (event?.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    setSingleTouch(false)
                    animate()
                        .translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setDuration(200).start()
                }
                MotionEvent.ACTION_DOWN -> if (initScale == null) initScale = scale
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    fakeDrag.up(this) {
                        setSingleTouch(it)
                    }
                MotionEvent.ACTION_MOVE -> {
                    if (singleTouch && scale == initScale) {
                        fakeDrag.drag(this@SubsamplingScaleImageView2, event.rawX, event.rawY)
                    }
                }
            }
        }

        private fun setSingleTouch(value: Boolean) {
            singleTouch = value
            viewModel?.setViewerUserInputEnabled(value)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            animate().cancel()
        }
    }
