package com.ismartcoding.plain.ui.models

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.preferences.EditorAccessoryLevelPreference
import com.ismartcoding.plain.preferences.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preferences.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState

data class MdAccessoryItem(val text: String, val before: String, val after: String = "")
data class MdAccessoryItem2(val icon: DrawableResource, val click: (MdEditorViewModel) -> Unit = {})

class MdEditorViewModel : ViewModel() {
    val blocks = BlockEditorState()
    var showSettings = mutableStateOf(false)
    var showInsertImage = mutableStateOf(false)
    var showColorPicker = mutableStateOf(false)
    var wrapContent = mutableStateOf(true)
    var showLineNumbers = mutableStateOf(true)
    var syntaxHighLight = mutableStateOf(true)
    var level = mutableIntStateOf(0)

    init {
        blocks.start(viewModelScope)
    }

    fun load() {
        viewModelScope.launchSafe {
            level.intValue = EditorAccessoryLevelPreference.getAsync()
            wrapContent.value = EditorWrapContentPreference.getAsync()
            showLineNumbers.value = EditorShowLineNumbersPreference.getAsync()
            syntaxHighLight.value = EditorSyntaxHighlightPreference.getAsync()
        }
    }

    fun loadText(text: String) = blocks.loadText(text)

    fun text(): String = blocks.text()

    val canUndo get() = blocks.canUndo
    val canRedo get() = blocks.canRedo

    fun undo() = blocks.undo()

    fun redo() = blocks.redo()

    fun insertAtFocused(before: String, after: String = "") = blocks.insertAtFocused(before, after)

    fun insertText(s: String) = blocks.insertText(s)

    fun moveCaretToStart() = blocks.moveCaretToStart()

    fun moveCaretToEnd() = blocks.moveCaretToEnd()

    // ---- cross-block selection ----

    fun enterSelectionMode() = blocks.enterSelectionMode()

    fun exitSelectionMode() = blocks.exitSelectionMode()

    fun selectAllBlocks() = blocks.selectAllBlocks()

    fun copySelected(): Boolean {
        val t = blocks.selectedText() ?: return false
        setClipboardText("text", t)
        return true
    }

    fun cutSelected() {
        if (copySelected()) blocks.deleteSelectedRange()
    }

    fun toggleLevel() {
        level.intValue = if (level.intValue == 1) 0 else 1
        viewModelScope.launchSafe {
            EditorAccessoryLevelPreference.putAsync(level.intValue)
        }
    }

    fun toggleLineNumbers() {
        showLineNumbers.value = !showLineNumbers.value
        viewModelScope.launchSafe {
            EditorShowLineNumbersPreference.putAsync(showLineNumbers.value)
        }
    }

    fun toggleWrapContent() {
        wrapContent.value = !wrapContent.value
        viewModelScope.launchSafe {
            EditorWrapContentPreference.putAsync(wrapContent.value)
        }
    }

    fun insertColor(color: String) {
        insertAtFocused("<font color=\"$color\">", "</font>")
        showColorPicker.value = false
    }
}
