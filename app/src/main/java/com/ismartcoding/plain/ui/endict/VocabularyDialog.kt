package com.ismartcoding.plain.ui.endict

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.px
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.SearchEndictByWordsQuery
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.databinding.DialogVocabularyBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.db.DVocabulary
import com.ismartcoding.plain.features.VocabularyWordsDeletedEvent
import com.ismartcoding.plain.features.VocabularyWordsUpdatedEvent
import com.ismartcoding.plain.features.vocabulary.VocabularyList
import com.ismartcoding.plain.fragment.EndictItemFragment
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class VocabularyDialog(val vocabulary: DVocabulary) : BaseDialog<DialogVocabularyBinding>() {
    private var offset = 0
    private val limit: Int = 20
    private val selectedWords = mutableSetOf<String>()

    private enum class PopupMenuItemType {
        SELECT,
        SORT_RANDOM,
        TOGGLE_WORD,
        TOGGLE_TRANSLATION,
    }

    private fun addMenuItem(
        menu: Menu,
        type: PopupMenuItemType,
        titleRes: Int,
    ) {
        menu.add(0, type.ordinal, type.ordinal, titleRes)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.run {
            initMenu(R.menu.vocabulary)
            title = vocabulary.name
            onBack {
                dismiss()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.more -> {
                        val popup = PopupMenu(requireContext(), binding.topAppBar.findViewById(R.id.more))
                        val popupMenu = popup.menu
                        addMenuItem(popupMenu, PopupMenuItemType.SELECT, R.string.select)
                        addMenuItem(popupMenu, PopupMenuItemType.SORT_RANDOM, R.string.sort_by_random)
                        addMenuItem(
                            popupMenu,
                            PopupMenuItemType.TOGGLE_WORD,
                            if (TempData.endictShowWord) R.string.hide_word else R.string.show_word,
                        )
                        addMenuItem(
                            popupMenu,
                            PopupMenuItemType.TOGGLE_TRANSLATION,
                            if (TempData.endictShowTranslation) R.string.hide_translation else R.string.show_translation,
                        )
                        popup.setOnMenuItemClickListener {
                            when (it.itemId) {
                                PopupMenuItemType.SELECT.ordinal -> {
                                    isVisible = false
                                    menu.findItem(R.id.cancel).isVisible = true
                                    binding.selectMenu.visibility = View.VISIBLE
                                    binding.list.rv.bindingAdapter.toggle()
                                }
                                PopupMenuItemType.SORT_RANDOM.ordinal -> {
                                    lifecycleScope.launch {
                                        vocabulary.words.shuffle()
                                        withIO {
                                            VocabularyList.addOrUpdateAsync(vocabulary.id) {
                                                words = vocabulary.words
                                            }
                                        }
                                        binding.list.page.showLoading()
                                    }
                                }
                                PopupMenuItemType.TOGGLE_WORD.ordinal -> {
                                    TempData.endictShowWord = !TempData.endictShowWord
                                    popupMenu.removeItem(PopupMenuItemType.TOGGLE_WORD.ordinal)
                                    addMenuItem(
                                        popupMenu,
                                        PopupMenuItemType.TOGGLE_WORD,
                                        if (TempData.endictShowWord) R.string.hide_word else R.string.show_word,
                                    )
                                    binding.list.rv.bindingAdapter.notifyDataSetChanged()
                                }
                                PopupMenuItemType.TOGGLE_TRANSLATION.ordinal -> {
                                    popupMenu.removeItem(PopupMenuItemType.TOGGLE_TRANSLATION.ordinal)
                                    addMenuItem(
                                        popupMenu,
                                        PopupMenuItemType.TOGGLE_TRANSLATION,
                                        if (TempData.endictShowTranslation) R.string.hide_translation else R.string.show_translation,
                                    )
                                    TempData.endictShowTranslation = !TempData.endictShowTranslation
                                    binding.list.rv.bindingAdapter.notifyDataSetChanged()
                                }
                            }
                            true
                        }
                        popup.show()
                    }
                    R.id.cancel -> {
                        binding.list.rv.bindingAdapter.toggle()
                        menu.findItem(R.id.more).isVisible = true
                        isVisible = false
                    }
                }
            }
        }

        binding.list.rv.linear().setup {
            addType<EndictItemFragment>(R.layout.view_list_item)
            onBind {
                val bindingItem = getBinding<ViewListItemBinding>()
                val m = getModel<EndictItemFragment>()
                bindingItem.setKeyText(m.word)
                bindingItem.clearTextRows()
                bindingItem.addTextRow(m.translation.joinToString("\n"))

                bindingItem.textKey.visibility =
                    if (TempData.endictShowWord) {
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }

                bindingItem.rows.visibility =
                    if (TempData.endictShowTranslation) {
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }

                if (toggleMode) {
                    bindingItem.cb.isChecked = selectedWords.contains(m.word)
                    bindingItem.cbContainer.visibility = View.VISIBLE
                    bindingItem.setClick {
                        setChecked(bindingAdapterPosition, !selectedWords.contains(m.word))
                    }
                } else {
                    bindingItem.cbContainer.visibility = View.GONE
                    bindingItem.enableSwipeMenu(true)
                    bindingItem.setRightSwipeButton(getString(R.string.delete)) {
                        DialogHelper.confirmToAction(requireContext(), R.string.confirm_to_delete) {
                            lifecycleScope.launch {
                                withIO {
                                    VocabularyList.addOrUpdateAsync(vocabulary.id) {
                                        words.remove(m.word)
                                    }
                                }
                                vocabulary.words.remove(m.word)
                                binding.list.rv.removeModel(m, modelPosition)
                                sendEvent(VocabularyWordsDeletedEvent(vocabulary.id))
                            }
                        }
                    }
                    bindingItem.setClick {
                        WordDialog(m).show()
                    }
                }
            }

            onClick(R.id.cb) {
                val m = getModel<EndictItemFragment>()
                setChecked(bindingAdapterPosition, !selectedWords.contains(m.word))
            }

            onChecked { position, _, _ ->
                val m = getModel<EndictItemFragment>(position)
                val isSelected = selectedWords.contains(m.word)
                if (isSelected) {
                    selectedWords.remove(m.word)
                } else {
                    selectedWords.add(m.word)
                }
                notifyItemChanged(position)
            }

            onToggle { position, toggleMode, end ->
                notifyItemChanged(position)
                if (end) {
                    binding.selectMenu.visibility = if (toggleMode) View.VISIBLE else View.GONE
                    if (toggleMode) {
                        binding.list.rv.setPadding(0, 0, 0, requireContext().px(R.dimen.size_hhl).toInt())
                    } else {
                        binding.list.rv.setPadding(0, 0, 0, requireContext().px(R.dimen.size_xl).toInt())
                        checkedAll(false)
                    }
                }
            }
        }

        binding.list.page.onRefresh {
            offset = 0
            updateList()
        }.showLoading()

        binding.list.page.setEnableLoadMore(true)
        binding.list.page.onLoadMore {
            offset += limit
            updateList()
        }

        binding.unselect.setSafeClick {
            binding.list.rv.bindingAdapter.checkedAll(false)
        }

        binding.delete.setSafeClick {
            if (selectedWords.isEmpty()) {
                DialogHelper.showMessage(R.string.select_first)
                return@setSafeClick
            }
            DialogHelper.confirmToAction(requireContext(), R.string.confirm_to_delete) {
                lifecycleScope.launch {
                    withIO {
                        VocabularyList.addOrUpdateAsync(vocabulary.id) {
                            words.removeAll(selectedWords)
                        }
                    }
                    vocabulary.words.removeAll(selectedWords)
                    binding.list.rv.mutable.removeAll { selectedWords.contains((it as EndictItemFragment).word) }
                    selectedWords.clear()
                    binding.list.rv.bindingAdapter.run {
                        checkedPosition.clear()
                        notifyDataSetChanged()
                    }
                    sendEvent(VocabularyWordsDeletedEvent(vocabulary.id))
                }
            }
        }

        binding.add.setSafeClick {
            if (selectedWords.isEmpty()) {
                DialogHelper.showMessage(R.string.select_first)
                return@setSafeClick
            }
            SelectVocabularyDialog(setOf(vocabulary.id)) { v ->
                lifecycleScope.launch {
                    withIO {
                        VocabularyList.addOrUpdateAsync(v.id) {
                            words.addAll(0, selectedWords)
                        }
                    }
                    DialogHelper.showMessage(R.string.added)
                    sendEvent(VocabularyWordsUpdatedEvent(vocabulary.id))
                }
            }.show()
        }
    }

    private fun updateList() {
        lifecycleScope.launch {
            val endIndex = offset + limit
            val total = vocabulary.words.size
            val words = vocabulary.words.toList().subList(offset, if (endIndex > total) total else endIndex)
            val r =
                withIO {
                    BoxApi.mixQueryAsync(SearchEndictByWordsQuery(words))
                }

            if (!r.isSuccess()) {
                binding.list.page.run {
                    if (offset == 0) {
                        finishRefresh(false)
                        showError(true)
                    } else {
                        finishLoadMore(false)
                    }
                }
                DialogHelper.showMessage(r)
                return@launch
            }

            r.response?.data?.let { data ->
                binding.list.page.addData(
                    data.searchEndictByWords.map {
                        it.endictItemFragment
                    }.sortedBy { words.indexOf(it.word) },
                    isEmpty = { total == 0 },
                ) {
                    endIndex < total
                }
            }
        }
    }
}
