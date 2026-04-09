package com.ismartcoding.plain.ui.base

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbRect = RectF()
    private val bubbleRect = RectF()
    private val thumbW = context.dp2px(4).toFloat()
    private val thumbRadius = context.dp2px(2).toFloat()
    private val bubbleW = context.dp2px(44).toFloat()
    private val bubbleH = context.dp2px(26).toFloat()
    private val bubbleRadius = context.dp2px(13).toFloat()
    private val gap = context.dp2px(8).toFloat()
    private val viewW = (bubbleW + gap + thumbW).toInt()
    private val viewH = context.dp2px(48)

    private var pdfView: PDFView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { isDragging = false; hide() }
    private var currentPos = 0f
    private var isDragging = false
    private var pageText = ""

    init {
        visibility = INVISIBLE
        thumbPaint.color = ContextCompat.getColor(context, R.color.scrollbar_primary)
        bubblePaint.color = 0xCC2C2C2E.toInt()
        textPaint.apply {
            color = 0xFFFFFFFF.toInt()
            textSize = context.dp2px(13).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    override fun setupLayout(pdfView: PDFView) {
        this.pdfView = pdfView
        val lp = RelativeLayout.LayoutParams(viewW, viewH)
        lp.addRule(RelativeLayout.ALIGN_PARENT_END)
        pdfView.addView(this, lp)
    }

    override fun destroyLayout() { pdfView?.removeView(this) }

    override fun setScroll(position: Float) {
        if (!shown()) show() else handler.removeCallbacks(hideRunnable)
        pdfView?.let { v ->
            val size = if (v.isSwipeVertical) v.height.toFloat() else v.width.toFloat()
            if (v.isSwipeVertical) y = (size * position - viewH / 2f).coerceIn(0f, size - viewH)
            else x = (size * position - viewW / 2f).coerceIn(0f, size - viewW)
        }
    }

    override fun hideDelayed() { handler.postDelayed(hideRunnable, 1200) }

    override fun setPageNum(pageNum: Int) {
        val text = pageNum.toString()
        if (pageText != text) { pageText = text; invalidate() }
    }

    override fun shown() = visibility == VISIBLE
    override fun show() { visibility = VISIBLE }
    override fun hide() { visibility = INVISIBLE }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat(); val w = width.toFloat()
        thumbRect.set(w - thumbW, 0f, w, h)
        canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint)
        if (isDragging && pageText.isNotEmpty()) {
            val top = (h - bubbleH) / 2f
            val right = w - thumbW - gap
            bubbleRect.set(right - bubbleW, top, right, top + bubbleH)
            canvas.drawRoundRect(bubbleRect, bubbleRadius, bubbleRadius, bubblePaint)
            val cy = bubbleRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(pageText, bubbleRect.centerX(), cy, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val v = pdfView ?: return super.onTouchEvent(event)
        if (v.pageCount == 0 || v.documentFitsView()) return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                v.stopFling(); handler.removeCallbacks(hideRunnable)
                isDragging = true; invalidate()
                currentPos = if (v.isSwipeVertical) event.rawY - y else event.rawX - x
                updatePosition(event); return true
            }
            MotionEvent.ACTION_MOVE -> { updatePosition(event); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                isDragging = false; invalidate(); hideDelayed(); v.performPageSnap(); return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updatePosition(event: MotionEvent) {
        val v = pdfView ?: return
        if (v.isSwipeVertical) {
            y = (event.rawY - currentPos).coerceIn(0f, v.height.toFloat() - viewH)
            v.setPositionOffset((y + viewH / 2f) / v.height.toFloat(), false)
        } else {
            x = (event.rawX - currentPos).coerceIn(0f, v.width.toFloat() - viewW)
            v.setPositionOffset((x + viewW / 2f) / v.width.toFloat(), false)
        }
    }
}

