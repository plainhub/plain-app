package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.nav.navigateChatEditText

@Composable
fun ChatListItemContextMenu(
    navController: NavHostController,
    selectedItem: MutableState<VChat?>,
    m: VChat,
    showContextMenu: MutableState<Boolean>,
    onForward: (VChat) -> Unit,
    onEnterSelectMode: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (Set<String>) -> Unit,
) {
    PDropdownMenu(
        expanded = showContextMenu.value && selectedItem.value == m,
        onDismissRequest = {
            selectedItem.value = null
            showContextMenu.value = false
        },
    ) {
        PDropdownMenuItem(
            text = { Text(stringResource(Res.string.select)) },
            onClick = {
                onEnterSelectMode()
                onSelect(m.id)
                selectedItem.value = null
                showContextMenu.value = false
            },
        )
        PDropdownMenuItem(
            text = { Text(stringResource(Res.string.forward)) },
            onClick = {
                selectedItem.value = null
                showContextMenu.value = false
                onForward(m)
            },
        )
        if (m.value is DMessageText) {
            PDropdownMenuItem(
                text = { Text(stringResource(Res.string.copy_text)) },
                onClick = {
                    selectedItem.value = null
                    showContextMenu.value = false
                    val text = (m.value as DMessageText).text
                    setClipboardText(LocaleHelper.getString(Res.string.message), text)
                    DialogHelper.showTextCopiedMessage(text)
                },
            )
            if (m.fromId == "me") {
                PDropdownMenuItem(
                    text = { Text(stringResource(Res.string.edit_text)) },
                    onClick = {
                        selectedItem.value = null
                        showContextMenu.value = false
                        val content = (m.value as DMessageText).text
                        navController.navigateChatEditText(m.id, content)
                    },
                )
            }
        }
        PDropdownMenuItem(
            text = { Text(stringResource(Res.string.delete)) },
            onClick = {
                selectedItem.value = null
                showContextMenu.value = false
                onDelete(setOf(m.id))
            },
        )
    }
}
