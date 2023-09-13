package com.ismartcoding.plain.ui.note

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.data.preference.NoteEditModePreference
import com.ismartcoding.plain.databinding.ItemRowBinding
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.note.NoteHelper
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DataModel
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch

class NotesDialog : BaseListDrawerDialog() {
    override val titleId: Int
        get() = if (viewModel.trash.value == true) R.string.trash_title else R.string.notes_title

    override val dataType: DataType
        get() = DataType.NOTE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = binding.list.rv
        viewModel.trash.observe(viewLifecycleOwner) { trash ->
            binding.bottomAction.menu.run {
                findItem(R.id.add_to_tags)?.isVisible = trash == false
                findItem(R.id.remove_from_tag)?.isVisible = trash == false
                findItem(R.id.trash)?.isVisible = trash == false
                findItem(R.id.restore)?.isVisible = trash == true
                findItem(R.id.delete)?.isVisible = trash == true
            }
        }

        initBottomBar(R.menu.action_notes) {
            when (itemId) {
                R.id.trash -> {
                    rv.ensureSelect { items ->
                        lifecycleScope.launch {
                            val ids = items.map { it.data.id }.toSet()
                            DialogHelper.showLoading()
                            withIO {
                                TagHelper.deleteTagRelationByKeys(
                                    ids,
                                    DataType.NOTE
                                )
                                NoteHelper.trashAsync(ids)
                            }
                            DialogHelper.hideLoading()
                            rv.bindingAdapter.checkedAll(false)
                            sendEvent(ActionEvent(ActionSourceType.NOTE, ActionType.TRASHED, ids))
                        }
                    }
                }
                R.id.restore -> {
                    rv.ensureSelect { items ->
                        lifecycleScope.launch {
                            val ids = items.map { it.data.id }.toSet()
                            DialogHelper.showLoading()
                            withIO {
                                NoteHelper.untrashAsync(ids)
                            }
                            DialogHelper.hideLoading()
                            rv.bindingAdapter.checkedAll(false)
                            sendEvent(ActionEvent(ActionSourceType.NOTE, ActionType.RESTORED, ids))
                        }
                    }
                }
                R.id.delete -> {
                    rv.ensureSelect { items ->
                        DialogHelper.confirmToDelete(requireContext()) {
                            lifecycleScope.launch {
                                val ids = items.map { it.data.id }.toSet()
                                DialogHelper.showLoading()
                                withIO {
                                    NoteHelper.deleteAsync(ids)
                                }
                                DialogHelper.hideLoading()
                                rv.bindingAdapter.checkedAll(false)
                                sendEvent(ActionEvent(ActionSourceType.NOTE, ActionType.DELETED, ids))
                            }
                        }
                    }
                }
                else -> {
                    BottomMenuHelper.onMenuItemClick(viewModel, binding, this)
                }
            }
        }
        initFab()
    }

    override fun initEvents() {
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.NOTE) {
                binding.list.page.refresh()
            }
        }
    }

    override fun initTopAppBar() {
        initTopAppBar(R.menu.notes) {
        }
    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<DataModel>(R.layout.item_row)
            onCreate {
                val b = DataBindingUtil.bind<ItemRowBinding>(itemView)!!
                b.subtitle.maxLines = 2
            }

            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
                val m = getModel<DataModel>()
                NoteDialog().show(m.data as DNote)
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }

        initRefreshLoadMore()
        binding.list.page.showLoading()
    }

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { NoteHelper.search(query, viewModel.limit, viewModel.offset) }
            viewModel.total = withIO { NoteHelper.count(query) }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<DataModel>()
            binding.list.page.addData(items.map { a ->
                DataModel(a).apply {
                    keyText = a.updatedAt.formatDateTime()
                    subtitle = a.title
                    this.toggleMode = toggleMode
                    isChecked = checkedItems.any { it.data.id == data.id }
                }
            }, hasMore = {
                items.size == viewModel.limit
            })
            updateTitle()
        }
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TRASH, DrawerMenuGroupType.TAGS)
    }

    private fun initFab() {
        binding.fab.run {
            isVisible = viewModel.trash.value == false
            setImageResource(R.drawable.ic_add)
            setSafeClick {
                lifecycleScope.launch {
                    withIO { NoteEditModePreference.putAsync(requireContext(), true) }
                    NoteDialog().show(null, viewModel.data as? DTag)
                }
            }
        }
    }
}
