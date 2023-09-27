package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import kotlinx.coroutines.Job

abstract class CustomViewBase(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), LifecycleObserver {
    protected val events = mutableListOf<Job>()

    protected fun registerLifecycleOwner(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        events.forEach {
            it.cancel()
        }
    }
}
