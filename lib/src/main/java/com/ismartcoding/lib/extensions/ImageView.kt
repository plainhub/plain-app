package com.ismartcoding.lib.extensions

import android.widget.ImageView
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.bumptech.glide.Glide
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import kotlinx.coroutines.launch
import java.io.File

fun ImageView.glide(url: String) {
    val view = this
    if (url.isPartialSupportVideo()) {
        view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
            val t = withIO { File(url).getBitmapAsync(view.context, 400, 400) }
            Glide.with(view).load(t).into(view)
        }
    } else {
        Glide.with(view).load(url).into(view)
    }
}