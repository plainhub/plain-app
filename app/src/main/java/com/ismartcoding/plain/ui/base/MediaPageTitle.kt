package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.models.CastViewModel

@Composable
internal fun getMediaPageTitle(
    mediaType: DataType,
    castVM: CastViewModel,
    bucket: DMediaBucket?,
    dragSelectState: DragSelectState,
    tag: MutableState<com.ismartcoding.plain.db.DTag?>,
    trash: MutableState<Boolean>
): String {
    val resourceId = when (mediaType) {
        DataType.IMAGE -> Res.string.images
        DataType.VIDEO -> Res.string.videos
        DataType.AUDIO -> Res.string.audios
        DataType.DOC -> Res.string.docs
        else -> Res.string.files
    }

    val mediaName = bucket?.name ?: stringResource(resourceId)
    return if (castVM.castMode.value) {
        stringResource(Res.string.cast_mode) + " - " + CastPlayer.currentDevice?.description?.device?.friendlyName
    } else if (dragSelectState.selectMode) {
        LocaleHelper.getStringSyncF(Res.string.x_selected, "count", dragSelectState.selectedIds.size)
    } else {
        mediaName
    }
}
