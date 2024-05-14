package com.ismartcoding.plain.ui.base.videoplayer

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.util.Rational
import androidx.media3.ui.PlayerView
import com.ismartcoding.lib.isTPlus

internal fun enterPIPMode(context: Context, defaultPlayerView: PlayerView) {
    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    ) {
        defaultPlayerView.useController = false
        val params = PictureInPictureParams.Builder()
        if (isTPlus()) {
            params
                .setTitle("Video Player")
                .setAspectRatio(Rational(16, 9))
                .setSeamlessResizeEnabled(true)
        }

        context.findActivity().enterPictureInPictureMode(params.build())
    }
}

internal fun Context.isActivityStatePipMode(): Boolean {
    return findActivity().isInPictureInPictureMode
}
