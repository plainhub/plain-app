package com.ismartcoding.plain.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.R
import java.util.*

class EqualizerView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        companion object {
            const val gapPercent = 0.4f
            val speeds = arrayOf(4f, 8f, 12f)
        }

        private var isStarted = false

        var colWidth: Int = 0
        var gap: Int = 0
        var totalGap: Int = 0

        var col1Rect = Rect()
        var col2Rect = Rect()
        var col3Rect = Rect()

        var col1CircleRect = RectF()
        var col2CircleRect = RectF()
        var col3CircleRect = RectF()

        var col1BottomRect = RectF()
        var col2BottomRect = RectF()
        var col3BottomRect = RectF()

        var y1: Int = -1
        var y2: Int = -1
        var y3: Int = -1

        var speed1: Float = speeds[1]
        var speed2: Float = speeds[0]
        var speed3: Float = speeds[2]

        val random = Random()
        private val animationHandler = Handler(Looper.getMainLooper())

        private val colPaint = Paint()

        var barColor: Int = ContextCompat.getColor(context, R.color.purple)
            private set

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            computeRects()

            colPaint.color = barColor

            canvas.drawRect(col1Rect, colPaint)
            canvas.drawRect(col2Rect, colPaint)
            canvas.drawRect(col3Rect, colPaint)

            canvas.drawOval(col1CircleRect, colPaint)
            canvas.drawOval(col2CircleRect, colPaint)
            canvas.drawOval(col3CircleRect, colPaint)

            canvas.drawOval(col1BottomRect, colPaint)
            canvas.drawOval(col2BottomRect, colPaint)
            canvas.drawOval(col3BottomRect, colPaint)
        }

        private fun computeRects()  {
            totalGap = ((gapPercent * width).toInt())
            gap = ((gapPercent * width) / 2f).toInt()
            colWidth = ((width - totalGap) / 3f).toInt()

            if (y1 == -1)
                {
                    y1 = height / 5
                    y2 = height / 5
                    y3 = height / 5
                }

            colPaint.color = ContextCompat.getColor(context, R.color.white)
            // col1
            col1Rect.apply {
                left = 0
                right = left + colWidth
                bottom = height - (colWidth / 2f).toInt()
                top = height - y1
            }

            col1CircleRect.apply {
                left = col1Rect.left.toFloat()
                right = col1Rect.right.toFloat()
                bottom = col1Rect.top + colWidth / 2f
                top = col1Rect.top - colWidth / 2f
            }

            col1BottomRect.apply {
                left = col1Rect.left.toFloat()
                right = col1Rect.right.toFloat()
                bottom = height.toFloat()
                top = (height - colWidth).toFloat()
            }

            // col2
            col2Rect.apply {
                left = col1Rect.right + gap
                right = left + colWidth
                bottom = height - (colWidth / 2f).toInt()
                top = height - y2
            }

            col2CircleRect.apply {
                left = col2Rect.left.toFloat()
                right = col2Rect.right.toFloat()
                bottom = col2Rect.top + colWidth / 2f
                top = col2Rect.top - colWidth / 2f
            }

            col2BottomRect.apply {
                left = col2Rect.left.toFloat()
                right = col2Rect.right.toFloat()
                bottom = height.toFloat()
                top = (height - colWidth).toFloat()
            }

            // col3
            col3Rect.apply {
                left = col2Rect.right + gap
                right = left + colWidth
                bottom = height - (colWidth / 2f).toInt()
                top = height - y3
            }

            col3CircleRect.apply {
                left = col3Rect.left.toFloat()
                right = col3Rect.right.toFloat()
                bottom = col3Rect.top + colWidth / 2f
                top = col3Rect.top - colWidth / 2f
            }

            col3BottomRect.apply {
                left = col3Rect.left.toFloat()
                right = col3Rect.right.toFloat()
                bottom = height.toFloat()
                top = (height - colWidth).toFloat()
            }
        }

        private fun getNextY(
            currentY: Float,
            speed: Float,
        ): Float {
            return currentY - speed
        }

        private val animationRunnable =
            Runnable {
                generateNewPositions()
                invalidate()
                start()
            }

        fun start()  {
            animationHandler.removeCallbacks(animationRunnable)
            isStarted = true
            animationHandler.postDelayed(animationRunnable, 33) // roughly 30fps
        }

        fun stop()  {
            isStarted = false
            animationHandler.removeCallbacks(animationRunnable)
            reset()
            invalidate()
        }

        private fun reset()  {
            y1 = -1
            y2 = -1
            y3 = -1
        }

        private fun generateNewPositions()  {
            y1 = getNextY(y1.toFloat(), speed1).toInt()
            y2 = getNextY(y2.toFloat(), speed2).toInt()
            y3 = getNextY(y3.toFloat(), speed3).toInt()
            if (y1 >= height - colWidth)
                {
                    y1 = height - colWidth
                    speed1 = speeds[random.nextInt(3)]
                } else if (y1 <= colWidth / 2f)
                {
                    y1 = (colWidth / 2f).toInt()
                    speed1 = -speeds[random.nextInt(3)]
                }

            if (y2 >= height - colWidth)
                {
                    y2 = height - colWidth
                    speed2 = speeds[random.nextInt(3)]
                } else if (y2 <= colWidth / 2f)
                {
                    y2 = (colWidth / 2f).toInt()
                    speed2 = -speeds[random.nextInt(3)]
                }

            if (y3 >= height - colWidth)
                {
                    y3 = height - colWidth
                    speed3 = speeds[random.nextInt(3)]
                } else if (y3 <= colWidth / 2f)
                {
                    y3 = (colWidth / 2f).toInt()
                    speed3 = -speeds[random.nextInt(3)]
                }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            start()
        }

        override fun onDetachedFromWindow() {
            animationHandler.removeCallbacks(animationRunnable)
            super.onDetachedFromWindow()
        }
    }
