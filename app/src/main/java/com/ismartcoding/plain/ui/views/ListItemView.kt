package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IFormItem
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.extensions.*

class ListItemView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs), IFormItem {
    private val binding = ViewListItemBinding.inflate(LayoutInflater.from(context), this, true)

    var isRequired = false
    var requiredErrorText = getString(R.string.input_required)
    var selectValue: String = ""
    private var mIsTextValueMasked = false
    private var mTextValue = ""

    var error: String
        get() {
            return binding.error.text.toString()
        }
        set(value) {
            binding.errorSection.visibility = if (value.isNotEmpty()) View.VISIBLE else View.GONE
            binding.error.text = value
        }

    var onValidate: ((String) -> String)? = null

    override fun beforeSubmit() {
        validate()
    }

    override fun blurAndHideSoftInput() {
    }

    override val hasError: Boolean
        get() {
            return error.isNotEmpty()
        }

    fun validate() {
        if (isRequired && selectValue.isEmpty()) {
            error = requiredErrorText
            return
        }
        error = onValidate?.invoke(selectValue) ?: ""
    }

    fun setClick(callback: (() -> Unit)?) {
        binding.setClick(callback)
    }

    fun setSelect() {
        setValueText(getString(R.string.tap_to_select))
        setValueTextColor(R.color.purple)
    }

    fun setKeyText(text: String) {
        binding.setKeyText(text)
    }

    fun setValueText(text: String) {
        binding.setValueText(text)
    }

    fun setValueTextColor(color: Int) {
        binding.textValue.setTextColor(ContextCompat.getColor(context, color))
    }

    fun maskValue() {
        mIsTextValueMasked = true
        mTextValue = binding.textValue.text.toString()
        setValueText("******")
    }

    fun revealValue() {
        mIsTextValueMasked = false
        setValueText(mTextValue)
    }

    fun setPasswordMode() {
        maskValue()
        setClick {
            if (mIsTextValueMasked) {
                revealValue()
            } else {
                maskValue()
            }
        }
    }

    fun setEndIcon(
        @DrawableRes iconId: Int,
        clickCallback: (() -> Unit)? = null,
    ) {
        binding.endIcon.run {
            visibility = View.VISIBLE
            setImageResource(iconId)
            if (clickCallback != null) {
                setSafeClick {
                    clickCallback.invoke()
                }
            }
        }
    }

    fun setStartIcon(
        @DrawableRes iconId: Int,
    ) {
        binding.startIcon.visibility = View.VISIBLE
        binding.startIcon.setImageResource(iconId)
    }

    fun setStartIconColor(
        @ColorRes colorId: Int,
    ) {
        binding.startIcon.setColorFilter(context.getColor(colorId))
    }

    fun showSelected() {
        setEndIcon(R.drawable.ic_done)
    }

    fun hideEndIcon() {
        binding.endIcon.visibility = View.GONE
    }

    fun showMore() {
        setEndIcon(R.drawable.ic_chevron_right)
    }

    fun enableSwipeButton(enable: Boolean) {
        binding.enableSwipeMenu(enable)
    }

    fun setLeftSwipeButton(
        buttonText: String,
        buttonCallback: () -> Unit,
    ) {
        binding.setLeftSwipeButton(buttonText, buttonCallback)
    }

    fun setRightSwipeButton(
        buttonText: String,
        buttonCallback: () -> Unit,
    ) {
        binding.setRightSwipeButton(buttonText, buttonCallback)
    }

    fun setChips(
        items: List<ChipItem>,
        value: String,
        onChanged: ((String) -> Unit)? = null,
    ) {
        selectValue = value
        binding.chipGroup.run {
            visibility = View.VISIBLE
            initView(items, value, false) {
                selectValue = it
                onChanged?.invoke(it)
            }
        }
    }

    fun setSwitch(
        enable: Boolean,
        onChanged: ((CompoundButton, Boolean) -> Unit)? = null,
    ) {
        binding.setSwitch(enable, onChanged)
    }

    fun setSwitchEnable(enable: Boolean) {
        binding.setSwitchEnable(enable)
    }

    fun addTextRow(text: String) {
        binding.addTextRow(text)
    }

    fun clearTextRows() {
        binding.clearTextRows()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    companion object {
        fun createItem(
            context: Context,
            key: String,
            value: String,
            click: (() -> Unit)? = null,
        ): ListItemView {
            val row = ListItemView(context)
            row.run {
                setKeyText(key)
                setValueText(value)
                setClick(click)
            }

            return row
        }

        fun createCommandItem(
            context: Context,
            keyTextId: Int,
            click: () -> Unit,
        ): ListItemView {
            val row = ListItemView(context)
            row.setKeyText(getString(keyTextId))
            row.setClick(click)

            return row
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ListItemView)
        val keyText = a.getString(R.styleable.ListItemView_keyText) ?: ""
        val keyTextColor = a.getColor(R.styleable.ListItemView_keyTextColor, -1)
        val valueText = a.getString(R.styleable.ListItemView_valueText) ?: ""
        val showMore = a.getBoolean(R.styleable.ListItemView_showMore, false)
        requiredErrorText = a.getString(R.styleable.ListItemView_requiredErrorText) ?: ""
        isRequired = a.getBoolean(R.styleable.ListItemView_isRequired, false)
        a.recycle()

        if (keyTextColor != -1) {
            binding.textKey.setTextColor(keyTextColor)
        }
        setKeyText(keyText)
        setValueText(valueText)
        if (showMore) {
            showMore()
        }
    }
}
