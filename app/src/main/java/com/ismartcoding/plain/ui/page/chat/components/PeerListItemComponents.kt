package com.ismartcoding.plain.ui.page.chat.components

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey

@Composable
internal fun PeerIconWithStatus(
    icon: DrawableResource,
    title: String,
    online: Boolean?,
) {
    Box(
        modifier = Modifier
            .size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        if (online != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = (-10).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(
                        if (online) MaterialTheme.colorScheme.green
                        else MaterialTheme.colorScheme.grey
                    )
            )
        }
    }
}

@Composable
internal fun PeerContextMenu(
    peerId: String,
    showContextMenu: MutableState<Boolean>,
    deleteDeviceText: String,
    deleteText: String,
    deleteWarningText: String,
    cancelText: String,
    onDelete: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .wrapContentSize(Alignment.Center),
    ) {
        PDropdownMenu(
            expanded = showContextMenu.value,
            onDismissRequest = { showContextMenu.value = false },
        ) {
            PDropdownMenuItem(
                text = { Text(deleteDeviceText) },
                onClick = {
                    showContextMenu.value = false
                    DialogHelper.showConfirmDialog(
                        title = deleteDeviceText,
                        message = deleteWarningText,
                        confirmButton = Pair(deleteText) { onDelete(peerId) },
                        dismissButton = Pair(cancelText) {}
                    )
                },
            )
        }
    }
}
