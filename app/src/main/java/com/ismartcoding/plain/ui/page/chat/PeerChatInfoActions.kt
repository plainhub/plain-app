package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper

internal fun LazyListScope.clearMessagesItem(
    clearMessagesText: String,
    clearMessagesConfirmText: String,
    cancelText: String,
    onClear: () -> Unit,
) {
    item {
        POutlinedButton(
            text = clearMessagesText,
            type = ButtonType.DANGER,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            onClick = {
                DialogHelper.showConfirmDialog(
                    title = clearMessagesText,
                    message = clearMessagesConfirmText,
                    confirmButton = Pair(clearMessagesText) { onClear() },
                    dismissButton = Pair(cancelText) {},
                )
            },
        )
    }
}

internal fun LazyListScope.deleteDeviceItem(
    deleteDeviceText: String,
    deleteText: String,
    deleteDeviceWarningText: String,
    cancelText: String,
    onDelete: () -> Unit,
) {
    item {
        VerticalSpace(dp = 16.dp)
        POutlinedButton(
            text = deleteDeviceText,
            type = ButtonType.DANGER,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            onClick = {
                DialogHelper.showConfirmDialog(
                    title = deleteDeviceText,
                    message = deleteDeviceWarningText,
                    confirmButton = Pair(deleteText) { onDelete() },
                    dismissButton = Pair(cancelText) {},
                )
            },
        )
    }
}
