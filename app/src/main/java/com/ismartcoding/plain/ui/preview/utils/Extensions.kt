package com.ismartcoding.plain.ui.preview.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.preview.PreviewItem

internal fun ViewGroup.findViewWithKeyTag(
    key: Int,
    tag: Any,
): View? {
    forEach {
        if (it.getTag(key) == tag) return it
        if (it is ViewGroup) {
            val result = it.findViewWithKeyTag(key, tag)
            if (result != null) return result
        }
    }
    return null
}

internal val View.activity: Activity?
    get() = getActivity(context)

// https://stackoverflow.com/questions/9273218/is-it-always-safe-to-cast-context-to-activity-within-view/45364110
private fun getActivity(context: Context?): Activity? {
    if (context == null) return null
    if (context is Activity) return context
    if (context is ContextWrapper) return getActivity(context.baseContext)
    return null
}

internal fun View.initTag(
    item: PreviewItem,
    holder: RecyclerView.ViewHolder,
) {
    setTag(R.id.viewer_adapter_item_key, item.id)
    setTag(R.id.viewer_adapter_item_data, item)
    setTag(R.id.viewer_adapter_item_holder, holder)
}
