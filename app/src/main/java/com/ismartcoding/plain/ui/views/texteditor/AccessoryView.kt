package com.ismartcoding.plain.ui.views.texteditor

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.plain.R

class AccessoryView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    private data class AccessoryItem(val text: String, val before: String, val after: String = "")

    private val items =
        listOf(
            AccessoryItem("*", "*"),
            AccessoryItem("_", "_"),
            AccessoryItem("`", "`"),
            AccessoryItem("#", "#"),
            AccessoryItem("-", "-"),
            AccessoryItem(">", ">"),
            AccessoryItem("<", "<"),
            AccessoryItem("/", "/"),
            AccessoryItem("\\", "\\"),
            AccessoryItem("|", "|"),
            AccessoryItem("!", "!"),
            AccessoryItem("[]", "[", "]"),
            AccessoryItem("()", "(", ")"),
            AccessoryItem("{}", "{", "}"),
            AccessoryItem("<>", "<", ">"),
            AccessoryItem("$", "$"),
            AccessoryItem("\"", "\""),
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
                button.text = m.text
                button.setBackgroundResource(outValue.resourceId)
            }

            R.id.button.onFastClick {
                val m = getModel<AccessoryItem>()
                val editor = getEditor()
                editor.insert(m.before, m.after)
            }
        }.models = items
    }
}
