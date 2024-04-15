package com.ismartcoding.plain.ui.views.texteditor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.extensions.getWindowWidth
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.hideSoftInput
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preference.EditorAccessoryLevelPreference
import com.ismartcoding.plain.preference.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preference.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.preference.EditorWrapContentPreference
import com.ismartcoding.plain.databinding.ViewTextEditorBinding
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.views.CustomViewBase
import kotlinx.coroutines.launch
import kotlin.math.abs

class TextEditorView(context: Context, attrs: AttributeSet?) : CustomViewBase(context, attrs), PageSystemButtons.PageButtonsInterface {
    private val binding = ViewTextEditorBinding.inflate(LayoutInflater.from(context), this, true)

    private val pageSystemButtons = PageSystemButtons(context, this, binding.fabPrev, binding.fabNext)

    private val updateHandler = Handler(Looper.getMainLooper())
    private val colorRunnableDuringScroll = Runnable { binding.editor.replaceTextKeepCursor(null) }
    private var isInitialized = false
    private var oldText = ""
    var onTextChanged: (() -> Unit)? = null

    fun hideSoftInput() {
        binding.editor.hideSoftInput()
    }

    fun isChanged(): Boolean {
        return oldText != getText()
    }

    fun resetChangedState() {
        oldText = getText()
    }

    @SuppressLint("ClickableViewAccessibility")
    suspend fun initViewAsync(
        lifecycle: Lifecycle,
        text: String = "",
        extension: String = "",
    ) {
        oldText = text
        if (isInitialized) {
            binding.editor.setText(text)
            return
        }

        registerLifecycleOwner(lifecycle)

        isInitialized = true

        val wrapContent = withIO { EditorWrapContentPreference.getAsync(context) }
        val syntaxHighlight = withIO { EditorSyntaxHighlightPreference.getAsync(context) }
        binding.editor.apply {
            verticalScroll = binding.verticalScroll
            fileExtension = extension
            showLineNumbers = withIO { EditorShowLineNumbersPreference.getAsync(context) }
            this.syntaxHighlight = syntaxHighlight
            this.wrapContent = wrapContent
            minWidth = context.getWindowWidth()
            pageSystem.onPageChanged = {
                pageSystemButtons.updateVisibility(false)
                clearHistory()
            }
        }
        if (wrapContent) {
            binding.horizontalScroll.removeView(binding.editor)
            binding.verticalScroll.removeView(binding.horizontalScroll)
            binding.verticalScroll.addView(binding.editor)
        }

        binding.editor.onTextChanged = {
            onTextChanged?.invoke()
            // updateTextSyntax()
        }

        binding.accessory.getEditor = {
            binding.editor
        }

        binding.accessory2.getEditor = {
            binding.editor
        }

        binding.toggle.setSafeClick {
            lifecycle.coroutineScope.launch {
                if (withIO { EditorAccessoryLevelPreference.getAsync(context) } == 0) {
                    withIO { EditorAccessoryLevelPreference.putAsync(context, 1) }
                } else {
                    withIO { EditorAccessoryLevelPreference.putAsync(context, 0) }
                }
                updateAccessoryVisible()
            }
        }

        updateAccessoryVisible()

        binding.verticalScroll.onScrollChanged = { _: Int, t: Int, _: Int, _: Int ->
            pageSystemButtons.updateVisibility(abs(t) > 10)
            updateTextSyntax()
        }
        binding.editor.setup()
        binding.editor.setText(text)
        binding.editor.enableTextChangedListener()
        if (syntaxHighlight) {
            updateTextSyntax()
        }

        events.add(
            receiveEventHandler<EditorSettingsChangedEvent> { event ->
                when (event.type) {
                    EditorSettingsType.WRAP_CONTENT -> {
                        lifecycle.coroutineScope.launch {
                            updateWrapContent()
                        }
                    }

                    EditorSettingsType.LINE_NUMBERS -> {
                        lifecycle.coroutineScope.launch {
                            binding.editor.apply {
                                showLineNumbers = withIO { EditorShowLineNumbersPreference.getAsync(context) }
                                disableTextChangedListener()
                                replaceTextKeepCursor(null)
                                enableTextChangedListener()
                                updatePadding()
                            }
                        }
                    }

                    EditorSettingsType.SYNTAX_HIGHLIGHT -> {
                        lifecycle.coroutineScope.launch {
                            binding.editor.apply {
                                this.syntaxHighlight = withIO { EditorSyntaxHighlightPreference.getAsync(context) }
                                disableTextChangedListener()
                                replaceTextKeepCursor(text.toString())
                                enableTextChangedListener()
                            }
                        }
                    }
                }
            },
        )

        events.add(
            receiveEventHandler<EditorInsertImageEvent> { event ->
                var html = "<img src=\"${event.url}\""
                if (event.width.isNotEmpty()) {
                    html += " width=\"${event.width}\""
                }
                if (event.description.isNotEmpty()) {
                    html += " alt=\"${event.description}\""
                }
                binding.editor.insert("$html />")
            },
        )
    }

