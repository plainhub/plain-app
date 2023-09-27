package com.ismartcoding.lib.pdfviewer.scroll

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.*
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ismartcoding.lib.R
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.pdfviewer.PDFView

class DefaultScrollHandle
    @JvmOverloads
    constructor(context: Context, private val inverted: Boolean = false) : RelativeLayout(context), ScrollHandle {
        private var relativeHandlerMiddle = 0f
        private var textView = TextView(context)
        private var pdfView: PDFView? = null
        private var currentPos = 0f
        private val _handler = Handler(Looper.getMainLooper())
        private val hidePageScrollerRunnable = Runnable { hide() }

        init {
            visibility = INVISIBLE
            setTextColor(Color.BLACK)
            setTextSize(DEFAULT_TEXT_SIZE)
        }

        override fun setupLayout(pdfView: PDFView) {
            val align: Int
            val width: Int
            val height: Int
            val background: Drawable?
            // determine handler position, default is right (when scrolling vertically) or bottom (when scrolling horizontally)
            if (pdfView.isSwipeVertical) {
                width = HANDLE_LONG
                height = HANDLE_SHORT
                if (inverted) { // left
                    align = ALIGN_PARENT_LEFT
                    background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_left)
                } else { // right
                    align = ALIGN_PARENT_RIGHT
                    background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_right)
                }
            } else {
                width = HANDLE_SHORT
                height = HANDLE_LONG
                if (inverted) { // top
                    align = ALIGN_PARENT_TOP
                    background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_top)
                } else { // bottom
                    align = ALIGN_PARENT_BOTTOM
                    background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_bottom)
                }
            }
            setBackground(background)
            val lp = LayoutParams(context.dp2px(width), context.dp2px(height))
            lp.setMargins(0, 0, 0, 0)
            val tvlp = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tvlp.addRule(CENTER_IN_PARENT, TRUE)
            addView(textView, tvlp)
            lp.addRule(align)
            pdfView.addView(this, lp)
            this.pdfView = pdfView
        }

        override fun destroyLayout() {
            pdfView?.removeView(this)
        }

        override fun setScroll(position: Float) {
            if (!shown()) {
                show()
            } else {
                _handler.removeCallbacks(hidePageScrollerRunnable)
            }

            pdfView?.let {
                setPosition((if (it.isSwipeVertical) it.height else it.width) * position)
            }
        }

        private fun setPosition(pos: Float) {
            var newPosition = pos
            if (java.lang.Float.isInfinite(newPosition) || java.lang.Float.isNaN(newPosition)) {
                return
            }
            val pdfViewSize =
                if (pdfView!!.isSwipeVertical) {
                    pdfView!!.height.toFloat()
                } else {
                    pdfView!!.width.toFloat()
                }
            newPosition -= relativeHandlerMiddle
            if (newPosition < 0) {
                newPosition = 0f
            } else if (newPosition > pdfViewSize - context.dp2px(HANDLE_SHORT)) {
                newPosition = pdfViewSize - context.dp2px(HANDLE_SHORT)
            }
            if (pdfView!!.isSwipeVertical) {
                y = newPosition
            } else {
                x = newPosition
            }
            calculateMiddle()
            invalidate()
        }

        private fun calculateMiddle() {
            val pos: Float
            val viewSize: Float
            val pdfViewSize: Float
            if (pdfView!!.isSwipeVertical) {
                pos = y
                viewSize = height.toFloat()
                pdfViewSize = pdfView!!.height.toFloat()
            } else {
                pos = x
                viewSize = width.toFloat()
                pdfViewSize = pdfView!!.width.toFloat()
            }
            relativeHandlerMiddle = (pos + relativeHandlerMiddle) / pdfViewSize * viewSize
        }

        override fun hideDelayed() {
            _handler.postDelayed(hidePageScrollerRunnable, 1000)
        }

        override fun setPageNum(pageNum: Int) {
            val text = pageNum.toString()
            if (textView.text != text) {
                textView.text = text
            }
        }

        override fun shown(): Boolean {
            return visibility == VISIBLE
        }

        override fun show() {
            visibility = VISIBLE
        }

        override fun hide() {
            visibility = INVISIBLE
        }

        fun setTextColor(color: Int) {
            textView.setTextColor(color)
        }

        /**
         * @param size text size in dp
         */
        fun setTextSize(size: Int) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
        }

        private val isPDFViewReady: Boolean
            get() = pdfView != null && pdfView!!.pageCount > 0 && !pdfView!!.documentFitsView()

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isPDFViewReady) {
                return super.onTouchEvent(event)
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    pdfView!!.stopFling()
                    _handler.removeCallbacks(hidePageScrollerRunnable)
                    currentPos =
                        if (pdfView!!.isSwipeVertical) {
                            event.rawY - y
                        } else {
                            event.rawX - x
                        }
                    if (pdfView!!.isSwipeVertical) {
                        setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                        pdfView!!.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                    } else {
                        setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                        pdfView!!.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (pdfView!!.isSwipeVertical) {
                        setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                        pdfView!!.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                    } else {
                        setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                        pdfView!!.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    hideDelayed()
                    pdfView!!.performPageSnap()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        companion object {
            private const val HANDLE_LONG = 65
            private const val HANDLE_SHORT = 40
            private const val DEFAULT_TEXT_SIZE = 16
        }
    }
