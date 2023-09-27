package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ViewNoDataBinding
import com.ismartcoding.plain.ui.extensions.setSafeClick

class NoDataView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val binding = ViewNoDataBinding.inflate(LayoutInflater.from(context), this, true)

    fun setText(text: String) {
        binding.text.text = text
    }

    fun setButton(
        text: String,
        onClick: () -> Unit,
    ) {
        binding.button.run {
            this.text = text
            isVisible = true
            setSafeClick {
                onClick()
            }
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.NoDataView)
        val text = a.getString(R.styleable.NoDataView_text) ?: ""
        a.recycle()
        binding.text.text = text
    }
}
