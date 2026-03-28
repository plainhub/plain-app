package com.ismartcoding.plain.ui.page.appfiles.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.VAppFile

@Composable
fun AppFileListContent(
    navController: NavHostController,
    files: List<VAppFile>,
    isLoading: Boolean,
    previewerState: MediaPreviewerState,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (files.isEmpty()) {
        NoDataView(
            iconResId = R.drawable.package_open,
            message = stringResource(R.string.no_app_files),
            showRefreshButton = true,
            onRefresh = onRefresh,
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(files, key = { it.appFile.id }) { file ->
            val itemState = rememberTransformItemState()
            AppFileListItem(
                file = file,
                itemState = itemState,
                previewerState = previewerState,
                onClick = {
                    openAppFile(context, files, file, navController, previewerState, itemState)
                },
            )
        }
        item { BottomSpace() }
    }
}
