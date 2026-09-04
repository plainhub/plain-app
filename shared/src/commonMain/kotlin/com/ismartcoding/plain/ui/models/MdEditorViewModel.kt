package com.ismartcoding.plain.ui.models

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState

data class MdAccessoryItem(val text: String, val before: String, val after: String = "")
data class MdAccessoryItem2(val icon: DrawableResource, val click: (MdEditorViewModel) -> Unit = {})

class MdEditorViewModel : ViewModel() {
    val blocks = BlockEditorState()
    var showInsertImage = mutableStateOf(false)
    var showColorPicker = mutableStateOf(false)

    init {
        blocks.start(viewModelScope)
    }

    fun loadText(text: String) = blocks.loadText(text)

    fun text(): String = blocks.text()

    val canUndo get() = blocks.canUndo
    val canRedo get() = blocks.canRedo

    fun undo() = blocks.undo()

    fun redo() = blocks.redo()

    fun insertAtFocused(before: String, after: String = "") = blocks.insertAtFocused(before, after)

    fun toggleWrap(before: String, after: String = "") =
        blocks.toggleWrap(before, after.ifEmpty { before })

    fun insertText(s: String) = blocks.insertText(s)

    fun toggleLinePrefix(prefix: String) = blocks.toggleLinePrefix(prefix)

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

    fun deleteSelected() = blocks.deleteSelectedRange()

    fun insertColor(color: String) {
        insertAtFocused("<font color=\"$color\">", "</font>")
        showColorPicker.value = false
    }
}
