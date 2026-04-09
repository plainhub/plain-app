package com.ismartcoding.plain.ui.base

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.pdfviewer.PDFView
import com.ismartcoding.lib.pdfviewer.scroll.ScrollHandle
import com.ismartcoding.plain.R

class MinimalScrollHandle(context: Context) : View(context), ScrollHandle {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val thumbWidth = context.dp2px(4)
    private val thumbHeight = context.dp2px(48)
    private val cornerRadius: Float = context.dp2px(2).toFloat()
    private var pdfView: PDFView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hide() }
    private var currentPos = 0f

    init {
        visibility = INVISIBLE
        paint.color = ContextCompat.getColor(context, R.color.scrollbar_primary)
    }

    override fun setupLayout(pdfView: PDFView) {
        this.pdfView = pdfView
        val lp = RelativeLayout.LayoutParams(thumbWidth, thumbHeight)
        lp.addRule(RelativeLayout.ALIGN_PARENT_END)
        pdfView.addView(this, lp)
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        if (!shown()) show() else handler.removeCallbacks(hideRunnable)
        pdfView?.let { v ->
            val viewSize = if (v.isSwipeVertical) v.height.toFloat() else v.width.toFloat()
            val newPos = (viewSize * position - thumbHeight / 2f).coerceIn(0f, viewSize - thumbHeight)
            if (v.isSwipeVertical) y = newPos else x = newPos
        }
    }

    override fun hideDelayed() {
        handler.postDelayed(hideRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {}

    override fun shown() = visibility == VISIBLE

    override fun show() {
        visibility = VISIBLE
    }

    override fun hide() {
        visibility = INVISIBLE
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val v = pdfView ?: return super.onTouchEvent(event)
        if (v.pageCount == 0 || v.documentFitsView()) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                v.stopFling()
                handler.removeCallbacks(hideRunnable)
                currentPos = if (v.isSwipeVertical) event.rawY - y else event.rawX - x
                updatePosition(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updatePosition(event)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                hideDelayed()
                v.performPageSnap()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updatePosition(event: MotionEvent) {
        val v = pdfView ?: return
        if (v.isSwipeVertical) {
            val newY = event.rawY - currentPos
            val viewSize = v.height.toFloat()
            y = newY.coerceIn(0f, viewSize - thumbHeight)
            v.setPositionOffset((y + thumbHeight / 2f) / viewSize, false)
        } else {
            val newX = event.rawX - currentPos
            val viewSize = v.width.toFloat()
            x = newX.coerceIn(0f, viewSize - thumbWidth)
            v.setPositionOffset((x + thumbWidth / 2f) / viewSize, false)
        }
    }
}
