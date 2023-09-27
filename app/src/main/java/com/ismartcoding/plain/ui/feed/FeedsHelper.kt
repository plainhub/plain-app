package com.ismartcoding.plain.ui.feed

import androidx.appcompat.widget.PopupMenu
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.onItemClick
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ExportFileType
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.extensions.formatName
import com.ismartcoding.plain.features.ExportFileEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.models.DrawerMenuGroup
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel
import com.ismartcoding.plain.ui.models.MenuItemModel
import java.util.*

object FeedsHelper {
    suspend fun createMenuGroupAsync(viewModel: FilteredItemsViewModel): DrawerMenuGroup {
        val items =
            FeedHelper.getAll().map { feed ->
                MenuItemModel(feed).apply {
                    isChecked = (viewModel.data as? DFeed)?.id == feed.id
                    title = feed.name
                    iconId = R.drawable.ic_rss_feed
                    this.count = 0 // TODO
                }
            }

        return DrawerMenuGroup(LocaleHelper.getString(R.string.subscriptions)).apply {
            itemSublist = items
            iconClick = { v ->
                PopupMenu(v.context, v).apply {
                    inflate(R.menu.feeds)
                    onItemClick {
                        when (itemId) {
                            R.id.add -> {
                                AddFeedDialog().show()
                            }
                            R.id.import_opml -> {
                                sendEvent(PickFileEvent(PickFileTag.FEED, PickFileType.FILE, false))
                            }
                            R.id.export -> {
                                sendEvent(ExportFileEvent(ExportFileType.OPML, "feeds_" + Date().formatName() + ".opml"))
                            }
                        }
                    }
                }.show()
            }
        }
    }
}
