package com.ismartcoding.lib.brv.listener

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

class SnapLinearSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_START
    }
}
