package com.ismartcoding.plain.ui.views

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputLayout
import com.ismartcoding.lib.softinput.hideSoftInput
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IFormItem
import com.ismartcoding.plain.databinding.ViewTextInputBinding
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

class TextInputView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs), IFormItem {
    private val binding = ViewTextInputBinding.inflate(LayoutInflater.from(context), this, true)

    var isRequired = false
    override val hasError: Boolean
        get() {
            return error.isNotEmpty()
        }

    var onBlur: ((hasError: Boolean, value: String) -> Unit)? = null
    var onValidate: ((String) -> String)? = null
    var onTextChanged: ((String) -> Unit)? = null

    var helperText: String
        get() {
            return binding.layout.helperText?.toString() ?: ""
        }
        set(value) {
            binding.layout.helperText = value
        }

    var text: String
        get() {
            return binding.layout.editText?.text?.toString() ?: ""
        }
        set(value) {
            binding.layout.editText?.setText(value)
            error = ""
        }

    var hint: String
        get() {
            return binding.layout.hint?.toString() ?: ""
        }
        set(value) {
            binding.layout.hint = value
        }

    var placeholderText: String
        get() {
            return binding.layout.placeholderText?.toString() ?: ""
        }
        set(value) {
            binding.layout.placeholderText = value
        }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.value.isEnabled = enabled
    }

    var error: String
        get() {
            return binding.layout.error?.toString() ?: ""
        }
        set(value) {
            binding.layout.run {
                error = value
                isErrorEnabled = value.isNotEmpty()
            }
        }

    override fun beforeSubmit() {
        binding.layout.editText?.run {
            if (isFocused) {
                clearFocus()
                hideSoftInput()
            }
            validate(text.toString())
        }
    }

    override fun blurAndHideSoftInput() {
        binding.layout.editText?.run {
            if (isFocused) {
                clearFocus()
                hideSoftInput()
            }
        }
    }

    fun setEndIconOnClick(callback: () -> Unit) {
        binding.layout.setEndIconOnClickListener {
            callback()
        }
    }

    private fun validate(value: String) {
        if (isRequired && text.isEmpty()) {
            error = getString(R.string.input_required)
            return
        }
        error = onValidate?.invoke(value) ?: ""
    }

    var inputType: Int
        set(value) {
            binding.value.inputType = value
        }
        get() = binding.value.inputType

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TextInputView)
        val aHint = a.getString(R.styleable.TextInputView_hint) ?: ""
        val aPlaceHolder = a.getString(R.styleable.TextInputView_placeholderText) ?: ""
        val aHelperText = a.getString(R.styleable.TextInputView_helperText) ?: ""
        isRequired = a.getBoolean(R.styleable.TextInputView_isRequired, false)
        val lines = a.getInt(R.styleable.TextInputView_lines, 1)
        val aEndIconMode = a.getInt(R.styleable.TextInputView_endIconMode, TextInputLayout.END_ICON_CLEAR_TEXT)
        val aEndIconDrawable = a.getDrawable(R.styleable.TextInputView_endIconDrawable)
        val aInputType = a.getInt(R.styleable.TextInputView_android_inputType, -1)

        a.recycle()

        binding.value.run {
            if (lines > 1) {
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                gravity = Gravity.TOP
                imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                isSingleLine = false
            } else {
                inputType =
                    if (aInputType != -1) {
                        aInputType
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    }
                gravity = Gravity.CENTER_VERTICAL
                isSingleLine = true
            }
            setLines(lines)

            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val value = this@TextInputView.text
                    validate(value)
                    onBlur?.invoke(hasError, value)
                }
            }
        }

        binding.layout.run {
            hint = aHint
            placeholderText = aPlaceHolder
            helperText = aHelperText
            endIconMode = aEndIconMode
            endIconDrawable = aEndIconDrawable
            editText?.run {
                addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence,
                            start: Int,
                            count: Int,
                            after: Int,
                        ) {}

                        override fun onTextChanged(
                            s: CharSequence,
                            start: Int,
                            before: Int,
                            count: Int,
                        ) {}

                        override fun afterTextChanged(s: Editable) {
                            binding.layout.editText?.run {
                                val value = s.toString()
                                if (isFocused && hasError) {
                                    validate(value)
                                }
                                if (!hasError) {
                                    onTextChanged?.invoke(value)
                                }
                            }
                        }
                    },
                )

                if (lines == 1) {
                    setOnEditorActionListener { _, actionId, event ->
                        if (arrayListOf(EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_NEXT).contains(actionId) ||
                            (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
                        ) {
                            binding.layout.editText?.run {
                                if (isFocused) {
                                    clearFocus()
                                }
                            }
                        }
                        false
                    }
                }
            }
        }
    }
}
