package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.plain.preference.EditorAccessoryLevelPreference
import com.ismartcoding.plain.preference.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preference.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.preference.EditorWrapContentPreference
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.extensions.add
import com.ismartcoding.plain.ui.extensions.inlineWrap
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.helpers.WebHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.R

data class MdAccessoryItem(val text: String, val before: String, val after: String = "")
data class MdAccessoryItem2(val icon: Int, val click: (MdEditorViewModel) -> Unit = {})

@OptIn(ExperimentalFoundationApi::class, SavedStateHandleSaveableApi::class)
class MdEditorViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val textFieldState = TextFieldState("")
    var showSettings by savedStateHandle.saveable { mutableStateOf(false) }
    var showInsertImage by savedStateHandle.saveable { mutableStateOf(false) }
    var showColorPicker by savedStateHandle.saveable { mutableStateOf(false) }
    var wrapContent by savedStateHandle.saveable { mutableStateOf(true) }
    var showLineNumbers by savedStateHandle.saveable { mutableStateOf(true) }
    var syntaxHighLight by savedStateHandle.saveable { mutableStateOf(true) }
    var linesText by savedStateHandle.saveable { mutableStateOf("1") }
    var level by savedStateHandle.saveable { mutableIntStateOf(0) }

    fun load(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            level = EditorAccessoryLevelPreference.getAsync(context)
            wrapContent = EditorWrapContentPreference.getAsync(context)
            showLineNumbers = EditorShowLineNumbersPreference.getAsync(context)
            syntaxHighLight = EditorSyntaxHighlightPreference.getAsync(context)
        }
    }

    fun toggleLevel(context: Context) {
        level = if (level == 1) 0 else 1
        viewModelScope.launch(Dispatchers.IO) {
            EditorAccessoryLevelPreference.putAsync(context, level)
        }
    }

    fun toggleLineNumbers(context: Context) {
        showLineNumbers = !showLineNumbers
        viewModelScope.launch(Dispatchers.IO) {
            EditorShowLineNumbersPreference.putAsync(context, showLineNumbers)
        }
    }

    fun toggleWrapContent(context: Context) {
        wrapContent = !wrapContent
        viewModelScope.launch(Dispatchers.IO) {
            EditorWrapContentPreference.putAsync(context, wrapContent)
        }
    }

    fun insertColor(color: String) {
        textFieldState.edit { inlineWrap("<font color=\"$color\">", "</font>") }
        showColorPicker = false
    }

    companion object {
        val mdAccessoryItems = listOf(
            MdAccessoryItem("*", "*"),
            MdAccessoryItem("_", "_"),
            MdAccessoryItem("`", "`"),
            MdAccessoryItem("#", "#"),
            MdAccessoryItem("-", "-"),
            MdAccessoryItem(">", ">"),
            MdAccessoryItem("<", "<"),
            MdAccessoryItem("/", "/"),
            MdAccessoryItem("\\", "\\"),
            MdAccessoryItem("|", "|"),
            MdAccessoryItem("!", "!"),
            MdAccessoryItem("[]", "[", "]"),
            MdAccessoryItem("()", "(", ")"),
            MdAccessoryItem("{}", "{", "}"),
            MdAccessoryItem("<>", "<", ">"),
            MdAccessoryItem("$", "$"),
            MdAccessoryItem("\"", "\""),
        )

        val mdAccessoryItems2 =
            listOf(
                MdAccessoryItem2(R.drawable.bold, click = {
                    it.textFieldState.edit { inlineWrap("**", "**") }
                }),
                MdAccessoryItem2(R.drawable.italic, click = {
                    it.textFieldState.edit { inlineWrap("*", "*") }
                }),
                MdAccessoryItem2(R.drawable.underline, click = {
                    it.textFieldState.edit { inlineWrap("<u>", "</u>") }
                }),
                MdAccessoryItem2(R.drawable.strikethrough, click = {
                    it.textFieldState.edit { inlineWrap("~~", "~~") }
                }),
                MdAccessoryItem2(R.drawable.code, click = {
                    it.textFieldState.edit { inlineWrap("```\n", "\n```") }
                }),
                MdAccessoryItem2(R.drawable.superscript, click = {
                    it.textFieldState.edit { inlineWrap("\$\$\n", "\n\$\$") }
                }),
                MdAccessoryItem2(
                    R.drawable.table,
                    click = {
                        it.textFieldState.edit {
                            add(
                                """
| HEADER | HEADER | HEADER |
|:----:|:----:|:----:|
|      |      |      |
|      |      |      |
|      |      |      |
"""
                            )
                        }
                    },
                ),
                MdAccessoryItem2(R.drawable.square_check, click = {
                    it.textFieldState.edit { inlineWrap("\n- [x] ") }
                }),
                MdAccessoryItem2(R.drawable.square, click = {
                    it.textFieldState.edit { inlineWrap("\n- [ ] ") }
                }),
                MdAccessoryItem2(R.drawable.link, click = {
                    it.textFieldState.edit { inlineWrap("[Link](", ")") }
                }),
                MdAccessoryItem2(R.drawable.image, click = {
                    it.showInsertImage = true
                }),
                MdAccessoryItem2(R.drawable.paint_bucket, click = {
                    it.showColorPicker = true
                }),
                MdAccessoryItem2(R.drawable.arrow_up_to_line, click = {
                    it.textFieldState.edit { setSelection(0) }
                }),
                MdAccessoryItem2(R.drawable.arrow_down_to_line, click = {
                    it.textFieldState.edit { setSelection(length) }
                }),
                MdAccessoryItem2(R.drawable.circle_help, click = {
                    WebHelper.open(MainActivity.instance.get()!!, "https://www.markdownguide.org/basic-syntax")
                }),
                MdAccessoryItem2(R.drawable.settings, click = {
                    it.showSettings = true
                }),
            )
    }
}
