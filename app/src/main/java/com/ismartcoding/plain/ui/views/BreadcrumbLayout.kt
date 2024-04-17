package com.ismartcoding.plain.ui.views

import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.databinding.ItemBreadcrumbBinding
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper

data class BreadcrumbItem(var name: String, var path: String)

class BreadcrumbLayout : HorizontalScrollView {
    private val mTabLayoutHeight = context.dp2px(48)
    private val mItemsLayout: LinearLayout
    private val mItemColor =
        ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()),
            intArrayOf(
                ContextCompat.getColor(context, R.color.primary),
                ContextCompat.getColor(context, R.color.secondary),
            ),
        )
    var navigateTo: ((BreadcrumbItem) -> Unit)? = null
    private val mPaths = mutableListOf<BreadcrumbItem>()
    private var mSelectedIndex: Int = 0

    private var mIsLayoutDirty = true
    private var mIsScrollToSelectedItemPending = false
    private var mIsFirstScroll = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs,
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
    ) : super(
        context,
        attrs,
        defStyleAttr,
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        isHorizontalScrollBarEnabled = false
        mItemsLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        mItemsLayout.setPaddingRelative(paddingStart, paddingTop, paddingEnd, paddingBottom)
        setPaddingRelative(0, 0, 0, 0)
        addView(mItemsLayout, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    override fun jumpDrawablesToCurrentState() {
        if (isInLayout) {
            return
        }
        super.jumpDrawablesToCurrentState()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var newHeightMeasureSpec = heightMeasureSpec
        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
            var height = mTabLayoutHeight
            if (heightMode == MeasureSpec.AT_MOST) {
                height = height.coerceAtMost(MeasureSpec.getSize(newHeightMeasureSpec))
            }
            newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

    override fun requestLayout() {
        mIsLayoutDirty = true
        super.requestLayout()
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)

        mIsLayoutDirty = false
        if (mIsScrollToSelectedItemPending) {
            scrollToSelectedItem()
            mIsScrollToSelectedItemPending = false
        }
    }

    fun setData(
        paths: List<BreadcrumbItem>,
        selectedIndex: Int,
    ) {
        mPaths.clear()
        mPaths.addAll(paths)
        mSelectedIndex = selectedIndex
        inflateItemViews()
        scrollToSelectedItem()
    }

    private fun scrollToSelectedItem() {
        if (mIsLayoutDirty) {
            mIsScrollToSelectedItemPending = true
            return
        }
        val selectedItemView = mItemsLayout.getChildAt(mSelectedIndex)
        val scrollX =
            if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                selectedItemView.left - mItemsLayout.paddingStart
            } else {
                selectedItemView.right - width + mItemsLayout.paddingStart
            }
        if (!mIsFirstScroll && isShown) {
            smoothScrollTo(scrollX, 0)
        } else {
            scrollTo(scrollX, 0)
        }
        mIsFirstScroll = false
    }

    private fun inflateItemViews() {
        mItemsLayout.removeAllViews()
        mPaths.forEachIndexed { index, data ->
            val binding = ItemBreadcrumbBinding.inflate(LayoutInflater.from(context), mItemsLayout, false)
            binding.root.setOnLongClickListener {
                val menu =
                    PopupMenu(context, binding.root)
                        .apply { inflate(R.menu.files_breadcrumb) }
                menu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.copy_path -> {
                            val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.file_path), data.path)
                            clipboardManager.setPrimaryClip(clip)
                            DialogHelper.showTextCopiedMessage(data.path)
                            true
                        }

                        else -> false
                    }
                }
                menu.show()
                true
            }
            binding.text.setTextColor(mItemColor)
            binding.arrowImage.imageTintList = mItemColor
            binding.text.text = data.name
            binding.arrowImage.isVisible = index != mPaths.size - 1
            binding.root.isActivated = index == mSelectedIndex
            binding.root.setOnClickListener {
                if (mSelectedIndex == index) {
                    scrollToSelectedItem()
                } else {
                    navigateTo?.invoke(data)
                }
            }
            mItemsLayout.addView(binding.root)
        }
    }
}
