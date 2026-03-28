package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
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
        DataType.IMAGE -> R.string.images
        DataType.VIDEO -> R.string.videos
        DataType.AUDIO -> R.string.audios
        else -> R.string.files
    }

    val mediaName = bucket?.name ?: stringResource(id = resourceId)
    return if (castVM.castMode.value) {
        stringResource(id = R.string.cast_mode) + " - " + CastPlayer.currentDevice?.description?.device?.friendlyName
    } else if (dragSelectState.selectMode) {
        LocaleHelper.getStringF(R.string.x_selected, "count", dragSelectState.selectedIds.size)
    } else if (tag.value != null) {
        mediaName + " - " + tag.value!!.name
    } else if (trash.value) {
        stringResource(id = resourceId) + " - " + stringResource(id = R.string.trash)
    } else {
        mediaName
    }
}
