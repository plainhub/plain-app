package com.ismartcoding.plain.ui.feed

import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.enums.*
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.ExportFileResultEvent
import com.ismartcoding.plain.features.FeedStatusEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import com.ismartcoding.plain.ui.models.DrawerMenuItemClickedEvent
import com.ismartcoding.plain.ui.views.ClassicsHeader
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class FeedEntriesDialog : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.feeds_title

    override val dataType: DataType
        get() = DataType.FEED_ENTRY


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBottomBar(R.menu.action_feed_entries) {
            when (itemId) {
                R.id.delete -> {
                    val rv = binding.list.rv
                    rv.ensureSelect { items ->
                        DialogHelper.confirmToDelete(requireContext()) {
                            lifecycleScope.launch {
                                val ids = items.map { it.data.id }.toSet()
                                DialogHelper.showLoading()
                                withIO {
                                    TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
                                    FeedEntryHelper.feedEntryDao.delete(ids)
                                }
                                DialogHelper.hideLoading()
                                rv.bindingAdapter.checkedAll(false)
                                sendEvent(ActionEvent(ActionSourceType.FEED_ENTRY, ActionType.DELETED, ids))
                            }
                        }
                    }
                }
                else -> {
                    BottomMenuHelper.onMenuItemClick(viewModel, binding, this)
                }
            }
        }
    }

    override fun initEvents() {
        receiveEvent<ExportFileResultEvent> { event ->
            if (event.type == ExportFileType.OPML) {
                OutputStreamWriter(contentResolver.openOutputStream(event.uri)!!, Charsets.UTF_8).use { writer ->
                    withIO { FeedHelper.export(writer) }
                }
                contentResolver.query(event.uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val cache = mutableMapOf<String, Int>()
                        val fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME, cache)
                        DialogHelper.showConfirmDialog(requireContext(), "", LocaleHelper.getStringF(R.string.exported_to, "name", fileName))
                    }
                }
            }
        }
        receiveEvent<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.FEED) {
                return@receiveEvent
            }

            val uri = event.uris.first()
            InputStreamReader(contentResolver.openInputStream(uri)!!).use { reader ->
                withIO {
                    try {
                        FeedHelper.import(reader)
                    } catch (ex: Exception) {
                        DialogHelper.showMessage(ex.toString())
                    }
                }
                updateDrawerMenu()
                refreshList()
            }
        }

        receiveEvent<ActionEvent> { event ->
            if (setOf(ActionSourceType.FEED_ENTRY, ActionSourceType.FEED).contains(event.source)) {
                if (event.source == ActionSourceType.FEED && event.action == ActionType.UPDATED
                    && (viewModel.data as? DFeed)?.id == event.ids.first()
                ) {
                    viewModel.data = event.extra as IData
                }
                refreshList()
            }
        }

        receiveEvent<FeedStatusEvent> { event ->
            updateTitle()
            if (event.status == FeedWorkerStatus.COMPLETED) {
                refreshList()
                binding.list.page.finishRefresh(true)
            } else if (event.status == FeedWorkerStatus.ERROR) {
                binding.list.page.finishRefresh(false)
            }
        }
    }

    override fun initTopAppBar() {
        initTopAppBar(R.menu.feed_entries) {
        }
    }

    override fun initBasicEvents() {
        receiveEvent<DrawerMenuItemClickedEvent> { event ->
            val m = event.model
            viewModel.offset = 0
            viewModel.data = m.data as? IData
            binding.topAppBar.layout.setExpanded(true)
            binding.drawer.close()
            refreshList()
        }

        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.TAG_RELATION) {
                refreshList()
                updateDrawerMenu()
            } else if (setOf(ActionSourceType.TAG, ActionSourceType.FEED).contains(event.source)) {
                if (event.action == ActionType.DELETED && viewModel.data != null && event.ids.contains(viewModel.data!!.id)) {
                    viewModel.data = null
                }
                updateDrawerMenu()
                if (event.action != ActionType.CREATED) {
                    refreshList()
                }
            }
        }
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.FEEDS, DrawerMenuGroupType.TAGS)
    }

    override fun initDrawerMenu() {
        super.initDrawerMenu()
        binding.drawerContent.header.run {
            initMenu(R.menu.nv_feeds_header)
            onMenuItemClick {
                when (itemId) {
                    R.id.settings -> {
                        FeedSettingsDialog().show()
                    }
                }
            }
        }
    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<FeedEntryModel>(R.layout.item_feed_entry)

            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
                val m = getModel<FeedEntryModel>()
                FeedEntryDialog(m.data, m.feed).show()
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }

        binding.list.page.run {
            setRefreshHeader(ClassicsHeader(context, this).apply {
                pullText = { if (viewModel.data is DFeed) LocaleHelper.getString(R.string.pull_down_to_sync_current_feed) else LocaleHelper.getString(R.string.pull_down_to_sync_all_feeds) }
                refreshingText = { LocaleHelper.getString(R.string.syncing) }
                releaseText = { if (viewModel.data is DFeed) LocaleHelper.getString(R.string.release_to_sync_current_feed) else LocaleHelper.getString(R.string.release_to_sync_all_feeds) }
                finishText = { LocaleHelper.getString(R.string.synced) }
                failedText = { LocaleHelper.getString(R.string.sync_failed) }
            })

            setOnRefreshListener {
                viewModel.offset = 0
                if (viewModel.data == null) {
                    FeedFetchWorker.oneTimeRequest("")
                } else if (viewModel.data is DFeed) {
                    FeedFetchWorker.oneTimeRequest((viewModel.data as DFeed).id)
                }
            }

            setEnableLoadMore(true)
            onLoadMore {
                viewModel.offset += viewModel.limit
                updateDBList()
            }

        }

        refreshList()
    }

    override fun updateList() {
    }

    private fun refreshList() {
        binding.list.page.showLoading(refresh = false)
        binding.list.rv.models = null
        updateDBList()
        binding.list.page.showContent()
    }

    private fun updateDBList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { FeedEntryHelper.search(query, viewModel.limit, viewModel.offset) }
            viewModel.total = withIO { FeedEntryHelper.count(query) }
            val feeds = if (viewModel.data is DFeed) {
                val feed = viewModel.data as DFeed
                mapOf(feed.id to feed)
            } else withIO { FeedHelper.getAll().associateBy { it.id } }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<FeedEntryModel>()
            binding.list.page.addData(items.map { a ->
                FeedEntryModel(a, feeds[a.feedId]).apply {
                    image = a.image
                    title = a.title
                    subtitle = a.publishedAt.formatDateTime()
                    this.toggleMode = toggleMode
                    isChecked = checkedItems.any { it.data.id == data.id }
                }
            }, hasMore = {
                items.size == viewModel.limit
            })
            updateTitle()
        }
    }

    override fun updateTitle() {
        super.updateTitle()
        binding.topAppBar.notification.isVisible = false
        if (viewModel.data == null) {
            if (FeedFetchWorker.errorMap.any()) {
                binding.topAppBar.notification.isVisible = true
                binding.topAppBar.notification.text = FeedFetchWorker.errorMap.values.toList().joinToString("\n")
            }
        } else if (viewModel.data is DFeed) {
            val feed = viewModel.data as DFeed
            when (FeedFetchWorker.statusMap[feed.id]) {
                FeedWorkerStatus.ERROR -> {
                    binding.topAppBar.notification.isVisible = true
                    binding.topAppBar.notification.text = FeedFetchWorker.errorMap[feed.id] ?: ""
                }
                else -> {}
            }
        }
    }
}

