@file:OptIn(ExperimentalFoundationApi::class)

package com.ismartcoding.plain.ui.extensions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange

fun TextFieldBuffer.inlineWrap(
    startWrappedString: String,
    endWrappedString: String = ""
) {
    val initialSelection = selectionInChars
    replace(initialSelection.min, initialSelection.min, startWrappedString)
    replace(
        initialSelection.max + startWrappedString.length,
        initialSelection.max + startWrappedString.length,
        endWrappedString
    )
    selectCharsIn(
        TextRange(
            initialSelection.max + startWrappedString.length,
            initialSelection.max + startWrappedString.length,
        )
    )
}

fun TextFieldBuffer.mark() = inlineWrap("<mark>", "</mark>")

fun TextFieldBuffer.diagram() = inlineWrap("<pre class=\"mermaid\">", "\n</pre>")

fun TextFieldBuffer.quote() {
    val text = toString()
    val lineStart = text.take(selectionInChars.min)
        .lastIndexOf('\n')
        .takeIf { it != -1 }
        ?.let { it + 1 }
        ?: 0

    val initialSelection = selectionInChars

    replace(lineStart, lineStart, "> ")
    selectCharsIn(
        TextRange(
            initialSelection.min + 2,
            initialSelection.max + 2
        )
    )
}

fun TextFieldBuffer.setSelection(index: Int)  {
    selectCharsIn(TextRange(index, index))
}

fun TextFieldBuffer.add(str: String) {
    val initialSelection = selectionInChars
    replace(initialSelection.min, initialSelection.max, str)
}

fun TextFieldBuffer.addLink(link: String) = add(link)

fun TextFieldBuffer.addTask(task: String, checked: Boolean) {
    if (checked) {
        add("- [x] $task")
    } else {
        add("- [ ] $task")
    }
}
