package com.ismartcoding.lib.extensions

import android.widget.ImageView
import coil.load
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision

fun ImageView.glide(url: String) {
    val builder: ImageRequest.Builder.() -> Unit = {
        videoFrameMillis(3000)
        placeholder(android.R.color.transparent)
        error(android.R.color.transparent)
        crossfade(true)
        precision(Precision.AUTOMATIC)
        size(400, 400)
    }
    load(url, builder = builder)
}