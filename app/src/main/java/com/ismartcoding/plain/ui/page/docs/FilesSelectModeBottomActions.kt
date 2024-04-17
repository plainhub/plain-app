package com.ismartcoding.plain.ui.page.docs

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.theme.bottomAppBarContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilesSelectModeBottomActions(
    viewModel: DocsViewModel,
) {
    val context = LocalContext.current
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    LaunchedEffect(Unit) {
        groupButtons.addAll(listOf(
            GroupButton(
                icon = Icons.Outlined.Share,
                text = LocaleHelper.getString(R.string.share),
                onClick = {
                    ShareHelper.sharePaths(context, viewModel.selectedIds.toSet())
                }
            ),
            GroupButton(
                icon = Icons.Outlined.DeleteForever,
                text = LocaleHelper.getString(R.string.delete),
                onClick = {
                    DialogHelper.confirmToDelete {
                        viewModel.delete(viewModel.selectedIds.toSet())
                        viewModel.exitSelectMode()
                    }
                }
            ),
        ))
    }
    BottomAppBar(
        modifier = Modifier.height(120.dp),
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.bottomAppBarContainer(),
    ) {
        GroupButtons(
            buttons = groupButtons
        )
    }
}