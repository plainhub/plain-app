package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.runtime.Composable
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.IconTextCastButton
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextOpenWithButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextRestoreButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.IconTextTrashButton
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel

@Composable
internal fun AudioActionButtons(
    m: DAudio,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    context: android.content.Context,
    onDismiss: () -> Unit,
    castVM: CastViewModel? = null,
) {
    ActionButtons {
        if (!audioVM.showSearchBar.value) {
            IconTextSelectButton {
                dragSelectState.enterSelectMode()
                dragSelectState.select(m.id)
                onDismiss()
            }
        }
        IconTextShareButton {
            ShareHelper.shareUris(context, listOf(AudioMediaStoreHelper.getItemUri(m.id)))
            onDismiss()
        }
        if (castVM != null && !m.path.isUrl()) {
            IconTextCastButton {
                castVM.showCastDialog.value = true
                onDismiss()
            }
        }
        if (!m.path.isUrl()) {
            IconTextOpenWithButton {
                ShareHelper.openPathWith(context, m.path)
            }
        }
        IconTextRenameButton {
            audioVM.showRenameDialog.value = true
        }
        if (AppFeatureType.MEDIA_TRASH.has()) {
            if (audioVM.trash.value) {
                IconTextRestoreButton {
                    audioVM.restore(context, tagsVM, setOf(m.id))
                    onDismiss()
                }
                IconTextDeleteButton {
                    DialogHelper.confirmToDelete {
                        audioVM.delete(context, tagsVM, setOf(m.id))
                        onDismiss()
                    }
                }
            } else {
                IconTextTrashButton {
                    audioVM.trash(context, tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
        } else {
            IconTextDeleteButton {
                DialogHelper.confirmToDelete {
                    audioVM.delete(context, tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
        }
    }
}
