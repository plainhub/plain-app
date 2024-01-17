package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.ismartcoding.lib.roundview.setStrokeColor
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ViewLoadingButtonBinding
import com.ismartcoding.plain.ui.extensions.alphaEnable

enum class ButtonType {
    NORMAL,
    DANGER,
}

class LoadingButtonView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val binding = ViewLoadingButtonBinding.inflate(LayoutInflater.from(context), this, true)

    private var normalText = ""
    private var loadingText = ""
    private var buttonType = ButtonType.NORMAL

    fun enable(enable: Boolean) {
        this.alphaEnable(enable)
    }

    fun showLoading() {
        this.alphaEnable(false)
        binding.loading.visibility = View.VISIBLE
        binding.text.text = loadingText
    }

    fun hideLoading() {
        this.alphaEnable(true)
        binding.loading.visibility = View.GONE
        binding.text.text = normalText
    }

    fun setText(text: String) {
        normalText = text
        binding.text.text = normalText
    }

    fun setButtonType(type: ButtonType) {
        buttonType = type
        when (type) {
            ButtonType.NORMAL -> {
                binding.container.setStrokeColor(ContextCompat.getColor(context, R.color.primary))
                binding.text.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
            ButtonType.DANGER -> {
                binding.container.setStrokeColor(ContextCompat.getColor(context, R.color.red))
                binding.text.setTextColor(ContextCompat.getColor(context, R.color.red))
            }
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.LoadingButtonView)
        normalText = a.getString(R.styleable.LoadingButtonView_text) ?: ""
        loadingText = a.getString(R.styleable.LoadingButtonView_loadingText) ?: ""
        buttonType = ButtonType.entries.toTypedArray()[a.getInt(R.styleable.LoadingButtonView_buttonType, 0)]

        a.recycle()
        binding.text.text = normalText
        setButtonType(buttonType)
    }
}
