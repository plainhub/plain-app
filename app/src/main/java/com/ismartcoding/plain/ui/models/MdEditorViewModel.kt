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
import com.ismartcoding.plain.preferences.EditorAccessoryLevelPreference
import com.ismartcoding.plain.preferences.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preferences.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.ui.extensions.inlineWrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
}
