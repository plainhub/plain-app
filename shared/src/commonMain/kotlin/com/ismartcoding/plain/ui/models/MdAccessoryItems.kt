package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.DrawableResource

data class MdToolbarItem(
    val tip: String,
    val caption: String? = null,
    val icon: DrawableResource? = null,
    val click: (MdEditorViewModel) -> Unit = {},
)

data class MdToolbarCategory(
    val key: String,
    val tip: String,
    val icon: DrawableResource? = null,
    val items: List<MdToolbarItem>,
)

val mdToolbarCategories = listOf(
    MdToolbarCategory(
        key = "style", tip = "Style", icon = Res.drawable.match_case,
        items = listOf(
            MdToolbarItem("Bold", caption = "Bold", icon = Res.drawable.format_bold, click = { it.toggleWrap("**") }),
            MdToolbarItem("Italic", caption = "Italic", icon = Res.drawable.format_italic, click = { it.toggleWrap("*") }),
            MdToolbarItem("Underline", caption = "Underline", icon = Res.drawable.format_underlined, click = { it.toggleWrap("<u>", "</u>") }),
            MdToolbarItem("Strikethrough", caption = "Strike", icon = Res.drawable.strikethrough_s, click = { it.toggleWrap("~~") }),
            MdToolbarItem("Text color", caption = "Color", icon = Res.drawable.format_color_fill, click = { it.showColorPicker.value = true }),
            MdToolbarItem("Highlight", caption = "Highlight", icon = Res.drawable.highlight, click = { it.toggleWrap("<mark>", "</mark>") }),
        ),
    ),
    MdToolbarCategory(
        key = "heading", tip = "Heading", icon = Res.drawable.title,
        items = listOf(
            MdToolbarItem("Normal", caption = "Normal", icon = Res.drawable.format_clear, click = { it.toggleLinePrefix("") }),
            MdToolbarItem("Heading 1", icon = Res.drawable.format_h1, click = { it.toggleLinePrefix("# ") }),
            MdToolbarItem("Heading 2", icon = Res.drawable.format_h2, click = { it.toggleLinePrefix("## ") }),
            MdToolbarItem("Heading 3", icon = Res.drawable.format_h3, click = { it.toggleLinePrefix("### ") }),
            MdToolbarItem("Heading 4", icon = Res.drawable.format_h4, click = { it.toggleLinePrefix("#### ") }),
            MdToolbarItem("Heading 5", icon = Res.drawable.format_h5, click = { it.toggleLinePrefix("##### ") }),
            MdToolbarItem("Heading 6", icon = Res.drawable.format_h6, click = { it.toggleLinePrefix("###### ") }),
        ),
    ),
    MdToolbarCategory(
        key = "list", tip = "List", icon = Res.drawable.format_list_bulleted,
        items = listOf(
            MdToolbarItem("Bulleted list", caption = "Bullet", icon = Res.drawable.format_list_bulleted, click = { it.toggleLinePrefix("- ") }),
            MdToolbarItem("Numbered list", caption = "Number", icon = Res.drawable.format_list_numbered, click = { it.toggleLinePrefix("1. ") }),
            MdToolbarItem("Task list", caption = "Task", icon = Res.drawable.checklist, click = { it.toggleLinePrefix("- [ ] ") }),
        ),
    ),
    MdToolbarCategory(
        key = "insert", tip = "Insert", icon = Res.drawable.add,
        items = listOf(
            MdToolbarItem("Link", caption = "Link", icon = Res.drawable.link, click = { it.insertAtFocused("[Link](", ")") }),
            MdToolbarItem("Image", caption = "Image", icon = Res.drawable.image, click = { it.showInsertImage.value = true }),
            MdToolbarItem(
                "Table",
                caption = "Table",
                icon = Res.drawable.table,
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
            MdToolbarItem("Divider", caption = "Divider", icon = Res.drawable.horizontal_rule, click = { it.insertText("\n---\n") }),
        ),
    ),
    MdToolbarCategory(
        key = "code", tip = "Code", icon = Res.drawable.code,
        items = listOf(
            MdToolbarItem("Inline code", caption = "Inline", icon = Res.drawable.code, click = { it.toggleWrap("`") }),
            MdToolbarItem("Code block", caption = "Block", icon = Res.drawable.code_blocks, click = { it.insertAtFocused("```\n", "\n```") }),
        ),
    ),
    MdToolbarCategory(
        key = "math", tip = "Math", icon = Res.drawable.functions,
        items = listOf(
            MdToolbarItem("Superscript", caption = "Sup", icon = Res.drawable.superscript, click = { it.toggleWrap("^") }),
            MdToolbarItem("Subscript", caption = "Sub", icon = Res.drawable.subscript, click = { it.toggleWrap("~") }),
            MdToolbarItem("Inline math", caption = "Inline", icon = Res.drawable.calculate, click = { it.toggleWrap("$") }),
            MdToolbarItem("Math block", caption = "Block", icon = Res.drawable.functions, click = { it.insertAtFocused("\$\$\n", "\n\$\$") }),
        ),
    ),
    MdToolbarCategory(
        key = "more", tip = "More", icon = Res.drawable.more_horiz,
        items = listOf(
            MdToolbarItem("Quote", caption = "Quote", icon = Res.drawable.format_quote, click = { it.toggleLinePrefix("> ") }),
            MdToolbarItem("Callout", caption = "Callout", icon = Res.drawable.info, click = { it.toggleLinePrefix("> [!note] ") }),
            MdToolbarItem("Footnote", caption = "Footnote", icon = Res.drawable.bookmark, click = { it.insertAtFocused("[^1]: ", "") }),
            MdToolbarItem("Comment", caption = "Comment", icon = Res.drawable.comment, click = { it.toggleWrap("%%") }),
            MdToolbarItem(
                "Details",
                caption = "Details",
                icon = Res.drawable.expand_more,
                click = { it.insertAtFocused("<details>\n<summary>Title</summary>\n", "\n</details>") },
            ),
            MdToolbarItem("Select blocks", caption = "Select", icon = Res.drawable.select_all, click = { it.enterSelectionMode() }),
        ),
    ),
)
