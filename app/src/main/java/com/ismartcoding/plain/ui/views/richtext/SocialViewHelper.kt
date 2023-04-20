package com.ismartcoding.plain.ui.views.richtext

import android.content.res.ColorStateList
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.CharacterStyle
import android.text.style.URLSpan
import android.widget.TextView
import androidx.core.util.PatternsCompat
import androidx.core.util.Supplier
import com.ismartcoding.plain.R
import java.util.regex.Pattern

class SocialViewHelper(val view: TextView) {
    private val initialMovementMethod: MovementMethod = view.movementMethod
    val hashtag: SuggestionView
        get() = SuggestionView(
            Pattern.compile("#(\\w+)"), false,
            view.context.getColorStateList(R.color.chip_color_state_list), view.context.getColor(R.color.red), arrayListOf(), null, null
        )
    val mention: SuggestionView
        get() = SuggestionView(
            Pattern.compile("@(\\w+)"), false,
            view.context.getColorStateList(R.color.chip_color_state_list),  view.context.getColor(R.color.red), arrayListOf(), null, null
        )
    val command: SuggestionView
        get() = SuggestionView(
            Pattern.compile(":(\\w+)"), false,
            view.context.getColorStateList(R.color.chip_color_state_list),  view.context.getColor(R.color.red), arrayListOf(), null, null
        )
    val hyperlink: SuggestionView
        get() = SuggestionView(
            PatternsCompat.WEB_URL, false,
            view.context.getColorStateList(R.color.chip_color_state_list),  view.context.getColor(R.color.red), arrayListOf(), null, null
        )

    //private var flags: Int
    private var commandEditing = false
    private var hashtagEditing = false
    private var mentionEditing = false
    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (count > 0 && start > 0) {
                val c = s[start - 1]
                when (c) {
                    ':' -> {
                        commandEditing = true
                        hashtagEditing = false
                        mentionEditing = false
                    }
                    '#' -> {
                        commandEditing = false
                        hashtagEditing = true
                        mentionEditing = false
                    }
                    '@' -> {
                        commandEditing = false
                        hashtagEditing = false
                        mentionEditing = true
                    }
                    else -> if (!Character.isLetterOrDigit(c)) {
                        commandEditing = false
                        hashtagEditing = false
                        mentionEditing = false
                    } else if (hashtag.textChanged != null && hashtagEditing) {
                        hashtag.textChanged?.invoke(
//                            this@SocialViewHelper, s.subSequence(
//                                indexOfPreviousNonLetterDigit(s, 0, start - 1) + 1, start
//                            )
                        )
                    } else if (mention.textChanged != null && mentionEditing) {
                        mention.textChanged?.invoke(
//                            this@SocialViewHelper, s.subSequence(
//                                indexOfPreviousNonLetterDigit(s, 0, start - 1) + 1, start
//                            )
                        )
                    }
                }
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // triggered when text is added
            if (s.isEmpty()) {
                return
            }
            //recolorize()
            if (start < s.length) {
                val index = start + count - 1
                if (index < 0) {
                    return
                }
                when (s[index]) {
                    ':' -> {
                        commandEditing = true
                        hashtagEditing = false
                        mentionEditing = false
                    }
                    '#' -> {
                        commandEditing = false
                        hashtagEditing = true
                        mentionEditing = false
                    }
                    '@' -> {
                        commandEditing = false
                        hashtagEditing = false
                        mentionEditing = true
                    }
                    else -> if (!Character.isLetterOrDigit(s[start])) {
                        commandEditing = false
                        hashtagEditing = false
                        mentionEditing = false
                    } else if (hashtag.textChanged != null && hashtagEditing) {
                        hashtag.textChanged?.invoke(
//                            this@SocialViewHelper, s.subSequence(
//                                indexOfPreviousNonLetterDigit(s, 0, start) + 1, start + count
//                            )
                        )
                    } else if (mention.textChanged != null && mentionEditing) {
                        mention.textChanged?.invoke(
//                            this@SocialViewHelper, s.subSequence(
//                                indexOfPreviousNonLetterDigit(s, 0, start) + 1, start + count
//                            )
                        )
                    }
                }
            }
        }

