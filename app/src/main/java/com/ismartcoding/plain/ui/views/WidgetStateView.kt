package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.apollographql.apollo3.api.Operation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.api.GraphqlApiResult
import com.ismartcoding.plain.databinding.ViewWidgetStateBinding
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.extensions.setSafeClick
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

class WidgetStateView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val binding = ViewWidgetStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun showLoading() {
        binding.state.setBackgroundColor(context.getColor(R.color.blur))
        isVisible = true
        binding.textView.text = getString(R.string.loading)
        binding.textView.setTextColor(ContextCompat.getColor(context, R.color.secondary))
        setSafeClick {}
    }

    fun <D : Operation.Data> update(
        result: GraphqlApiResult<D>,
        refresh: () -> Unit,
    ) {
        if (result.isSuccess()) {
            isVisible = false
        } else {
            isVisible = true
            binding.textView.text = result.getErrorMessage()
            binding.textView.setTextColor(ContextCompat.getColor(context, R.color.red))
            val self = this
            setSafeClick {
                refresh()
                self.isVisible = true
                self.showLoading()
                self.setSafeClick {}
            }
        }
    }

    fun update(
        result: ApiResult,
        refresh: () -> Unit,
    ) {
        if (result.isOk()) {
            isVisible = false
        } else {
            isVisible = true
            binding.textView.text = result.errorMessage()
            binding.textView.setTextColor(ContextCompat.getColor(context, R.color.red))
            val self = this
            setSafeClick {
                refresh()
                self.isVisible = true
                self.showLoading()
                self.setSafeClick {}
            }
        }
    }
}
