package com.ismartcoding.plain.ui.page.docs

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocFilesSelectModeBottomActions(
    docsVM: DocsViewModel,
) {
    val context = LocalContext.current
    PBottomAppBar {
        BottomActionButtons {
            IconTextSmallButtonShare {
                ShareHelper.sharePaths(context, docsVM.selectedIds.toSet())
            }
            IconTextSmallButtonDelete {
                DialogHelper.confirmToDelete {
                    docsVM.delete(docsVM.selectedIds.toSet())
                    docsVM.exitSelectMode()
                }
            }
        }
    }
}