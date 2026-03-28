package com.ismartcoding.plain.ui.page.chat.components

import android.content.ClipData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.models.delete
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.nav.navigateChatEditText

@Composable
fun ChatListItemContextMenu(
    navController: NavHostController,
    chatVM: ChatViewModel,
    m: VChat,
    showContextMenu: MutableState<Boolean>,
    onForward: (VChat) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    PDropdownMenu(
        expanded = showContextMenu.value && chatVM.selectedItem.value == m,
        onDismissRequest = {
            chatVM.selectedItem.value = null
            showContextMenu.value = false
        },
    ) {
        PDropdownMenuItem(
            text = { Text(stringResource(id = R.string.select)) },
            onClick = {
                chatVM.enterSelectMode()
                chatVM.select(m.id)
                chatVM.selectedItem.value = null
                showContextMenu.value = false
            },
        )
        PDropdownMenuItem(
            text = { Text(stringResource(id = R.string.forward)) },
            onClick = {
                chatVM.selectedItem.value = null
                showContextMenu.value = false
                onForward(m)
            },
        )
        if (m.value is DMessageText) {
            PDropdownMenuItem(
                text = { Text(stringResource(id = R.string.copy_text)) },
                onClick = {
                    chatVM.selectedItem.value = null
                    showContextMenu.value = false
                    val text = (m.value as DMessageText).text
                    val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.message), text)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(text)
                },
            )
            if (m.fromId == "me") {
                PDropdownMenuItem(
                    text = { Text(stringResource(id = R.string.edit_text)) },
                    onClick = {
                        chatVM.selectedItem.value = null
                        showContextMenu.value = false
                        val content = (m.value as DMessageText).text
                        navController.navigateChatEditText(m.id, content)
                    },
                )
            }
        }
        PDropdownMenuItem(
            text = { Text(stringResource(id = R.string.delete)) },
            onClick = {
                chatVM.selectedItem.value = null
                showContextMenu.value = false
                chatVM.delete(context, setOf(m.id))
            },
        )
    }
}
