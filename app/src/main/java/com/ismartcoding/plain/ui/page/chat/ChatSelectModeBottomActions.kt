package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatSelectModeBottomActions(
    chatVM: ChatViewModel,
) {
    val context = LocalContext.current

    PBottomAppBar {
        BottomActionButtons {
            IconTextSmallButtonDelete {
                chatVM.delete(context, chatVM.selectedIds.toSet())
                chatVM.exitSelectMode()
            }
        }
    }
}