package com.ismartcoding.plain.ui.models

import org.jetbrains.compose.resources.DrawableResource
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.preferences.EditorAccessoryLevelPreference
import com.ismartcoding.plain.preferences.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preferences.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.ui.extensions.inlineWrap

data class MdAccessoryItem(val text: String, val before: String, val after: String = "")
data class MdAccessoryItem2(val icon: DrawableResource, val click: (MdEditorViewModel) -> Unit = {})

@OptIn(ExperimentalFoundationApi::class)
class MdEditorViewModel : ViewModel() {
    val textFieldState = TextFieldState("")
    var showSettings = mutableStateOf(false)
    var showInsertImage = mutableStateOf(false)
    var showColorPicker = mutableStateOf(false)
    var wrapContent = mutableStateOf(true)
    var showLineNumbers = mutableStateOf(true)
    var syntaxHighLight = mutableStateOf(true)
    var linesText = mutableStateOf("1")
    var level = mutableIntStateOf(0)

    fun load(context: Context) {
        launchSafe {
            level.intValue = EditorAccessoryLevelPreference.getAsync()
            wrapContent.value = EditorWrapContentPreference.getAsync()
            showLineNumbers.value = EditorShowLineNumbersPreference.getAsync()
            syntaxHighLight.value = EditorSyntaxHighlightPreference.getAsync()
        }
    }

    fun toggleLevel(context: Context) {
        level.intValue = if (level.intValue == 1) 0 else 1
        launchSafe {
            EditorAccessoryLevelPreference.putAsync(level.intValue)
        }
    }

    fun toggleLineNumbers(context: Context) {
        showLineNumbers.value = !showLineNumbers.value
        launchSafe {
            EditorShowLineNumbersPreference.putAsync(showLineNumbers.value)
        }
    }

    fun toggleWrapContent(context: Context) {
        wrapContent.value = !wrapContent.value
        launchSafe {
            EditorWrapContentPreference.putAsync(wrapContent.value)
        }
    }

    fun insertColor(color: String) {
        textFieldState.edit { inlineWrap("<font color=\"$color\">", "</font>") }
        showColorPicker.value = false
    }
}
