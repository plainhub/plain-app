package com.ismartcoding.plain.ui.extensions

import android.graphics.drawable.AnimationDrawable
import android.widget.ImageView
import com.ismartcoding.plain.R

fun ImageView.voiceDefault()  {
    (background as? AnimationDrawable)?.stop()
    setBackgroundResource(R.drawable.ic_voice0)
}

fun ImageView.voicePlay()  {
    setBackgroundResource(R.drawable.animation_voice)
    (background as AnimationDrawable).start()
}
