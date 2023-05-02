package com.ismartcoding.plain.services

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.LifecycleService
import com.ismartcoding.plain.mediaProjectionManager
import java.io.IOException

class ScreenMirroringService : LifecycleService() {
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null

    private var mMediaProjection: MediaProjection? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            mResultCode = intent.getIntExtra("code", -1)
            mResultData = intent.getParcelableExtra("data")
            mScreenWidth = intent.getIntExtra("width", 720)
            mScreenHeight = intent.getIntExtra("height", 1280)
            mScreenDensity = intent.getIntExtra("density", 1)
        }
        mMediaProjection = createMediaProjection()
        mMediaRecorder = createMediaRecorder()
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder?.start()
        return START_NOT_STICKY
    }

    private fun createMediaProjection(): MediaProjection {
        return mediaProjectionManager.getMediaProjection(mResultCode, mResultData!!)
    }

    private fun createMediaRecorder(): MediaRecorder {
        var videoQuality = "HD"
        val mediaRecorder = MediaRecorder()
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setVideoSize(mScreenWidth, mScreenHeight) //after setVideoSource(), setOutFormat()
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264) //after setOutputFormat()
        mediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight)
        mediaRecorder.setVideoFrameRate(60)
        try {
            mediaRecorder.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaRecorder
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mMediaProjection?.createVirtualDisplay(
            "ScreenMirroringService", mScreenWidth, mScreenHeight, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder!!.surface, null, null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            mVirtualDisplay = null
        }
        if (mMediaRecorder != null) {
            mMediaRecorder?.setOnErrorListener(null)
            mMediaProjection?.stop()
            mMediaRecorder?.reset()
        }
        if (mMediaProjection != null) {
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }
}