        override fun afterTextChanged(s: Editable) {}
    }

    private fun ensureMovementMethod(listener: Any?) {
        if (listener == null) {
            view.movementMethod = initialMovementMethod
        } else if (view.movementMethod !is LinkMovementMethod) {
            view.movementMethod = LinkMovementMethod.getInstance()
        }
    }

//    private fun recolorize() {
//        val text = view.text
//        check(text is Spannable) {
//            "Attached text is not a Spannable," +
//                    "add TextView.BufferType.SPANNABLE when setting text to this TextView."
//        }
//        val spannable = text
//        for (span in spannable.getSpans(0, text.length, CharacterStyle::class.java)) {
//            spannable.removeSpan(span)
//        }
//        if (isHashtagEnabled) {
//            spanAll(
//                spannable, hashtagPattern
//            ) { if (hashtagClickListener != null) SocialClickableSpan(hashtagClickListener!!, hashtagColors, false) else ForegroundColorSpan(hashtagColors.defaultColor) }
//        }
//        if (isMentionEnabled) {
//            spanAll(
//                spannable, mentionPattern
//            ) { if (mentionClickListener != null) SocialClickableSpan(mentionClickListener!!, mentionColors, false) else ForegroundColorSpan(mentionColors.defaultColor) }
//        }
//        if (isHyperlinkEnabled) {
//            spanAll(
//                spannable, hyperlinkPattern!!
//            ) { if (hyperlinkClickListener != null) SocialClickableSpan(hyperlinkClickListener!!, hyperlinkColors, true) else SocialURLSpan(text, hyperlinkColors) }
//        }
//    }

    /**
     * [CharacterStyle] that will be used for **hashtags**, **mentions**, and/or **hyperlinks**
     * when [com.ismartcoding.plain.ui.views.richtext.SocialView.OnClickListener] are activated.
     */
//    private class SocialClickableSpan(private val listener: SuggestionView.OnClickListener, colors: ColorStateList, isHyperlink: Boolean) : ClickableSpan() {
//        private val color: Int = colors.defaultColor
//        private val isHyperlink: Boolean
//        var text: CharSequence? = null
//        override fun onClick(widget: View) {
//            listener.onClick((widget as SocialView), (if (!isHyperlink) text!!.subSequence(1, text!!.length) else text)!!)
//        }
//
//        override fun updateDrawState(ds: TextPaint) {
//            ds.color = color
//            ds.isUnderlineText = isHyperlink
//        }
//
//        init {
//            this.isHyperlink = isHyperlink
//        }
//    }

    /**
     * Default [CharacterStyle] for **hyperlinks**.
     */
    private class SocialURLSpan(url: CharSequence, colors: ColorStateList) : URLSpan(url.toString()) {
        private val color: Int
        override fun updateDrawState(ds: TextPaint) {
            ds.color = color
            ds.isUnderlineText = true
        }

        init {
            color = colors.defaultColor
        }
    }

    companion object {
        private const val FLAG_HASHTAG = 1
        private const val FLAG_MENTION = 2
        private const val FLAG_HYPERLINK = 4

        private fun indexOfNextNonLetterDigit(text: CharSequence, start: Int): Int {
            for (i in start + 1 until text.length) {
                if (!Character.isLetterOrDigit(text[i])) {
                    return i
                }
            }
            return text.length
        }

        private fun indexOfPreviousNonLetterDigit(text: CharSequence, start: Int, end: Int): Int {
            for (i in end downTo start + 1) {
                if (!Character.isLetterOrDigit(text[i])) {
                    return i
                }
            }
            return start
        }

        private fun spanAll(spannable: Spannable, pattern: Pattern, styleSupplier: Supplier<CharacterStyle>) {
            val matcher = pattern.matcher(spannable)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                var span: Any = styleSupplier.get()
                spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//                if (span is SocialClickableSpan) {
//                    span.text = spannable.subSequence(start, end)
//                }
            }
        }

        private fun listOf(text: CharSequence, pattern: Pattern, isHyperlink: Boolean): List<String> {
            val list: MutableList<String> = ArrayList()
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                list.add(
                    matcher.group(
                        if (!isHyperlink) 1 // remove hashtag and mention symbol
                        else 0
                    )!!
                )
            }
            return list
        }
    }

    init {
        view.addTextChangedListener(textWatcher)
        view.setText(view.text, TextView.BufferType.SPANNABLE)
    }
}