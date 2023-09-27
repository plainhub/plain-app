package com.ismartcoding.plain.ui.views.videoplayer.render

import android.view.View

class MeasureHelper {
    private var videoWidth = 0
    private var videoHeight = 0

    private var measureWidth = 0
    private var measureHeight = 0

    private var videoRotationDegree = 0

    fun setVideoSize(
        videoWidth: Int,
        videoHeight: Int,
    ) {
        this.videoWidth = videoWidth
        this.videoHeight = videoHeight
    }

    fun setVideoRotationDegree(videoRotationDegree: Int) {
        this.videoRotationDegree = videoRotationDegree
    }

    fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        aspectRatio: Int,
    ) {
        val wms: Int
        val hms: Int
        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
            wms = heightMeasureSpec
            hms = widthMeasureSpec
        } else {
            wms = widthMeasureSpec
            hms = heightMeasureSpec
        }
        // 测量需要显示的视频宽高
        var width = View.getDefaultSize(videoWidth, wms)
        var height = View.getDefaultSize(videoHeight, hms)
        if (videoWidth > 0 && videoHeight > 0) { // 当前视频尺寸已知，且有效
            val widthSpecMode = View.MeasureSpec.getMode(wms)
            val widthSpecSize = View.MeasureSpec.getSize(wms)
            val heightSpecMode = View.MeasureSpec.getMode(hms)
            val heightSpecSize = View.MeasureSpec.getSize(hms)
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                val specAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
                val displayAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
                val shouldBeWider = displayAspectRatio > specAspectRatio

                if (aspectRatio == IRenderView.AR_ASPECT_FIT_PARENT) {
                    if (shouldBeWider) {
                        // too wide, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    }
                } else {
                    if (shouldBeWider) {
                        // not high enough, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    } else {
                        // not wide enough, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    }
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) { // the size is fixed
                width = widthSpecSize
                height = heightSpecSize
                // for compatibility, we adjust size based on aspect ratio
                if (videoWidth * height < width * videoHeight) { // Log.i("@@@", "image too wide, correcting");
                    width = height * videoWidth / videoHeight
                } else if (videoWidth * height > width * videoHeight) { // Log.i("@@@", "image too tall, correcting");
                    height = width * videoHeight / videoWidth
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) { // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize
                height = width * videoHeight / videoWidth
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) { // couldn't match aspect ratio within the constraints
                    height = heightSpecSize
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) { // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize
                width = height * videoWidth / videoHeight
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) { // couldn't match aspect ratio within the constraints
                    width = widthSpecSize
                }
            } else { // neither the width nor the height are fixed, try to use actual video size
                width = videoWidth
                height = videoHeight
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) { // too tall, decrease both width and height
                    height = heightSpecSize
                    width = height * videoWidth / videoHeight
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) { // too wide, decrease both width and height
                    width = widthSpecSize
                    height = width * videoHeight / videoWidth
                }
            }
        } else { // no size yet, just adopt the given spec sizes
        }
        measureWidth = width
        measureHeight = height
    }

    fun getMeasureWidth(): Int {
        return measureWidth
    }

    fun getMeasureHeight(): Int {
        return measureHeight
    }
}
