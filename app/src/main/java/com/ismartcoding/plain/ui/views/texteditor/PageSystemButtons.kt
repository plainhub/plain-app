package com.ismartcoding.plain.ui.views.texteditor

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ismartcoding.plain.R

class PageSystemButtons(context: Context, val pageButtonsInterface: PageButtonsInterface, val prev: FloatingActionButton, val next: FloatingActionButton) {
    val handler = Handler(Looper.getMainLooper())
    val runnable =
        Runnable {
            next.isVisible = false
            prev.isVisible = false
        }

    init {
        next.setBackgroundColor(context.getColor(R.color.canvas))
        prev.setBackgroundColor(context.getColor(R.color.canvas))
        next.isVisible = pageButtonsInterface.canReadNextPage()
        prev.isVisible = pageButtonsInterface.canReadPrevPage()
        next.setOnClickListener { pageButtonsInterface.nextPageClicked() }
        next.setOnLongClickListener {
            pageButtonsInterface.pageSystemButtonLongClicked()
            true
        }
        prev.setOnClickListener { pageButtonsInterface.prevPageClicked() }
        prev.setOnLongClickListener {
            pageButtonsInterface.pageSystemButtonLongClicked()
            true
        }
    }

    fun updateVisibility(autoHide: Boolean) {
        next.isVisible = pageButtonsInterface.canReadNextPage()
        prev.isVisible = pageButtonsInterface.canReadPrevPage()
        if (autoHide) {
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, TIME_TO_SHOW_FABS.toLong())
        } else {
            handler.removeCallbacks(runnable)
        }
    }

    interface PageButtonsInterface {
        fun nextPageClicked()

        fun prevPageClicked()

        fun pageSystemButtonLongClicked()

        fun canReadNextPage(): Boolean

        fun canReadPrevPage(): Boolean
    }

    companion object {
        private const val TIME_TO_SHOW_FABS = 2000
    }
}
