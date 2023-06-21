package com.ismartcoding.plain.ui.home.views

import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.HomeItemEducationBinding
import com.ismartcoding.plain.ui.endict.VocabulariesDialog
import com.ismartcoding.plain.ui.extensions.initTheme
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.extensions.showMore

fun HomeItemEducationBinding.initView() {
    title.setTextColor(title.context.getColor(R.color.primary))
    title.setText(R.string.home_item_title_education)
    memorizeWords
        .initTheme()
        .setKeyText(R.string.memorize_words)
        .showMore()
        .setClick {
            VocabulariesDialog().show()
        }
}