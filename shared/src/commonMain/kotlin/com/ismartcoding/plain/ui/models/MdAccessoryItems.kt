package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.i18n.*

import com.ismartcoding.plain.ui.helpers.WebHelper

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
        MdAccessoryItem2(Res.drawable.bold, click = {
            it.insertAtFocused("**", "**")
        }),
        MdAccessoryItem2(Res.drawable.italic, click = {
            it.insertAtFocused("*", "*")
        }),
        MdAccessoryItem2(Res.drawable.underline, click = {
            it.insertAtFocused("<u>", "</u>")
        }),
        MdAccessoryItem2(Res.drawable.strikethrough, click = {
            it.insertAtFocused("~~", "~~")
        }),
        MdAccessoryItem2(Res.drawable.code, click = {
            it.insertAtFocused("```\n", "\n```")
        }),
        MdAccessoryItem2(Res.drawable.superscript, click = {
            it.insertAtFocused("\$\$\n", "\n\$\$")
        }),
        MdAccessoryItem2(
            Res.drawable.table,
            click = {
                it.insertText(
                    """
| HEADER | HEADER | HEADER |
|:----:|:----:|:----:|
|      |      |      |
|      |      |      |
|      |      |      |
"""
                )
            },
        ),
        MdAccessoryItem2(Res.drawable.square_check, click = {
            it.insertAtFocused("\n- [x] ")
        }),
        MdAccessoryItem2(Res.drawable.square, click = {
            it.insertAtFocused("\n- [ ] ")
        }),
        MdAccessoryItem2(Res.drawable.link, click = {
            it.insertAtFocused("[Link](", ")")
        }),
        MdAccessoryItem2(Res.drawable.list_checks, click = {
            it.enterSelectionMode()
        }),
        MdAccessoryItem2(Res.drawable.image, click = {
            it.showInsertImage.value = true
        }),
        MdAccessoryItem2(Res.drawable.paint_bucket, click = {
            it.showColorPicker.value = true
        }),
        MdAccessoryItem2(Res.drawable.arrow_up_to_line, click = {
            it.moveCaretToStart()
        }),
        MdAccessoryItem2(Res.drawable.arrow_down_to_line, click = {
            it.moveCaretToEnd()
        }),
        MdAccessoryItem2(Res.drawable.circle_help, click = {
            WebHelper.open("https://www.markdownguide.org/basic-syntax")
        }),
        MdAccessoryItem2(Res.drawable.settings, click = {
            it.showSettings.value = true
        }),
    )
