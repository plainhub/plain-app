package com.ismartcoding.plain.ui.home.views

import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.HomeItemWorkBinding
import com.ismartcoding.plain.ui.book.BooksDialog
import com.ismartcoding.plain.ui.extensions.initTheme
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.extensions.showMore
import com.ismartcoding.plain.ui.feed.FeedEntriesDialog
import com.ismartcoding.plain.ui.note.NotesDialog

fun HomeItemWorkBinding.initView() {
    title.setTextColor(title.context.getColor(R.color.primary))
    title.setText(R.string.home_item_title_work)
    notes
        .initTheme()
        .setKeyText(R.string.notes)
        .showMore()
        .setClick {
            NotesDialog().show()
        }

    books.initTheme()
        .setKeyText(R.string.books)
        .showMore()
        .setClick {
            BooksDialog().show()
        }

    feeds.initTheme()
        .setKeyText(R.string.feeds)
        .showMore()
        .setClick {
            FeedEntriesDialog().show()
        }
}