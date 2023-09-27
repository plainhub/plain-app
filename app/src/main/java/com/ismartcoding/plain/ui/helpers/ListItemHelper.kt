package com.ismartcoding.plain.ui.helpers

import android.view.View
import android.widget.TextView
import com.ismartcoding.lib.extensions.delayOnLifecycle
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.extensions.setSelectableTextClickable

object ListItemHelper {
    private fun click(view: View) {
        val container = view.findViewById<View>(R.id.container)
        container.performClick()
        container.isPressed = true
        container.delayOnLifecycle(100) {
            container.isPressed = false
        }
    }

    fun initView(view: View) {
        setOf(R.id.text_key, R.id.text_value, R.id.subtitle).forEach {
            view.findViewById<TextView>(it).setSelectableTextClickable {
                click(view)
            }
        }
    }
}
