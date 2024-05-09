package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscript
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
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

data class MdAccessoryItem(val text: String, val before: String, val after: String = "")
data class MdAccessoryItem2(val icon: ImageVector, val click: (MdEditorViewModel) -> Unit = {})

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
                MdAccessoryItem2(Icons.Outlined.FormatBold, click = {
                    it.textFieldState.edit { inlineWrap("**", "**") }
                }),
                MdAccessoryItem2(Icons.Outlined.FormatItalic, click = {
                    it.textFieldState.edit { inlineWrap("*", "*") }
                }),
                MdAccessoryItem2(Icons.Outlined.FormatUnderlined, click = {
                    it.textFieldState.edit { inlineWrap("<u>", "</u>") }
                }),
                MdAccessoryItem2(Icons.Outlined.FormatStrikethrough, click = {
                    it.textFieldState.edit { inlineWrap("~~", "~~") }
                }),
                MdAccessoryItem2(Icons.Outlined.Code, click = {
                    it.textFieldState.edit { inlineWrap("```\n", "\n```") }
                }),
                MdAccessoryItem2(Icons.Outlined.Subscript, click = {
                    it.textFieldState.edit { inlineWrap("\$\$\n", "\n\$\$") }
                }),
                MdAccessoryItem2(
                    Icons.Outlined.TableView,
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
                MdAccessoryItem2(Icons.Outlined.CheckBox, click = {
                    it.textFieldState.edit { inlineWrap("\n- [x] ") }
                }),
                MdAccessoryItem2(Icons.Outlined.CheckBoxOutlineBlank, click = {
                    it.textFieldState.edit { inlineWrap("\n- [ ] ") }
                }),
                MdAccessoryItem2(Icons.Outlined.Link, click = {
                    it.textFieldState.edit { inlineWrap("[Link](", ")") }
                }),
                MdAccessoryItem2(Icons.Outlined.Image, click = {
                    it.showInsertImage = true
                }),
                MdAccessoryItem2(Icons.Outlined.FormatColorFill, click = {
                    it.showColorPicker = true
                }),
                MdAccessoryItem2(Icons.Outlined.VerticalAlignTop, click = {
                    it.textFieldState.edit { setSelection(0) }
                }),
                MdAccessoryItem2(Icons.Outlined.VerticalAlignBottom, click = {
                    it.textFieldState.edit { setSelection(length) }
                }),
//                MdAccessoryItem2(Icons.Outlined.FindReplace),
                MdAccessoryItem2(Icons.AutoMirrored.Outlined.Help, click = {
                    WebHelper.open(MainActivity.instance.get()!!, "https://www.markdownguide.org/basic-syntax")
                }),
                MdAccessoryItem2(Icons.Outlined.Settings, click = {
                    it.showSettings = true
                }),
            )
    }
}
