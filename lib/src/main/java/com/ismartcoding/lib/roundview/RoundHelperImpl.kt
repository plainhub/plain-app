package com.ismartcoding.lib.roundview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.ismartcoding.lib.R
import com.ismartcoding.lib.extensions.dp2px

class RoundHelperImpl : RoundHelper {
    private var mContext: Context? = null
    private var mView: View? = null
    private var mPaint: Paint? = null
    private var mRectF: RectF? = null
    private var mStrokeRectF: RectF? = null
    private var mOriginRectF: RectF? = null
    private var mPath: Path? = null
    private var mTempPath: Path? = null
    private var mXfermode: Xfermode? = null
    private var isCircle = false
    private lateinit var mRadii: FloatArray
    private lateinit var mStrokeRadii: FloatArray
    private var mWidth = 0
    private var mHeight = 0
    private var mStrokeColor = 0
    private var mStrokeWidth = 0f
    private var mStrokeColorStateList: ColorStateList? = null
    private var mRadiusTopLeft = 0f
    private var mRadiusTopRight = 0f
    private var mRadiusBottomLeft = 0f
    private var mRadiusBottomRight = 0f
    private var isNewLayer = false

    override fun init(
        context: Context,
        attrs: AttributeSet?,
        view: View,
    ) {
        if (view is ViewGroup && view.getBackground() == null) {
            view.setBackgroundColor(Color.parseColor("#00000000"))
        }
        mContext = context
        mView = view
        mRadii = FloatArray(8)
        mStrokeRadii = FloatArray(8)
        mPaint = Paint()
        mRectF = RectF()
        mStrokeRectF = RectF()
        mOriginRectF = RectF()
        mPath = Path()
        mTempPath = Path()
        mXfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        mStrokeColor = Color.WHITE
        val array = context.obtainStyledAttributes(attrs, R.styleable.RoundCorner)
        val radius = array.getDimension(R.styleable.RoundCorner_rRadius, 0f)
        val radiusLeft = array.getDimension(R.styleable.RoundCorner_rLeftRadius, radius)
        val radiusRight = array.getDimension(R.styleable.RoundCorner_rRightRadius, radius)
        val radiusTop = array.getDimension(R.styleable.RoundCorner_rTopRadius, radius)
        val radiusBottom = array.getDimension(R.styleable.RoundCorner_rBottomRadius, radius)
        mRadiusTopLeft = array.getDimension(R.styleable.RoundCorner_rTopLeftRadius, if (radiusTop > 0) radiusTop else radiusLeft)
        mRadiusTopRight = array.getDimension(R.styleable.RoundCorner_rTopRightRadius, if (radiusTop > 0) radiusTop else radiusRight)
        mRadiusBottomLeft = array.getDimension(R.styleable.RoundCorner_rBottomLeftRadius, if (radiusBottom > 0) radiusBottom else radiusLeft)
        mRadiusBottomRight = array.getDimension(R.styleable.RoundCorner_rBottomRightRadius, if (radiusBottom > 0) radiusBottom else radiusRight)
        mStrokeWidth = array.getDimension(R.styleable.RoundCorner_rStrokeWidth, 0f)
        mStrokeColor = array.getColor(R.styleable.RoundCorner_rStrokeColor, mStrokeColor)
        mStrokeColorStateList = array.getColorStateList(R.styleable.RoundCorner_rStrokeColor)
        isNewLayer = array.getBoolean(R.styleable.RoundCorner_rNewLayer, false)
        if (isNewLayer && view is ViewGroup) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        array.recycle()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
    ) {
        mWidth = width
        mHeight = height
        if (isCircle) {
            val radius = Math.min(height, width) * 1f / 2
            mRadiusTopLeft = radius
            mRadiusTopRight = radius
            mRadiusBottomRight = radius
            mRadiusBottomLeft = radius
        }
        setRadius()
        if (mRectF != null) {
            mRectF!![mStrokeWidth, mStrokeWidth, width - mStrokeWidth] = height - mStrokeWidth
        }
        if (mStrokeRectF != null) {
            mStrokeRectF!![mStrokeWidth / 2, mStrokeWidth / 2, width - mStrokeWidth / 2] = height - mStrokeWidth / 2
        }
        if (mOriginRectF != null) {
            mOriginRectF!![0f, 0f, width.toFloat()] = height.toFloat()
        }
    }

    override fun preDraw(canvas: Canvas) {
        canvas.saveLayer(
            if (isNewLayer && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) mOriginRectF else mRectF,
            null,
            Canvas.ALL_SAVE_FLAG,
        )
    }

