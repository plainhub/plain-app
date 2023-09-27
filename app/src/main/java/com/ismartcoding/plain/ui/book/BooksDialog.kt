package com.ismartcoding.plain.ui.book

import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.book.BookHelper
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class BooksDialog : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.books_title

    override val dataType: DataType
        get() = DataType.BOOK

    override fun initTopAppBar() {
        initTopAppBar(R.menu.books) {
            when (itemId) {
                R.id.delete -> {
                    val rv = binding.list.rv
                    rv.ensureSelect { items ->
                        lifecycleScope.launch {
                            val ids = items.map { it.data.id }.toSet()
                            withIO {
                                TagHelper.deleteTagRelationByKeys(ids, DataType.BOOK)
                                BookHelper.bookDao.delete(ids)
                            }
                            rv.bindingAdapter.checkedAll(false)
                            sendEvent(ActionEvent(ActionSourceType.BOOK, ActionType.DELETED, ids))
                        }
                    }
                }
            }
        }
    }

    override fun initEvents() {
        receiveEvent<PickFileResultEvent> { event ->
            if (event.tag != PickFileTag.BOOK) {
                return@receiveEvent
            }

            val uri = event.uris.first()
            InputStreamReader(contentResolver.openInputStream(uri)!!).use { reader ->
                updateDrawerMenu()
                binding.list.page.refresh()
            }
        }
    }

    override fun initDrawerMenu() {
        super.initDrawerMenu()
        binding.drawerContent.header.run {
            initMenu(R.menu.nv_books_header)
            onMenuItemClick {
                when (itemId) {
                    R.id.add -> {
                    }
                }
            }
        }
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TAGS)
    }

    override fun initList() {
        binding.list.rv.linear().setup {
            addType<BookModel>(R.layout.item_feed_entry)
            checkable(onItemClick = {
                val m = getModel<BookModel>()
                BookDialog(m.data).show()
            }, onChecked = { updateTitle() })
        }

        initRefreshLoadMore()
        binding.list.page.showLoading()
    }

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { BookHelper.search(query, viewModel.limit, viewModel.offset) }
            viewModel.total = withIO { BookHelper.count(query) }
            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<BookModel>()
            binding.list.page.addData(
                items.map { a ->
                    BookModel(a).apply {
                        image = a.image
                        title = a.name
                        subtitle = a.description
                        this.toggleMode = toggleMode
                        isChecked = checkedItems.any { it.data.id == data.id }
                    }
                },
                hasMore = {
                    items.size == viewModel.limit
                },
            )
            updateTitle()
        }
    }
}
