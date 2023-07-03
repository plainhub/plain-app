package com.ismartcoding.plain.ui.note

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.extensions.parcelable
import com.ismartcoding.lib.extensions.px
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.databinding.DialogNoteBinding
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.note.NoteHelper
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.*
import kotlinx.coroutines.launch

class NoteDialog() : BaseDialog<DialogNoteBinding>() {
    private val ARG_NOTE = "note"
    private val ARG_TAG = "tag"

    private var note: DNote? = null
    private var tag: DTag? = null
    private var id: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        note = arguments?.parcelable(ARG_NOTE)
        tag = arguments?.parcelable(ARG_TAG)
        id = note?.id ?: ""

        binding.topAppBar.toolbar.run {
            initMenu(R.menu.note_edit)

            onBack {
                dismiss()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.preview -> {
                        LocalStorage.noteIsEditMode = !LocalStorage.noteIsEditMode
                        updateModeUI()
                    }
                }
            }
        }
        val context = requireContext()

        binding.markdown.updatePadding(bottom = context.px(R.dimen.list_bottom_padding))
        setWindowSoftInput(binding.editor)
        binding.editor.initView(lifecycle, note?.content ?: "")
        binding.editor.onTextChanged = {
            lifecycleScope.launch {
                val isNew = id.isEmpty()
                withIO {
                    id = NoteHelper.addOrUpdateAsync(id) {
                        val text = binding.editor.getText()
                        title = text.cut(100).replace("\n", "")
                        content = text
                    }
                    if (isNew && tag != null) {
                        // create note from tag items page.
                        TagHelper.addTagRelations(arrayListOf(TagRelationStub(id).toTagRelation(tag!!.id, TagType.NOTE)))
                        sendEvent(ActionEvent(ActionSourceType.TAG_RELATION, ActionType.DELETED, setOf(id)))
                    }
                }
                binding.editor.resetChangedState()
                sendEvent(ActionEvent(ActionSourceType.NOTE, if (isNew) ActionType.CREATED else ActionType.UPDATED, setOf(id)))
            }
        }
        updateModeUI()
    }

    private fun updateModeUI() {
        val context = requireContext()
        if (LocalStorage.noteIsEditMode) {
            binding.topAppBar.apply {
                toolbar.setTitle(R.string.edit_mode)
                toolbar.menu.findItem(R.id.preview).icon = ContextCompat.getDrawable(context, R.drawable.ic_markdown)
                setScrollBehavior(false)
            }
            binding.editor.isVisible = true
            binding.markdownContainer.isVisible = false
        } else {
            binding.topAppBar.apply {
                toolbar.setTitle(R.string.read_mode)
                toolbar.menu.findItem(R.id.preview).icon = ContextCompat.getDrawable(context, R.drawable.ic_edit)
                setScrollBehavior(true)
            }
            binding.editor.isVisible = false
            binding.markdownContainer.isVisible = true
            binding.markdown.markdown(binding.editor.getText())
            binding.editor.hideSoftInput()
        }
    }

    fun show(note: DNote?, tag: DTag? = null) {
        arguments = Bundle().apply {
            putParcelable(ARG_NOTE, note)
            putParcelable(ARG_TAG, tag)
        }
        show()
    }
}