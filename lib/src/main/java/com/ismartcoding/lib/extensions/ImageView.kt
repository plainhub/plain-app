package com.ismartcoding.lib.extensions

import android.widget.ImageView
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import coil.load
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import kotlinx.coroutines.launch
import java.io.File

fun ImageView.glide(url: String) {
    if (url.isPartialSupportVideo()) {
        val view = this
        view.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
            val t = withIO { File(url).getBitmapAsync(view.context, 400, 400) }
            load(t)
        }
    } else {
        load(url)
    }
}