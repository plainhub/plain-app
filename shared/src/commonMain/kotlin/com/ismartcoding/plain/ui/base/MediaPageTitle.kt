package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState

@Composable
internal fun getMediaPageTitle(
    mediaType: DataType,
    isCastMode: Boolean,
    castDeviceName: String?,
    bucket: DMediaBucket?,
    dragSelectState: DragSelectState,
): String {
    val resourceId = when (mediaType) {
        DataType.IMAGE -> Res.string.images
        DataType.VIDEO -> Res.string.videos
        DataType.AUDIO -> Res.string.audios
        DataType.DOC -> Res.string.docs
        else -> Res.string.files
    }

    val mediaName = bucket?.name ?: stringResource(resourceId)
    return if (isCastMode) {
        stringResource(Res.string.cast_mode) + " - " + castDeviceName
    } else if (dragSelectState.selectMode) {
        LocaleHelper.getStringF(Res.string.x_selected, "count", dragSelectState.selectedIds.size)
    } else {
        mediaName
    }
}
