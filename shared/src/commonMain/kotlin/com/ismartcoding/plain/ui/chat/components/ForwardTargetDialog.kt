package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.forward

/**
 * Multi-select forward target picker: local chat, joined channels, paired
 * peers. Fires once with every selected target on confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardTargetDialog(
    onDismiss: () -> Unit,
    onTargetsSelected: (List<ChatTarget>) -> Unit,
    enabled: Boolean = true,
    sending: Boolean = false,
    beforeConfirm: @Composable () -> Unit = {},
) {
    val options = chatTargetOptions()
    val selectedIds = remember { mutableStateListOf<String>() }

    val eligibleSelection = options.filter { it.target.encodedToId in selectedIds }
    PModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        sheetGesturesEnabled = !sending,
    ) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.forward))
        ChatTargetPicker(
            options = options,
            selectedIds = eligibleSelection.map { it.target.encodedToId },
            onToggle = { id -> if (!selectedIds.remove(id)) selectedIds.add(id) },
            onConfirm = {
                if (enabled && !sending && eligibleSelection.isNotEmpty()) onTargetsSelected(eligibleSelection.map { it.target })
            },
            enabled = enabled,
            sending = sending,
            beforeConfirm = beforeConfirm,
        )
        BottomSpace()
    }
}
