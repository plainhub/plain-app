package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.forward
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import org.jetbrains.compose.resources.stringResource

/**
 * Multi-select forward target picker: local chat, joined channels, paired
 * peers. Fires once with every selected target on confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardTargetDialog(
    onDismiss: () -> Unit,
    onTargetsSelected: (List<ChatTarget>) -> Unit
) {
    val options = chatTargetOptions()
    val selectedIds = remember { mutableStateListOf<String>() }

    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.forward))
        ChatTargetPicker(
            selectedIds = selectedIds.toList(),
            onToggle = { id -> if (!selectedIds.remove(id)) selectedIds.add(id) },
            onConfirm = {
                onTargetsSelected(options.filter { selectedIds.contains(it.target.encodedToId) }.map { it.target })
            },
        )
        BottomSpace()
    }
}
