package com.ismartcoding.lib.extensions

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable.setCompatTint(
    @ColorInt color: Int,
) = DrawableCompat.setTint(this, color)

fun Drawable.wrap(): Drawable = DrawableCompat.wrap(this)
