package com.ismartcoding.plain.ui.models

import org.jetbrains.compose.resources.DrawableResource
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.plain.preferences.EditorAccessoryLevelPreference
import com.ismartcoding.plain.preferences.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preferences.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.ui.extensions.inlineWrap

data class MdAccessoryItem(val text: String, val before: String, val after: String = "")
data class MdAccessoryItem2(val icon: DrawableResource, val click: (MdEditorViewModel) -> Unit = {})

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
        launchSafe {
            level = EditorAccessoryLevelPreference.getAsync()
            wrapContent = EditorWrapContentPreference.getAsync()
            showLineNumbers = EditorShowLineNumbersPreference.getAsync()
            syntaxHighLight = EditorSyntaxHighlightPreference.getAsync()
        }
    }

    fun toggleLevel(context: Context) {
        level = if (level == 1) 0 else 1
        launchSafe {
            EditorAccessoryLevelPreference.putAsync(level)
        }
    }

    fun toggleLineNumbers(context: Context) {
        showLineNumbers = !showLineNumbers
        launchSafe {
            EditorShowLineNumbersPreference.putAsync(showLineNumbers)
        }
    }

    fun toggleWrapContent(context: Context) {
        wrapContent = !wrapContent
        launchSafe {
            EditorWrapContentPreference.putAsync(wrapContent)
        }
    }

    fun insertColor(color: String) {
        textFieldState.edit { inlineWrap("<font color=\"$color\">", "</font>") }
        showColorPicker = false
    }
}
