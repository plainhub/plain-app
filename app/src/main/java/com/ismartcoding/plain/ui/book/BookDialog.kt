package com.ismartcoding.plain.ui.book

import android.os.Bundle
import android.view.View
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.databinding.DialogFeedEntryBinding
import com.ismartcoding.plain.db.DBook
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.tag.SelectTagsDialog

class BookDialog(private val book: DBook) : BaseDialog<DialogFeedEntryBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.toolbar.run {
            initMenu(R.menu.feed_entry_view)

            onBack {
                dismiss()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.add_to_tags -> {
                        SelectTagsDialog(DataType.BOOK, arrayListOf(TagRelationStub.create(book))).show()
                    }
                }
            }
        }
//        binding.page.run {
//            onRefresh {
//                lifecycleScope.launch {
//                    val r = withIO { feedEntry.fetchContentAsync() }
//                    if (r.isOk()) {
//                        finishRefresh()
//                        updateContent()
//                    } else {
//                        finishRefresh(false)
//                        DialogHelper.showErrorDialog(requireContext(), r.errorMessage())
//                    }
//                }
//            }
//        }

        updateContent()
    }

    private fun updateContent() {
        binding.title.text = book.name
//        binding.subtitle.text = arrayOf(feed?.name ?: "", feedEntry.author, feedEntry.publishedAt.formatDateTime()).filter { it.isNotEmpty() }.joinToString(" | ")
//        val content = feedEntry.content.ifEmpty { feedEntry.description }
//        binding.title.setDoubleCLick {
//            PlainTextDialog("HTML", content).show()
//        }
//        binding.markdown.markdown(content)
    }
}