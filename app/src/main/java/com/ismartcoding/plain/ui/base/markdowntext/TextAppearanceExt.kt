package com.ismartcoding.plain.ui.base.markdowntext

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.view.doOnNextLayout
import androidx.core.widget.TextViewCompat
import com.ismartcoding.lib.isQPlus

fun TextView.applyFontWeight(fontWeight: FontWeight) {
    typeface = Typeface.create(typeface, fontWeight.weight, false)
}

fun TextView.applyFontStyle(fontStyle: FontStyle) {
    val type = when (fontStyle) {
        FontStyle.Italic -> Typeface.ITALIC
        FontStyle.Normal -> Typeface.NORMAL
        else -> Typeface.NORMAL
    }
    setTypeface(typeface, type)
}

fun TextView.applyTextColor(argbColor: Int) {
    setTextColor(argbColor)
}

fun TextView.applyFontSize(textStyle: TextStyle) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, textStyle.fontSize.value)
}

fun TextView.applyLineSpacing(textStyle: TextStyle) {
    setLineSpacing(textStyle.lineHeight.value, 1f)
}

fun TextView.applyTextDecoration(textStyle: TextStyle) {
    if (textStyle.textDecoration == TextDecoration.LineThrough) {
        paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
    }
}

fun TextView.applyLineHeight(textStyle: TextStyle) {
    if (textStyle.lineHeight.isSp) {
        TextViewCompat.setLineHeight(
            this,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textStyle.lineHeight.value,
                context.resources.displayMetrics
            ).toInt()
        )
    }
}

fun TextView.applyTextAlign(align: TextAlign) {
    textAlignment = when (align) {
        TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
        TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
        TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
        else -> View.TEXT_ALIGNMENT_TEXT_START
    }

    if (isQPlus() && align == TextAlign.Justify) {
        justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
    }
}

fun TextView.enableTextOverflow() {
    doOnNextLayout {
        if (maxLines != -1 && lineCount > maxLines) {
            val endOfLastLine = layout.getLineEnd(maxLines - 1)
            val spannedDropLast3Chars = text.subSequence(0, endOfLastLine - 3) as? Spanned
            if (spannedDropLast3Chars != null) {
                val spannableBuilder = SpannableStringBuilder()
                    .append(spannedDropLast3Chars)
                    .append("…")

                text = spannableBuilder
            }
        }
    }
}