    private suspend fun updateWrapContent() {
        val wrapContent = withIO { EditorWrapContentPreference.getAsync(context) }
        binding.editor.wrapContent = wrapContent
        if (wrapContent) {
            binding.horizontalScroll.removeView(binding.editor)
            binding.verticalScroll.removeView(binding.horizontalScroll)
            binding.verticalScroll.addView(binding.editor)
        } else {
            binding.verticalScroll.removeView(binding.editor)
            binding.verticalScroll.addView(binding.horizontalScroll)
            binding.horizontalScroll.addView(binding.editor)
        }
    }

    private suspend fun updateAccessoryVisible() {
        if (withIO { EditorAccessoryLevelPreference.getAsync(context) } == 0) {
            binding.toggle.setIconResource(R.drawable.ic_one)
            binding.accessory.isVisible = true
            binding.accessory2.isVisible = false
        } else {
            binding.toggle.setIconResource(R.drawable.ic_two)
            binding.accessory.isVisible = false
            binding.accessory2.isVisible = true
        }
    }

    private fun updateTextSyntax() {
        updateHandler.removeCallbacks(colorRunnableDuringScroll)
        updateHandler.postDelayed(colorRunnableDuringScroll, SYNTAX_DELAY_MILLIS_SHORT.toLong())
    }

    fun getText(): String {
        return binding.editor.getAllText()
    }

    override fun nextPageClicked() {
        binding.editor.run {
            pageSystem.savePage(text.toString())
            pageSystem.nextPage()
            disableTextChangedListener()
            replaceTextKeepCursor(pageSystem.currentPageText)
            enableTextChangedListener()
        }
        binding.verticalScroll.postDelayed({ binding.verticalScroll.smoothScrollTo(0, 0) }, 200)
    }

    override fun prevPageClicked() {
        binding.editor.run {
            pageSystem.savePage(text.toString())
            pageSystem.prevPage()
            disableTextChangedListener()
            replaceTextKeepCursor(pageSystem.currentPageText)
            enableTextChangedListener()
        }
        binding.verticalScroll.postDelayed({ binding.verticalScroll.smoothScrollTo(0, 0) }, 200)
    }

    override fun pageSystemButtonLongClicked() {
        binding.editor.run {
            val maxPages = pageSystem.maxPage
            val currentPage = pageSystem.currentPage
            //        NumberPickerDialog.newInstance(NumberPickerDialog.Actions.SelectPage, 0, currentPage, maxPages).show(fragmentManager.beginTransaction(), "dialog")
        }
    }

    override fun canReadNextPage(): Boolean {
        return binding.editor.pageSystem.canReadNextPage()
    }

    override fun canReadPrevPage(): Boolean {
        return binding.editor.pageSystem.canReadPrevPage()
    }

    fun setRefreshStateListener(refreshState: ((canRefresh: Boolean) -> Unit)?) {
        binding.editor.setRefreshStateListener(refreshState)
    }

    companion object {
        private const val SYNTAX_DELAY_MILLIS_SHORT = 250
    }
}
