package com.ismartcoding.plain.ui.views.texteditor

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.helpers.WebHelper

class Accessory2View(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    private data class AccessoryItem(val icon: Int = 0, val before: String = "", val after: String = "")

    private val items =
        listOf(
            AccessoryItem(R.drawable.ic_undo),
            AccessoryItem(R.drawable.ic_redo),
            AccessoryItem(R.drawable.ic_paste),
            AccessoryItem(R.drawable.ic_format_bold, "**", "**"),
            AccessoryItem(R.drawable.ic_format_italic, "*", "*"),
            AccessoryItem(R.drawable.ic_format_underlined, "<u>", "</u>"),
            AccessoryItem(R.drawable.ic_format_strikethrough, "~~", "~~"),
            AccessoryItem(R.drawable.ic_code, "```\n", "\n```"),
            AccessoryItem(R.drawable.ic_superscript, "\$\$\n", "\n\$\$"),
            AccessoryItem(
                R.drawable.ic_table,
                """
| HEADER | HEADER | HEADER |
|:----:|:----:|:----:|
|      |      |      |
|      |      |      |
|      |      |      |
""",
            ),
            AccessoryItem(R.drawable.ic_check_box, "- [x] "),
            AccessoryItem(R.drawable.ic_check_box_blank, "- [ ] "),
            AccessoryItem(R.drawable.ic_link, "[Link](", ")"),
            AccessoryItem(R.drawable.ic_image),
            AccessoryItem(R.drawable.ic_format_color, "<font color=\"\">", "</font>"),
            AccessoryItem(R.drawable.ic_to_top),
            AccessoryItem(R.drawable.ic_to_bottom),
//        AccessoryItem(R.drawable.ic_find_replace),
            AccessoryItem(R.drawable.ic_help),
            AccessoryItem(R.drawable.ic_settings),
        )
    private val outValue = TypedValue()
    lateinit var getEditor: (() -> Editor)

    init {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        this.linear(HORIZONTAL).setup {
            addType<AccessoryItem>(R.layout.item_button)
            onBind {
                val button = itemView.findViewById<MaterialButton>(R.id.button)
                val m = getModel<AccessoryItem>()
                button.setBackgroundResource(outValue.resourceId)
                button.setIconResource(m.icon)
            }

            R.id.button.onFastClick {
                val m = getModel<AccessoryItem>()
                val editor = getEditor()
                when (m.icon) {
                    R.drawable.ic_undo -> {
                        editor.undo()
                    }
                    R.drawable.ic_redo -> {
                        editor.redo()
                    }
                    R.drawable.ic_paste -> {
                        editor.paste()
                    }
                    R.drawable.ic_image -> {
                        EditorInsertImageDialog().show()
                    }
                    R.drawable.ic_to_top -> {
                        editor.setSelection(0)
                    }
                    R.drawable.ic_to_bottom -> {
                        editor.text?.let { editor.setSelection(it.length) }
                    }
                    R.drawable.ic_settings -> {
                        EditorSettingsDialog().show()
                    }
                    R.drawable.ic_help -> {
                        WebHelper.open(context, "https://www.markdownguide.org/basic-syntax")
                    }
                    else -> {
                        editor.insert(m.before, m.after)
                    }
                }
            }
        }.models = items
    }
}
