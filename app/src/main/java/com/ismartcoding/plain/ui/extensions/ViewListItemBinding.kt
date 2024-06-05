package com.ismartcoding.plain.ui.extensions

import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.google.android.material.button.MaterialButton
import com.ismartcoding.lib.extensions.delayOnLifecycle
import com.ismartcoding.lib.extensions.px
import com.ismartcoding.lib.extensions.setSelectableItemBackground
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

fun ViewListItemBinding.setKeyText(text: String): ViewListItemBinding {
    textKey.text = text
    textKey.setSelectableTextClickable {
        performClickRow()
    }
    return this
}

fun ViewListItemBinding.initTheme(): ViewListItemBinding {
    val context = container.context
    val color = context.getColor(R.color.primary)
    container.setSelectableItemBackground()
    return setKeyTextColor(color)
        .setValueTextColor(color)
}

fun ViewListItemBinding.setKeyTextColor(
    @ColorInt color: Int,
): ViewListItemBinding {
    this.textKey.setTextColor(color)
    return this
}

fun ViewListItemBinding.setKeyText(textId: Int): ViewListItemBinding {
    return this.setKeyText(getString(textId))
}

fun ViewListItemBinding.performClickRow() {
    container.performClick()
    container.isPressed = true
    container.delayOnLifecycle(100) {
        container.isPressed = false
    }
}

fun ViewListItemBinding.setValueTextColor(
    @ColorInt color: Int,
): ViewListItemBinding {
    this.textValue.setTextColor(color)
    return this
}

fun ViewListItemBinding.setValueText(text: String): ViewListItemBinding {
    textValue.text = text
    textValue.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
    textValue.setSelectableTextClickable {
        performClickRow()
    }
    return this
}

fun ViewListItemBinding.setStartIcon(
    @DrawableRes iconId: Int,
): ViewListItemBinding {
    startIcon.visibility = View.VISIBLE
    startIcon.setImageResource(iconId)
    return this
}

fun ViewListItemBinding.clearTextRows(): ViewListItemBinding {
    rows.removeAllViews()
    return this
}

fun ViewListItemBinding.addTextRow(text: String): ViewListItemBinding {
    val context = rows.context
    val textView = TextView(context, null)
    textView.text = text
    textView.setSelectableTextClickable {
        performClickRow()
    }
    val layoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    layoutParams.topMargin = context.px(R.dimen.size_mini)
    textView.layoutParams = layoutParams
    textView.setTextIsSelectable(true)
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.px(R.dimen.text_size_lg).toFloat())
    rows.addView(textView)
    return this
}

fun ViewListItemBinding.enableSwipeMenu(enable: Boolean): ViewListItemBinding {
    swipeMenu.isSwipeEnable = enable
    return this
}

fun ViewListItemBinding.setButton(block: MaterialButton.() -> Unit) {
    block(button)
}

fun ViewListItemBinding.setLeftSwipeButton(
    buttonText: String,
    buttonCallback: () -> Unit,
): ViewListItemBinding {
    leftSwipeButton.let {
        it.visibility = View.VISIBLE
        it.text = buttonText
        it.setSafeClick {
            buttonCallback()
        }
    }
    return this
}

fun ViewListItemBinding.setRightSwipeButton(
    buttonText: String,
    buttonCallback: () -> Unit,
): ViewListItemBinding {
    rightSwipeButton.let {
        it.visibility = View.VISIBLE
        it.text = buttonText
        it.setSafeClick {
            buttonCallback()
        }
    }
    return this
}

fun ViewListItemBinding.setClick(callback: (() -> Unit)?): ViewListItemBinding {
    container.setSafeClick {
        callback?.invoke()
    }
    return this
}

fun ViewListItemBinding.setSwitch(
    enable: Boolean,
    onChanged: ((CompoundButton, Boolean) -> Unit)? = null,
): ViewListItemBinding {
    endSwitch.visibility = View.VISIBLE
    endSwitch.setOnCheckedChangeListener(null)
    endSwitch.isChecked = enable
    val listener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> onChanged?.invoke(buttonView, isChecked) }
    endSwitch.setOnCheckedChangeListener(listener)
    endSwitch.tag = listener
    return this
}

fun ViewListItemBinding.hideSwitch() {
    endSwitch.visibility = View.GONE
}

fun ViewListItemBinding.setSwitchEnable(enable: Boolean): ViewListItemBinding {
    endSwitch.setOnCheckedChangeListener(null)
    endSwitch.isChecked = enable
    endSwitch.setOnCheckedChangeListener(endSwitch.tag as? CompoundButton.OnCheckedChangeListener)
    return this
}

fun ViewListItemBinding.hideEndIcon() {
    endIcon.visibility = View.GONE
}

fun ViewListItemBinding.showSelected() {
    setEndIcon(R.drawable.ic_done)
}

fun ViewListItemBinding.setEndIcon(
    @DrawableRes iconId: Int,
    clickCallback: (() -> Unit)? = null,
): ViewListItemBinding {
    endIcon.visibility = View.VISIBLE
    endIcon.setImageResource(iconId)
    if (clickCallback != null) {
        endIcon.setSafeClick {
            clickCallback.invoke()
        }
    }
    return this
}

fun ViewListItemBinding.showMore(): ViewListItemBinding {
    setEndIcon(R.drawable.ic_chevron_right)
    val p8 = container.context.px(R.dimen.size_sm)
    val p16 = container.context.px(R.dimen.size_normal)
    container.setPadding(p16, p8, container.context.px(R.dimen.size_mini), p8)
    return this
}