    override fun drawPath(
        canvas: Canvas,
        drawableState: IntArray?,
    ) {
        mPaint!!.reset()
        mPath!!.reset()
        mPaint!!.isAntiAlias = true
        mPaint!!.style = Paint.Style.FILL
        mPaint!!.xfermode = mXfermode
        mPath!!.addRoundRect(mRectF!!, mRadii, Path.Direction.CCW)
        mTempPath!!.reset()
        mTempPath!!.addRect(mOriginRectF!!, Path.Direction.CCW)
        mTempPath!!.op(mPath!!, Path.Op.DIFFERENCE)
        canvas.drawPath(mTempPath!!, mPaint!!)
        mPaint!!.xfermode = null
        canvas.restore()

        // draw stroke
        if (mStrokeWidth > 0) {
            if (mStrokeColorStateList != null && mStrokeColorStateList!!.isStateful) {
                mStrokeColor = mStrokeColorStateList!!.getColorForState(drawableState, mStrokeColorStateList!!.defaultColor)
            }
            mPaint!!.style = Paint.Style.STROKE
            mPaint!!.strokeWidth = mStrokeWidth
            mPaint!!.color = mStrokeColor
            mPath!!.reset()
            mPath!!.addRoundRect(mStrokeRectF!!, mStrokeRadii, Path.Direction.CCW)
            canvas.drawPath(mPath!!, mPaint!!)
        }
    }

    override fun setCircle(isCircle: Boolean) {
        this.isCircle = isCircle
    }

    override fun setRadius(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        val radiusPx = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        mRadiusTopLeft = radiusPx
        mRadiusTopRight = radiusPx
        mRadiusBottomLeft = radiusPx
        mRadiusBottomRight = radiusPx
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadius(
        radiusTopLeftDp: Float,
        radiusTopRightDp: Float,
        radiusBottomLeftDp: Float,
        radiusBottomRightDp: Float,
    ) {
        if (mContext == null) {
            return
        }
        mRadiusTopLeft = mContext!!.dp2px(radiusTopLeftDp.toInt()).toFloat()
        mRadiusTopRight = mContext!!.dp2px(radiusTopRightDp.toInt()).toFloat()
        mRadiusBottomLeft = mContext!!.dp2px(radiusBottomLeftDp.toInt()).toFloat()
        mRadiusBottomRight = mContext!!.dp2px(radiusBottomRightDp.toInt()).toFloat()
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusLeft(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        val radiusPx = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        mRadiusTopLeft = radiusPx
        mRadiusBottomLeft = radiusPx
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusRight(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        val radiusPx = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        mRadiusTopRight = radiusPx
        mRadiusBottomRight = radiusPx
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusTop(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        val radiusPx = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        mRadiusTopLeft = radiusPx
        mRadiusTopRight = radiusPx
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusBottom(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        val radiusPx = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        mRadiusBottomLeft = radiusPx
        mRadiusBottomRight = radiusPx
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusTopLeft(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        mRadiusTopLeft = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusTopRight(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        mRadiusTopRight = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusBottomLeft(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        mRadiusBottomLeft = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setRadiusBottomRight(radiusDp: Float) {
        if (mContext == null) {
            return
        }
        mRadiusBottomRight = mContext!!.dp2px(radiusDp.toInt()).toFloat()
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setStrokeWidth(widthDp: Float) {
        if (mContext == null) {
            return
        }
        mStrokeWidth = mContext!!.dp2px(widthDp.toInt()).toFloat()
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setStrokeColor(color: Int) {
        mStrokeColor = color
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    override fun setStrokeWidthColor(
        widthDp: Float,
        color: Int,
    ) {
        if (mContext == null) {
            return
        }
        mStrokeWidth = mContext!!.dp2px(widthDp.toInt()).toFloat()
        mStrokeColor = color
        if (mView != null) {
            onSizeChanged(mWidth, mHeight)
            mView!!.invalidate()
        }
    }

    private fun setRadius() {
        mRadii[1] = mRadiusTopLeft - mStrokeWidth
        mRadii[0] = mRadii[1]
        mRadii[3] = mRadiusTopRight - mStrokeWidth
        mRadii[2] = mRadii[3]
        mRadii[5] = mRadiusBottomRight - mStrokeWidth
        mRadii[4] = mRadii[5]
        mRadii[7] = mRadiusBottomLeft - mStrokeWidth
        mRadii[6] = mRadii[7]
        mStrokeRadii[1] = mRadiusTopLeft - mStrokeWidth / 2
        mStrokeRadii[0] = mStrokeRadii[1]
        mStrokeRadii[3] = mRadiusTopRight - mStrokeWidth / 2
        mStrokeRadii[2] = mStrokeRadii[3]
        mStrokeRadii[5] = mRadiusBottomRight - mStrokeWidth / 2
        mStrokeRadii[4] = mStrokeRadii[5]
        mStrokeRadii[7] = mRadiusBottomLeft - mStrokeWidth / 2
        mStrokeRadii[6] = mStrokeRadii[7]
    }
}
