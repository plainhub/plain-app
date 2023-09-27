package com.ismartcoding.plain.ui.feed

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.databinding.DialogFeedEntryBinding
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.feed.fetchContentAsync
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.PlainTextDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.markdown
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.extensions.setDoubleCLick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.tag.SelectTagsDialog
import com.ismartcoding.plain.ui.views.ClassicsHeader
import kotlinx.coroutines.launch

class FeedEntryDialog(private val feedEntry: DFeedEntry, val feed: DFeed?) : BaseDialog<DialogFeedEntryBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.toolbar.run {
            initMenu(R.menu.feed_entry_view)

            onBack {
                dismiss()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.web -> {
                        WebHelper.open(requireContext(), feedEntry.url)
                    }
                    R.id.add_to_tags -> {
                        SelectTagsDialog(DataType.FEED_ENTRY, arrayListOf(TagRelationStub.create(feedEntry))).show()
                    }
                    R.id.share -> {
                        ShareHelper.shareText(requireContext(), feedEntry.title.let { it + "\n" } + feedEntry.url)
                    }
                }
            }
        }
        binding.page.run {
            setRefreshHeader(
                ClassicsHeader(context, this).apply {
                    pullText = { getString(R.string.pull_down_to_fecth_content) }
                    refreshingText = { getString(R.string.fetching_content) }
                    releaseText = { getString(R.string.release_to_fetch) }
                    finishText = { getString(R.string.fetched) }
                    failedText = { getString(R.string.fetch_failed) }
                },
            )
            onRefresh {
                lifecycleScope.launch {
                    val r = withIO { feedEntry.fetchContentAsync() }
                    if (r.isOk()) {
                        finishRefresh()
                        updateContent()
                    } else {
                        finishRefresh(false)
                        DialogHelper.showErrorDialog(requireContext(), r.errorMessage())
                    }
                }
            }
        }

        updateContent()
    }

    private fun updateContent() {
        binding.title.text = feedEntry.title
        binding.subtitle.text =
            arrayOf(feed?.name ?: "", feedEntry.author, feedEntry.publishedAt.formatDateTime()).filter {
                it.isNotEmpty()
            }.joinToString(" | ")
        val content = feedEntry.content.ifEmpty { feedEntry.description }
        binding.title.setDoubleCLick {
            PlainTextDialog("HTML", content).show()
        }
        binding.markdown.markdown(content)
    }
}
