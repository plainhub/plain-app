package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.extensions.add
import com.ismartcoding.plain.ui.extensions.inlineWrap
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.MainActivity

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
