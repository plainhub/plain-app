package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class MdToolbarItem(
    val tip: StringResource,
    val caption: StringResource? = null,
    val icon: DrawableResource? = null,
    val click: (MdEditorViewModel) -> Unit = {},
)

data class MdToolbarCategory(
    val key: String,
    val tip: StringResource,
    val icon: DrawableResource? = null,
    val items: List<MdToolbarItem>,
)

val mdToolbarCategories = listOf(
    MdToolbarCategory(
        key = "style", tip = Res.string.style, icon = Res.drawable.match_case,
        items = listOf(
            MdToolbarItem(Res.string.md_bold, caption = Res.string.md_bold, icon = Res.drawable.format_bold, click = { it.toggleWrap("**") }),
            MdToolbarItem(Res.string.md_italic, caption = Res.string.md_italic, icon = Res.drawable.format_italic, click = { it.toggleWrap("*") }),
            MdToolbarItem(Res.string.md_underline, caption = Res.string.md_underline, icon = Res.drawable.format_underlined, click = { it.toggleWrap("<u>", "</u>") }),
            MdToolbarItem(Res.string.md_strikethrough, caption = Res.string.md_strike, icon = Res.drawable.strikethrough_s, click = { it.toggleWrap("~~") }),
            MdToolbarItem(Res.string.md_text_color, caption = Res.string.md_color, icon = Res.drawable.format_color_fill, click = { it.showColorPicker.value = true }),
            MdToolbarItem(Res.string.md_highlight, caption = Res.string.md_highlight, icon = Res.drawable.highlight, click = { it.toggleWrap("<mark>", "</mark>") }),
        ),
    ),
    MdToolbarCategory(
        key = "heading", tip = Res.string.md_heading, icon = Res.drawable.title,
        items = listOf(
            MdToolbarItem(Res.string.md_normal, caption = Res.string.md_normal, icon = Res.drawable.format_clear, click = { it.toggleLinePrefix("") }),
            MdToolbarItem(Res.string.md_heading_1, icon = Res.drawable.format_h1, click = { it.toggleLinePrefix("# ") }),
            MdToolbarItem(Res.string.md_heading_2, icon = Res.drawable.format_h2, click = { it.toggleLinePrefix("## ") }),
            MdToolbarItem(Res.string.md_heading_3, icon = Res.drawable.format_h3, click = { it.toggleLinePrefix("### ") }),
            MdToolbarItem(Res.string.md_heading_4, icon = Res.drawable.format_h4, click = { it.toggleLinePrefix("#### ") }),
            MdToolbarItem(Res.string.md_heading_5, icon = Res.drawable.format_h5, click = { it.toggleLinePrefix("##### ") }),
            MdToolbarItem(Res.string.md_heading_6, icon = Res.drawable.format_h6, click = { it.toggleLinePrefix("###### ") }),
        ),
    ),
    MdToolbarCategory(
        key = "list", tip = Res.string.md_list, icon = Res.drawable.format_list_bulleted,
        items = listOf(
            MdToolbarItem(Res.string.md_bulleted_list, caption = Res.string.md_bullet, icon = Res.drawable.format_list_bulleted, click = { it.toggleLinePrefix("- ") }),
            MdToolbarItem(Res.string.md_numbered_list, caption = Res.string.md_number, icon = Res.drawable.format_list_numbered, click = { it.toggleLinePrefix("1. ") }),
            MdToolbarItem(Res.string.md_task_list, caption = Res.string.md_task, icon = Res.drawable.checklist, click = { it.toggleLinePrefix("- [ ] ") }),
        ),
    ),
    MdToolbarCategory(
        key = "insert", tip = Res.string.md_insert, icon = Res.drawable.add,
        items = listOf(
            MdToolbarItem(Res.string.link, caption = Res.string.link, icon = Res.drawable.link, click = { it.insertAtFocused("[Link](", ")") }),
            MdToolbarItem(Res.string.image, caption = Res.string.image, icon = Res.drawable.image, click = { it.showInsertImage.value = true }),
            MdToolbarItem(
                Res.string.md_table,
                caption = Res.string.md_table,
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
            MdToolbarItem(Res.string.md_divider, caption = Res.string.md_divider, icon = Res.drawable.horizontal_rule, click = { it.insertText("\n---\n") }),
        ),
    ),
    MdToolbarCategory(
        key = "code", tip = Res.string.md_code, icon = Res.drawable.code,
        items = listOf(
            MdToolbarItem(Res.string.md_inline_code, caption = Res.string.md_inline, icon = Res.drawable.code, click = { it.toggleWrap("`") }),
            MdToolbarItem(Res.string.md_code_block, caption = Res.string.md_block, icon = Res.drawable.code_blocks, click = { it.insertAtFocused("```\n", "\n```") }),
        ),
    ),
    MdToolbarCategory(
        key = "math", tip = Res.string.md_math, icon = Res.drawable.functions,
        items = listOf(
            MdToolbarItem(Res.string.md_superscript, caption = Res.string.md_sup, icon = Res.drawable.superscript, click = { it.toggleWrap("^") }),
            MdToolbarItem(Res.string.md_subscript, caption = Res.string.md_sub, icon = Res.drawable.subscript, click = { it.toggleWrap("~") }),
            MdToolbarItem(Res.string.md_inline_math, caption = Res.string.md_inline, icon = Res.drawable.calculate, click = { it.toggleWrap("$") }),
            MdToolbarItem(Res.string.md_math_block, caption = Res.string.md_block, icon = Res.drawable.functions, click = { it.insertAtFocused("\$\$\n", "\n\$\$") }),
        ),
    ),
    MdToolbarCategory(
        key = "more", tip = Res.string.more, icon = Res.drawable.more_horiz,
        items = listOf(
            MdToolbarItem(Res.string.md_quote, caption = Res.string.md_quote, icon = Res.drawable.format_quote, click = { it.toggleLinePrefix("> ") }),
            MdToolbarItem(Res.string.md_callout, caption = Res.string.md_callout, icon = Res.drawable.info, click = { it.toggleLinePrefix("> [!note] ") }),
            MdToolbarItem(Res.string.md_footnote, caption = Res.string.md_footnote, icon = Res.drawable.bookmark, click = { it.insertAtFocused("[^1]: ", "") }),
            MdToolbarItem(Res.string.md_comment, caption = Res.string.md_comment, icon = Res.drawable.comment, click = { it.toggleWrap("%%") }),
            MdToolbarItem(
                Res.string.md_details,
                caption = Res.string.md_details,
                icon = Res.drawable.expand_more,
                click = { it.insertAtFocused("<details>\n<summary>Title</summary>\n", "\n</details>") },
            ),
            MdToolbarItem(Res.string.md_select_blocks, caption = Res.string.select, icon = Res.drawable.select_all, click = { it.enterSelectionMode() }),
        ),
    ),
)
