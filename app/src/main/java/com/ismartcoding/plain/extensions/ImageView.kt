package com.ismartcoding.plain.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.ismartcoding.lib.extensions.isPartialSupportVideo
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import java.io.File

fun ImageView.glide(url: String) {
    val view = this
    if (url.isPartialSupportVideo()) {
        coMain {
            val t = withIO { File(url).getBitmapAsync(view.context, 200, 200) }
            Glide.with(view).load(t).into(view)
        }
    } else {
        Glide.with(view).load(url).into(view)
    }
}
