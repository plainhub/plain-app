package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.add as ui_drawable_add
import com.ismartcoding.plain.ui.resources.bookmark as ui_drawable_bookmark
import com.ismartcoding.plain.ui.resources.calculate as ui_drawable_calculate
import com.ismartcoding.plain.ui.resources.checklist as ui_drawable_checklist
import com.ismartcoding.plain.ui.resources.code as ui_drawable_code
import com.ismartcoding.plain.ui.resources.code_blocks as ui_drawable_code_blocks
import com.ismartcoding.plain.ui.resources.comment as ui_drawable_comment
import com.ismartcoding.plain.ui.resources.expand_more as ui_drawable_expand_more
import com.ismartcoding.plain.ui.resources.format_bold as ui_drawable_format_bold
import com.ismartcoding.plain.ui.resources.format_clear as ui_drawable_format_clear
import com.ismartcoding.plain.ui.resources.format_color_fill as ui_drawable_format_color_fill
import com.ismartcoding.plain.ui.resources.format_h1 as ui_drawable_format_h1
import com.ismartcoding.plain.ui.resources.format_h2 as ui_drawable_format_h2
import com.ismartcoding.plain.ui.resources.format_h3 as ui_drawable_format_h3
import com.ismartcoding.plain.ui.resources.format_h4 as ui_drawable_format_h4
import com.ismartcoding.plain.ui.resources.format_h5 as ui_drawable_format_h5
import com.ismartcoding.plain.ui.resources.format_h6 as ui_drawable_format_h6
import com.ismartcoding.plain.ui.resources.format_italic as ui_drawable_format_italic
import com.ismartcoding.plain.ui.resources.format_list_bulleted as ui_drawable_format_list_bulleted
import com.ismartcoding.plain.ui.resources.format_list_numbered as ui_drawable_format_list_numbered
import com.ismartcoding.plain.ui.resources.format_quote as ui_drawable_format_quote
import com.ismartcoding.plain.ui.resources.format_underlined as ui_drawable_format_underlined
import com.ismartcoding.plain.ui.resources.functions as ui_drawable_functions
import com.ismartcoding.plain.ui.resources.highlight as ui_drawable_highlight
import com.ismartcoding.plain.ui.resources.horizontal_rule as ui_drawable_horizontal_rule
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.info as ui_drawable_info
import com.ismartcoding.plain.ui.resources.link as ui_drawable_link
import com.ismartcoding.plain.ui.resources.match_case as ui_drawable_match_case
import com.ismartcoding.plain.ui.resources.more_horiz as ui_drawable_more_horiz
import com.ismartcoding.plain.ui.resources.select_all as ui_drawable_select_all
import com.ismartcoding.plain.ui.resources.strikethrough_s as ui_drawable_strikethrough_s
import com.ismartcoding.plain.ui.resources.subscript as ui_drawable_subscript
import com.ismartcoding.plain.ui.resources.superscript as ui_drawable_superscript
import com.ismartcoding.plain.ui.resources.table as ui_drawable_table
import com.ismartcoding.plain.ui.resources.title as ui_drawable_title
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.select_all
import com.ismartcoding.plain.i18n.title
import com.ismartcoding.plain.i18n.add
import com.ismartcoding.plain.i18n.link

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
        key = "style", tip = Res.string.style, icon = UiRes.drawable.ui_drawable_match_case,
        items = listOf(
            MdToolbarItem(Res.string.md_bold, caption = Res.string.md_bold, icon = UiRes.drawable.ui_drawable_format_bold, click = { it.toggleWrap("**") }),
            MdToolbarItem(Res.string.md_italic, caption = Res.string.md_italic, icon = UiRes.drawable.ui_drawable_format_italic, click = { it.toggleWrap("*") }),
            MdToolbarItem(Res.string.md_underline, caption = Res.string.md_underline, icon = UiRes.drawable.ui_drawable_format_underlined, click = { it.toggleWrap("<u>", "</u>") }),
            MdToolbarItem(Res.string.md_strikethrough, caption = Res.string.md_strike, icon = UiRes.drawable.ui_drawable_strikethrough_s, click = { it.toggleWrap("~~") }),
            MdToolbarItem(Res.string.md_text_color, caption = Res.string.md_color, icon = UiRes.drawable.ui_drawable_format_color_fill, click = { it.showColorPicker.value = true }),
            MdToolbarItem(Res.string.md_highlight, caption = Res.string.md_highlight, icon = UiRes.drawable.ui_drawable_highlight, click = { it.toggleWrap("<mark>", "</mark>") }),
        ),
    ),
    MdToolbarCategory(
        key = "heading", tip = Res.string.md_heading, icon = UiRes.drawable.ui_drawable_title,
        items = listOf(
            MdToolbarItem(Res.string.md_normal, caption = Res.string.md_normal, icon = UiRes.drawable.ui_drawable_format_clear, click = { it.toggleLinePrefix("") }),
            MdToolbarItem(Res.string.md_heading_1, icon = UiRes.drawable.ui_drawable_format_h1, click = { it.toggleLinePrefix("# ") }),
            MdToolbarItem(Res.string.md_heading_2, icon = UiRes.drawable.ui_drawable_format_h2, click = { it.toggleLinePrefix("## ") }),
            MdToolbarItem(Res.string.md_heading_3, icon = UiRes.drawable.ui_drawable_format_h3, click = { it.toggleLinePrefix("### ") }),
            MdToolbarItem(Res.string.md_heading_4, icon = UiRes.drawable.ui_drawable_format_h4, click = { it.toggleLinePrefix("#### ") }),
            MdToolbarItem(Res.string.md_heading_5, icon = UiRes.drawable.ui_drawable_format_h5, click = { it.toggleLinePrefix("##### ") }),
            MdToolbarItem(Res.string.md_heading_6, icon = UiRes.drawable.ui_drawable_format_h6, click = { it.toggleLinePrefix("###### ") }),
        ),
    ),
    MdToolbarCategory(
        key = "list", tip = Res.string.md_list, icon = UiRes.drawable.ui_drawable_format_list_bulleted,
        items = listOf(
            MdToolbarItem(Res.string.md_bulleted_list, caption = Res.string.md_bullet, icon = UiRes.drawable.ui_drawable_format_list_bulleted, click = { it.toggleLinePrefix("- ") }),
            MdToolbarItem(Res.string.md_numbered_list, caption = Res.string.md_number, icon = UiRes.drawable.ui_drawable_format_list_numbered, click = { it.toggleLinePrefix("1. ") }),
            MdToolbarItem(Res.string.md_task_list, caption = Res.string.md_task, icon = UiRes.drawable.ui_drawable_checklist, click = { it.toggleLinePrefix("- [ ] ") }),
        ),
    ),
    MdToolbarCategory(
        key = "insert", tip = Res.string.md_insert, icon = UiRes.drawable.ui_drawable_add,
        items = listOf(
            MdToolbarItem(Res.string.link, caption = Res.string.link, icon = UiRes.drawable.ui_drawable_link, click = { it.insertAtFocused("[Link](", ")") }),
            MdToolbarItem(Res.string.image, caption = Res.string.image, icon = UiRes.drawable.ui_drawable_image, click = { it.showInsertImage.value = true }),
            MdToolbarItem(
                Res.string.md_table,
                caption = Res.string.md_table,
                icon = UiRes.drawable.ui_drawable_table,
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
            MdToolbarItem(Res.string.md_divider, caption = Res.string.md_divider, icon = UiRes.drawable.ui_drawable_horizontal_rule, click = { it.insertText("\n---\n") }),
        ),
    ),
    MdToolbarCategory(
        key = "code", tip = Res.string.md_code, icon = UiRes.drawable.ui_drawable_code,
        items = listOf(
            MdToolbarItem(Res.string.md_inline_code, caption = Res.string.md_inline, icon = UiRes.drawable.ui_drawable_code, click = { it.toggleWrap("`") }),
            MdToolbarItem(Res.string.md_code_block, caption = Res.string.md_block, icon = UiRes.drawable.ui_drawable_code_blocks, click = { it.insertAtFocused("```\n", "\n```") }),
        ),
    ),
    MdToolbarCategory(
        key = "math", tip = Res.string.md_math, icon = UiRes.drawable.ui_drawable_functions,
        items = listOf(
            MdToolbarItem(Res.string.md_superscript, caption = Res.string.md_sup, icon = UiRes.drawable.ui_drawable_superscript, click = { it.toggleWrap("^") }),
            MdToolbarItem(Res.string.md_subscript, caption = Res.string.md_sub, icon = UiRes.drawable.ui_drawable_subscript, click = { it.toggleWrap("~") }),
            MdToolbarItem(Res.string.md_inline_math, caption = Res.string.md_inline, icon = UiRes.drawable.ui_drawable_calculate, click = { it.toggleWrap("$") }),
            MdToolbarItem(Res.string.md_math_block, caption = Res.string.md_block, icon = UiRes.drawable.ui_drawable_functions, click = { it.insertAtFocused("\$\$\n", "\n\$\$") }),
        ),
    ),
    MdToolbarCategory(
        key = "more", tip = Res.string.more, icon = UiRes.drawable.ui_drawable_more_horiz,
        items = listOf(
            MdToolbarItem(Res.string.md_quote, caption = Res.string.md_quote, icon = UiRes.drawable.ui_drawable_format_quote, click = { it.toggleLinePrefix("> ") }),
            MdToolbarItem(Res.string.md_callout, caption = Res.string.md_callout, icon = UiRes.drawable.ui_drawable_info, click = { it.toggleLinePrefix("> [!note] ") }),
            MdToolbarItem(Res.string.md_footnote, caption = Res.string.md_footnote, icon = UiRes.drawable.ui_drawable_bookmark, click = { it.insertAtFocused("[^1]: ", "") }),
            MdToolbarItem(Res.string.md_comment, caption = Res.string.md_comment, icon = UiRes.drawable.ui_drawable_comment, click = { it.toggleWrap("%%") }),
            MdToolbarItem(
                Res.string.md_details,
                caption = Res.string.md_details,
                icon = UiRes.drawable.ui_drawable_expand_more,
                click = { it.insertAtFocused("<details>\n<summary>Title</summary>\n", "\n</details>") },
            ),
            MdToolbarItem(Res.string.md_select_blocks, caption = Res.string.select, icon = UiRes.drawable.ui_drawable_select_all, click = { it.enterSelectionMode() }),
        ),
    ),
)
