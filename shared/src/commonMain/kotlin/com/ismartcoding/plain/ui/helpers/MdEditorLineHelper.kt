package com.ismartcoding.plain.ui.helpers

import androidx.compose.ui.text.TextLayoutResult

object MdEditorLineHelper {
    fun getLinesText(
        lineCount: Int,
        layout: TextLayoutResult,
        text: String,
    ): String {
        val hasNewLineArray = BooleanArray(lineCount)
        val goodLines = BooleanArray(lineCount)
        val realLines = IntArray(lineCount)

        if (text.isEmpty()) {
            goodLines[0] = true
            realLines[0] = 1
            return " 1 "
        }

        var i = 0

        // for every line on the edittext
        while (i < lineCount) {
            // check if this line contains "\n" or it is the last line
            // hasNewLineArray[i] = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).endsWith("\n");
            hasNewLineArray[i] = text[layout.getLineEnd(i) - 1] == '\n' || i == lineCount - 1
            // if true
            if (hasNewLineArray[i]) {
                var j = i - 1
                while (j > -1 && !hasNewLineArray[j]) {
                    j--
                }
                goodLines[j + 1] = true
            }
            i++
        }

        var realLine = 0 // the first line is not 0, is 1. We start counting from 1
        i = 0
        while (i < goodLines.size) {
            if (goodLines[i]) {
                realLine++
            }
            realLines[i] = realLine
            i++
        }

        val lines = Array(lineCount) { "" }
        val maxLength = (lineCount + 1).toString().length
        goodLines.forEachIndexed { index, b ->
            lines[index] = if (b) {
                StringBuilder(" ").append(realLines[index].toString().padStart(maxLength)).append(" ").toString()
            } else {
                ""
            }
        }

        return lines.joinToString("\n")
    }
}
