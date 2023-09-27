package com.ismartcoding.plain.ui.preview

import android.widget.ImageView
import java.lang.ref.WeakReference

object TransitionHelper {
    private val transition = HashMap<String, WeakReference<ImageView>>()

    fun put(
        key: String,
        imageView: ImageView,
    ) {
        transition[key] = WeakReference(imageView)
    }

    fun provide(key: String): ImageView? {
        return transition[key]?.get()
    }
}
