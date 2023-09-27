package com.ismartcoding.lib.fastscroll

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.ismartcoding.lib.R

class FastScrollRecyclerView(context: Context, attrs: AttributeSet? = null) : RecyclerView(context, attrs) {
    val fastScroller = createFastScroller(context, attrs)

    override fun setAdapter(adapter: Adapter<*>?) =
        super.setAdapter(adapter).also {
            when (adapter) {
                is FastScroller.SectionIndexer -> fastScroller.setSectionIndexer(adapter)
                null -> fastScroller.setSectionIndexer(null)
            }
        }

    override fun setVisibility(visibility: Int) =
        super.setVisibility(visibility).also {
            fastScroller.visibility = visibility
        }

    override fun onAttachedToWindow() =
        super.onAttachedToWindow().also {
            fastScroller.attachRecyclerView(this)
        }

    override fun onDetachedFromWindow() {
        fastScroller.detachRecyclerView()
        super.onDetachedFromWindow()
    }

    private fun createFastScroller(
        context: Context,
        attrs: AttributeSet? = null,
    ): FastScroller {
        return FastScroller(context, attrs).apply {
            id = R.id.fast_scroller
        }
    }
}
