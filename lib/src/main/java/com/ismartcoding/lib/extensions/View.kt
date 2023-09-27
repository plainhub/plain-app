package com.ismartcoding.lib.extensions

import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.*

val View.compatPaddingStart get() = ViewCompat.getPaddingStart(this)

@BindingAdapter("glide")
fun glide(
    view: ImageView,
    url: String?,
) {
    if (!url.isNullOrEmpty()) {
        view.glide(url)
    }
}

fun View.slideUp(
    show: Boolean,
    duration: Long = 200,
) {
    val animate = TranslateAnimation(0f, 0f, this.height.toFloat(), 0f)
    animate.duration = duration
    animate.setAnimationListener(
        object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                isVisible = show
            }

            override fun onAnimationRepeat(animation: Animation) {}
        },
    )
    startAnimation(animate)
}

fun View.slideDown(
    show: Boolean,
    duration: Long = 200,
) {
    val animate = TranslateAnimation(0f, 0f, 0f, this.height.toFloat())
    animate.duration = duration
    animate.setAnimationListener(
        object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                isVisible = show
            }

            override fun onAnimationRepeat(animation: Animation) {}
        },
    )
    startAnimation(animate)
}

fun View.toggle() {
    isVisible = !isVisible
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
