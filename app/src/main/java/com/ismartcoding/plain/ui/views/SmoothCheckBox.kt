package com.ismartcoding.plain.ui.views

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.plain.R
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SmoothCheckBox(context: Context, attrs: AttributeSet? = null) : View(context, attrs), Checkable {
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mFloorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTickPoints = arrayOf(Point(), Point(), Point())
    private val mCenterPoint = Point()
    private val mTickPath: Path = Path()
    private var mLeftLineDistance = 0f
    private var mRightLineDistance = 0f
    private var mDrewDistance = 0f
    private var mScaleVal = 1.0f
    private var mFloorScale = 1.0f
    private var mWidth = 0
    private var mStrokeWidth = 1
    private var mCheckedColor = context.getColor(R.color.purple)
    private var mUnCheckedColor = context.getColor(android.R.color.white)
    private var mFloorColor = context.getColor(R.color.secondary)
    private var mFloorUnCheckedColor = context.getColor(R.color.secondary)
    private var mChecked = false
    private var mTickDrawing = false
    private var mOnCheckedChanged: ((checkBox: SmoothCheckBox, isChecked: Boolean) -> Unit)? = null

    init {
        mFloorUnCheckedColor = mFloorColor
        mTickPaint.style = Paint.Style.STROKE
        mTickPaint.strokeCap = Paint.Cap.ROUND
        mTickPaint.color = Color.WHITE
        mFloorPaint.style = Paint.Style.FILL
        mFloorPaint.color = mFloorColor
        mPaint.style = Paint.Style.FILL
        mPaint.color = mCheckedColor
        setOnClickListener {
            toggle()
            mTickDrawing = false
            mDrewDistance = 0f
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(KEY_INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putBoolean(KEY_INSTANCE_STATE, isChecked)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            val bundle = state
            val isChecked = bundle.getBoolean(KEY_INSTANCE_STATE)
            setChecked(isChecked)
            super.onRestoreInstanceState(bundle.parcelable(KEY_INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        this.isChecked = !isChecked
    }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
        reset()
        invalidate()
        mOnCheckedChanged?.invoke(this@SmoothCheckBox, mChecked)
    }

    private fun reset() {
        mTickDrawing = true
        mFloorScale = 1.0f
        mScaleVal = if (isChecked) 0f else 1.0f
        mFloorColor = if (isChecked) mCheckedColor else mFloorUnCheckedColor
        mDrewDistance = if (isChecked) mLeftLineDistance + mRightLineDistance else 0f
    }

    private fun measureSize(measureSpec: Int): Int {
        val defSize = context.dp2px(DEF_DRAW_SIZE)
        val specSize = MeasureSpec.getSize(measureSpec)
        val specMode = MeasureSpec.getMode(measureSpec)
        var result = 0
        when (specMode) {
            MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> result = defSize.coerceAtMost(specSize)
            MeasureSpec.EXACTLY -> result = specSize
        }
        return result
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measureSize(widthMeasureSpec), measureSize(heightMeasureSpec))
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        mWidth = measuredWidth
        mStrokeWidth = if (mStrokeWidth == 0) measuredWidth / 10 else mStrokeWidth
        mStrokeWidth = if (mStrokeWidth > measuredWidth / 5) measuredWidth / 5 else mStrokeWidth
        mStrokeWidth = if (mStrokeWidth < 3) 3 else mStrokeWidth
        mCenterPoint.x = mWidth / 2
        mCenterPoint.y = measuredHeight / 2
        mTickPoints[0].x = (measuredWidth.toFloat() / 30 * 7).roundToInt()
        mTickPoints[0].y = (measuredHeight.toFloat() / 30 * 14).roundToInt()
        mTickPoints[1].x = (measuredWidth.toFloat() / 30 * 13).roundToInt()
        mTickPoints[1].y = (measuredHeight.toFloat() / 30 * 20).roundToInt()
        mTickPoints[2].x = (measuredWidth.toFloat() / 30 * 22).roundToInt()
        mTickPoints[2].y = (measuredHeight.toFloat() / 30 * 10).roundToInt()
        mLeftLineDistance =
            sqrt(
                (mTickPoints[1].x - mTickPoints[0].x).toDouble().pow(2.0) +
                    (mTickPoints[1].y - mTickPoints[0].y).toDouble().pow(2.0),
            ).toFloat()
        mRightLineDistance =
            sqrt(
                (mTickPoints[2].x - mTickPoints[1].x).toDouble().pow(2.0) +
                    (mTickPoints[2].y - mTickPoints[1].y).toDouble().pow(2.0),
            ).toFloat()
        mTickPaint.strokeWidth = mStrokeWidth.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        drawBorder(canvas)
        drawCenter(canvas)
        drawTick(canvas)
    }

    private fun drawCenter(canvas: Canvas) {
        mPaint.color = mUnCheckedColor
        val radius = (mCenterPoint.x - mStrokeWidth) * mScaleVal
        canvas.drawCircle(mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat(), radius, mPaint)
    }

    private fun drawBorder(canvas: Canvas) {
        mFloorPaint.color = mFloorColor
        val radius = mCenterPoint.x
        canvas.drawCircle(mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat(), radius * mFloorScale, mFloorPaint)
    }

    private fun drawTick(canvas: Canvas) {
        if (mTickDrawing && isChecked) {
            drawTickPath(canvas)
        }
    }

    private fun drawTickPath(canvas: Canvas) {
        mTickPath.reset()
        // draw left of the tick
        if (mDrewDistance < mLeftLineDistance) {
            val step: Float = if (mWidth / 20.0f < 3) 3f else mWidth / 20.0f
            mDrewDistance += step
            val stopX = mTickPoints[0].x + (mTickPoints[1].x - mTickPoints[0].x) * mDrewDistance / mLeftLineDistance
            val stopY = mTickPoints[0].y + (mTickPoints[1].y - mTickPoints[0].y) * mDrewDistance / mLeftLineDistance
            mTickPath.moveTo(mTickPoints[0].x.toFloat(), mTickPoints[0].y.toFloat())
            mTickPath.lineTo(stopX, stopY)
            canvas.drawPath(mTickPath, mTickPaint)
            if (mDrewDistance > mLeftLineDistance) {
                mDrewDistance = mLeftLineDistance
            }
        } else {
            mTickPath.moveTo(mTickPoints[0].x.toFloat(), mTickPoints[0].y.toFloat())
            mTickPath.lineTo(mTickPoints[1].x.toFloat(), mTickPoints[1].y.toFloat())
            canvas.drawPath(mTickPath, mTickPaint)

            // draw right of the tick
            if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
                val stopX = mTickPoints[1].x + (mTickPoints[2].x - mTickPoints[1].x) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                val stopY = mTickPoints[1].y - (mTickPoints[1].y - mTickPoints[2].y) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                mTickPath.reset()
                mTickPath.moveTo(mTickPoints[1].x.toFloat(), mTickPoints[1].y.toFloat())
                mTickPath.lineTo(stopX, stopY)
                canvas.drawPath(mTickPath, mTickPaint)
                val step: Float = if (mWidth / 20 < 3) 3f else (mWidth / 20).toFloat()
                mDrewDistance += step
            } else {
                mTickPath.reset()
                mTickPath.moveTo(mTickPoints[1].x.toFloat(), mTickPoints[1].y.toFloat())
                mTickPath.lineTo(mTickPoints[2].x.toFloat(), mTickPoints[2].y.toFloat())
                canvas.drawPath(mTickPath, mTickPaint)
            }
        }

        // invalidate
        if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
            postDelayed({ postInvalidate() }, 10)
        }
    }

    fun setOnCheckedChanged(callback: ((checkBox: SmoothCheckBox, isChecked: Boolean) -> Unit)?) {
        mOnCheckedChanged = callback
    }

    companion object {
        private const val KEY_INSTANCE_STATE = "InstanceState"
        private const val DEF_DRAW_SIZE = 25
    }
}
