package com.ismartcoding.lib.extensions

import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@BindingAdapter("glide")
fun glide(
    view: ImageView,
    url: String?,
) {
    if (!url.isNullOrEmpty()) {
        view.glide(url)
    }
}

fun View.delayOnLifecycle(
    durationInMillis: Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    block: () -> Unit,
): Job? =
    findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
        lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
            delay(durationInMillis)
            block()
        }
    }

fun View.setBackgroundAttr(
    @AttrRes attr: Int,
) {
    val outValue = TypedValue()
    context.theme.resolveAttribute(attr, outValue, true)
    background = ContextCompat.getDrawable(context, outValue.resourceId)
}

fun View.setSelectableItemBackground() {
    setBackgroundAttr(android.R.attr.selectableItemBackground)
}
