package com.ismartcoding.plain.ui.views.texteditor

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.text.*
import android.text.method.KeyListener
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.ismartcoding.lib.extensions.dp
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.views.GoodScrollView
import java.util.*

class Editor(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    private var refreshState: ((canRefresh: Boolean) -> Unit)? = null

    private val mPaintNumbers: TextPaint =
        TextPaint().apply {
            isAntiAlias = true
            isDither = false
            textAlign = Paint.Align.RIGHT
            color = context.getColor(R.color.secondary)
        }
    private val editHistory = EditHistory()
    private val mChangeListener = EditTextChangeListener()

    var syntaxHighlight = true
    var isReadOnly = false
    var suggestionActive = true
    var showLineNumbers = true
    var textFontSize = context.dp(R.dimen.text_size_lg).toInt()
    var pageSystem = PageSystem()
    var verticalScroll: GoodScrollView? = null
    var fileExtension = ""
    var onTextChanged: (() -> Unit)? = null
    var wrapContent = false

    /**
     * Disconnect this undo/redo from the text view.
     */
    private var enabledChangeListener = false
    private val _paddingTop = EditTextPadding.getPaddingTop(context)
    private var numbersWidth = 0f
    private var _lineCount = 0
    private var startingLine = 0
    private val lineUtils = LineUtils()

    /**
     * Is undo/redo being performed? This member
     * signals if an undo/redo operation is
     * currently being performed. Changes in the
     * text during undo/redo are not recorded
     * because it would mess up the undo history.
     */
    private var isUndoOrRedo = false
    private var showUndo = false
    private var showRedo = false
    private var _keyListener: KeyListener? = null
    private var firstVisibleIndex = 0
    private var lastVisibleIndex = 0
    private var deviceHeight = resources.displayMetrics.heightPixels
    private var editorHeight = 0

    fun updatePadding() {
        setPadding(
            if (showLineNumbers) {
                EditTextPadding.getPaddingWithLineNumbers(
                    context,
                    textFontSize,
                )
            } else {
                EditTextPadding.getPaddingWithoutLineNumbers(context)
            },
            _paddingTop,
            _paddingTop,
            context.dp2px(16),
        )
    }

    fun insert(
        before: String,
        after: String = "",
    ) {
        val start = selectionStart
        text?.insert(start, before)
        val end = selectionEnd // don't move this line to be before the insert code.
        if (after.isNotEmpty()) {
            text?.insert(end, after)
        }
        setSelection(end)
    }

    //region OVERRIDES
    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        val scale = context.resources.displayMetrics.density
        mPaintNumbers.textSize = size * scale * 0.65f
        numbersWidth = EditTextPadding.getPaddingWithLineNumbers(
            context, textFontSize,
        ) * 0.8f
    }

    override fun onDraw(canvas: Canvas) {
        if (_lineCount != lineCount || startingLine != pageSystem.startingLine) {
            startingLine = pageSystem.startingLine
            _lineCount = lineCount
            lineUtils.updateHasNewLineArray(
                pageSystem.startingLine,
                _lineCount,
                layout,
                text.toString(),
            )
        }
        if (showLineNumbers) {
            for (i in 0 until lineCount) {
                // if last line we count it anyway
                if (!wrapContent ||
                    lineUtils.goodLines[i]
                ) {
                    val realLine = lineUtils.realLines[i]
                    val baseline = getLineBounds(i, null)
                    canvas.drawText(
                        realLine.toString(),
                        numbersWidth,
                        baseline.toFloat(),
                        mPaintNumbers,
                    )
                }
            }
        }
        super.onDraw(canvas)
    }

    //endregion
    //region Other
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return if (event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_A -> onTextContextMenuItem(ID_SELECT_ALL)
                KeyEvent.KEYCODE_X -> onTextContextMenuItem(ID_CUT)
                KeyEvent.KEYCODE_C -> onTextContextMenuItem(ID_COPY)
                KeyEvent.KEYCODE_V -> onTextContextMenuItem(ID_PASTE)
                KeyEvent.KEYCODE_Z -> {
                    if (canUndo) {
                        return onTextContextMenuItem(R.id.undo)
                    }
                    if (canRedo) {
                        return onTextContextMenuItem(R.id.redo)
                    }
                    // mainActivity.saveTheFile(false)
                    true
                }
                KeyEvent.KEYCODE_Y -> {
                    if (canRedo) {
                        return onTextContextMenuItem(R.id.redo)
                    }
                    // mainActivity.saveTheFile(false)
                    true
                }
                KeyEvent.KEYCODE_S -> {
                    // mainActivity.saveTheFile(false)
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
        } else {
            when (keyCode) {
                KeyEvent.KEYCODE_TAB -> {
                    val textToInsert = "  "
                    val start: Int = selectionStart.coerceAtLeast(0)
                    val end: Int = selectionEnd.coerceAtLeast(0)
                    text?.replace(
                        start.coerceAtMost(end),
                        start.coerceAtLeast(end),
                        textToInsert,
                        0,
                        textToInsert.length,
                    )
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return if (event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_S -> true
                else -> false
            }
        } else {
            when (keyCode) {
                KeyEvent.KEYCODE_TAB -> true
                else -> false
            }
        }
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        return when (id) {
            R.id.undo -> {
                undo()
                true
            }
            R.id.redo -> {
                redo()
                true
            }
            else -> {
                super.onTextContextMenuItem(id)
            }
        }
    }

    /**
     * Can undo be performed?
     */
    val canUndo: Boolean
        get() = editHistory.position > 0

    /**
     * Can redo be performed?
     */
    val canRedo: Boolean
        get() = (
            editHistory.position
                < editHistory.history.size
        )

    /**
     * Perform undo.
     */
    fun undo() {
        val edit = editHistory.previous ?: return
        val text: Editable = editableText
        val start = edit.start
        val end = start + if (edit.after != null) edit.after.length else 0
        isUndoOrRedo = true
        text.replace(start, end, edit.before)
        isUndoOrRedo = false

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (o in text.getSpans<UnderlineSpan>(
            0,
            text.length,
            UnderlineSpan::class.java,
        )) {
            text.removeSpan(o)
        }
        Selection.setSelection(
            text,
            if (edit.before == null) start else start + edit.before.length,
        )
    }

    fun paste() {
        onTextContextMenuItem(ID_PASTE)
    }

    /**
     * Perform redo.
     */
    fun redo() {
        val edit = editHistory.next ?: return
        val text: Editable = editableText
        val start = edit.start
        val end = start + if (edit.before != null) edit.before.length else 0
        isUndoOrRedo = true
        text.replace(start, end, edit.after)
        isUndoOrRedo = false

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (o in text.getSpans<UnderlineSpan>(
            0,
            text.length,
            UnderlineSpan::class.java,
        )) {
            text.removeSpan(o)
        }
        Selection.setSelection(
            text,
            if (edit.after == null) start else start + edit.after.length,
        )
    }

    /**
     * Set the maximum history size. If size is
     * negative, then history size is only limited
     * by the device memory.
     */
    fun setMaxHistorySize(maxHistorySize: Int) {
        editHistory.maxHistorySize = maxHistorySize
    }

    fun resetVariables() {
        editHistory.clear()
        enabledChangeListener = false
        _lineCount = 0
        startingLine = 0
        isUndoOrRedo = false
        showUndo = false
        showRedo = false
        firstVisibleIndex = 0
    }

    fun replaceTextKeepCursor(textToUpdate: String?) {
        val cursorPos: Int
        val cursorPosEnd: Int
        if (textToUpdate != null) {
            cursorPos = 0
            cursorPosEnd = 0
        } else {
            cursorPos = selectionStart
            cursorPosEnd = selectionEnd
        }
        disableTextChangedListener()
        if (syntaxHighlight) {
            text =
                highlight(
                    if (textToUpdate == null) {
                        editableText
                    } else {
                        Editable.Factory
                            .getInstance().newEditable(textToUpdate)
                    },
                    textToUpdate != null,
                )
        } else {
            setText(textToUpdate ?: editableText)
        }
        enableTextChangedListener()
        val newCursorPos: Int
        val cursorOnScreen = cursorPos in firstVisibleIndex..lastVisibleIndex
        newCursorPos =
            if (cursorOnScreen) { // if the cursor is on screen
                cursorPos // we don't change its position
            } else {
                firstVisibleIndex // else we set it to the first visible pos
            }
        if (newCursorPos > -1 && newCursorPos <= length()) {
            if (cursorPosEnd != cursorPos) setSelection(cursorPos, cursorPosEnd) else setSelection(newCursorPos)
        }
    }

    fun disableTextChangedListener() {
        enabledChangeListener = false
        removeTextChangedListener(mChangeListener)
    }

    fun highlight(
        editable: Editable,
        newText: Boolean,
    ): Editable {
        editable.clearSpans()
        if (editable.isEmpty()) {
            return editable
        }
        editorHeight = height
        if (!newText && editorHeight > 0) {
            verticalScroll?.let {
                firstVisibleIndex = layout?.getLineStart(LineUtils.getFirstVisibleLine(it, editorHeight, lineCount)) ?: 0
                lastVisibleIndex = layout?.getLineEnd(LineUtils.getLastVisibleLine(it, editorHeight, lineCount, deviceHeight) - 1) ?: 0
            }
        } else {
            firstVisibleIndex = 0
            lastVisibleIndex = CHARS_TO_COLOR
        }
        var firstColoredIndex = firstVisibleIndex - CHARS_TO_COLOR / 5

        // normalize
        if (firstColoredIndex < 0) firstColoredIndex = 0
        if (lastVisibleIndex > editable.length) lastVisibleIndex = editable.length
        if (firstColoredIndex > lastVisibleIndex) firstColoredIndex = lastVisibleIndex
        val textToHighlight = editable.subSequence(firstColoredIndex, lastVisibleIndex)
        val highlightDriver = HighlightDriver(AndroidHighlightColorProvider(), fileExtension)
        val highlights = highlightDriver.highlightText(textToHighlight, firstColoredIndex)
        for ((color, start, end) in highlights) {
            editable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return editable
    }

    fun enableTextChangedListener() {
        if (!enabledChangeListener) {
            addTextChangedListener(mChangeListener)
            enabledChangeListener = true
        }
    }

    fun clearHistory() {
        editHistory.clear()
        showUndo = canUndo
        showRedo = canRedo
    }

    fun storePersistentState(
        editor: SharedPreferences.Editor,
        prefix: String,
    ) {
        // Store hash code of text in the editor so that we can check if the
        // editor contents has changed.
        editor.putString(
            "$prefix.hash",
            text.toString().hashCode().toString(),
        )
        editor.putInt(
            "$prefix.maxSize",
            editHistory.maxHistorySize,
        )
        editor.putInt(
            "$prefix.position",
            editHistory.position,
        )
        editor.putInt(
            "$prefix.size",
            editHistory.history.size,
        )
        editHistory.history.forEachIndexed { i, ei ->
            val pre = "$prefix.$i"
            editor.putInt("$pre.start", ei.start)
            editor.putString(
                "$pre.before",
                ei.before.toString(),
            )
            editor.putString(
                "$pre.after",
                ei.after.toString(),
            )
        }
    }

    /**
     * Restore preferences.
     *
     * @param prefix The preference key prefix
     * used when state was stored.
     * @return did restore succeed? If this is
     * false, the undo history will be empty.
     */
    @Throws(IllegalStateException::class)
    fun restorePersistentState(
        sp: SharedPreferences,
        prefix: String,
    ): Boolean {
        val ok = doRestorePersistentState(sp, prefix)
        if (!ok) {
            editHistory.clear()
        }
        return ok
    }

    private fun doRestorePersistentState(
        sp: SharedPreferences,
        prefix: String,
    ): Boolean {
        val hash: String =
            sp.getString("$prefix.hash", null)
                ?: // No state to be restored.
                return true
        if (Integer.valueOf(hash)
            != text.toString().hashCode()
        ) {
            return false
        }
        editHistory.clear()
        editHistory.maxHistorySize = sp.getInt("$prefix.maxSize", -1)
        val count: Int = sp.getInt("$prefix.size", -1)
        if (count == -1) {
            return false
        }
        for (i in 0 until count) {
            val pre = "$prefix.$i"
            val start: Int = sp.getInt("$pre.start", -1)
            val before = sp.getString("$pre.before", null)
            val after = sp.getString("$pre.after", null)
            if (start == -1 || before == null || after == null) {
                return false
            }
            editHistory.add(
                EditHistory.EditItem(start, before, after),
            )
        }
        editHistory.position = sp.getInt("$prefix.position", -1)
        return editHistory.position != -1
    }

    /**
     * Class that listens to changes in the text.
     */
    private inner class EditTextChangeListener : TextWatcher {
        /**
         * The text that will be removed by the
         * change event.
         */
        private var beforeChange: CharSequence? = null

        /**
         * The text that was inserted by the change
         * event.
         */
        private var afterChange: CharSequence? = null

        override fun beforeTextChanged(
            s: CharSequence,
            start: Int,
            count: Int,
            after: Int,
        ) {
            if (isUndoOrRedo) {
                return
            }
            beforeChange = s.subSequence(start, start + count)
        }

        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int,
        ) {
            if (isUndoOrRedo) {
                return
            }
            afterChange = s.subSequence(start, start + count)
            editHistory.add(
                EditHistory.EditItem(
                    start,
                    beforeChange,
                    afterChange,
                ),
            )
        }

        override fun afterTextChanged(s: Editable) {
            val sUndo = canUndo
            val sRedo = canRedo
            if (sUndo != showUndo || sRedo != showRedo) {
                showUndo = sUndo
                showRedo = sRedo
                // TODO: Update UI
            }
            onTextChanged?.invoke()
        }
    }

    fun setText(text: String) {
        pageSystem = PageSystem(text)
        disableTextChangedListener()
        replaceTextKeepCursor(pageSystem.currentPageText)
        enableTextChangedListener()
    }

    fun getAllText(): String {
        return pageSystem.getAllText(text.toString())
    }

    fun setup() {
        setTextColor(context.getColor(R.color.primary))
        updatePadding()
        if (isReadOnly) {
            _keyListener = keyListener
            keyListener = null
        } else {
            if (_keyListener != null) {
                keyListener = _keyListener
            }
            inputType =
                if (suggestionActive) {
                    (
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                            or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
                    )
                } else {
                    (
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                            or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
                    )
                }
        }
        isFocusable = true
        textSize = textFontSize.toFloat()
        setOnClickListener {
            if (!isReadOnly) {
                verticalScroll?.tempDisableListener(1000)
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(this@Editor, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus && !isReadOnly) {
                verticalScroll?.tempDisableListener(1000)
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(this@Editor, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        setMaxHistorySize(100)
        resetVariables()
    }

    fun setRefreshStateListener(refreshState: ((canRefresh: Boolean) -> Unit)?) {
        this.refreshState = refreshState
    }

    override fun onOverScrolled(
        scrollX: Int,
        scrollY: Int,
        clampedX: Boolean,
        clampedY: Boolean,
    ) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        refreshState?.invoke(clampedY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            refreshState?.invoke(false)
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val ID_SELECT_ALL = android.R.id.selectAll
        private const val ID_CUT = android.R.id.cut
        private const val ID_COPY = android.R.id.copy
        private const val ID_PASTE = android.R.id.paste
        private const val CHARS_TO_COLOR = 2500
    }
}
