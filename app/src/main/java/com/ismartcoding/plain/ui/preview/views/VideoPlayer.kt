package com.ismartcoding.plain.ui.preview.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.ismartcoding.plain.ui.preview.utils.FakeDragHelper
import com.ismartcoding.plain.ui.views.videoplayer.PlayerControllerView

class VideoPlayer
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : PlayerControllerView(context, attrs, defStyleAttr) {
        private val fakeDragHelper = FakeDragHelper(context)
        private var singleTouch = true

        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
            handleDispatchTouchEvent(event)
            return super.dispatchTouchEvent(event)
        }

        private fun handleDispatchTouchEvent(event: MotionEvent?) {
            when (event?.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    singleTouch = false
                    animate()
                        .translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setDuration(200).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    fakeDragHelper.up(this) {
                        singleTouch = it
                    }
                MotionEvent.ACTION_MOVE -> {
                    if (singleTouch) {
                        fakeDragHelper.drag(this@VideoPlayer, event.rawX, event.rawY)
                    }
                }
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            animate().cancel()
        }
    }
