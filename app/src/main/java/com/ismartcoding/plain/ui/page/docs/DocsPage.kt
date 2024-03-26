package com.ismartcoding.plain.ui.page.docs

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.PdfViewerDialog
import com.ismartcoding.plain.ui.TextEditorDialog
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    viewModel: DocsViewModel = viewModel(),
) {
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(context) }
                setRefreshState(RefreshContentState.Stop)
            }
        }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(context)
        }
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.docs),
        actions = {
            ActionButtonSearch {
                navController.navigate("${RouteName.DOCS.name}/search?q=")
            }
        }
    ) {
        PullToRefresh(
            refreshLayoutState = topRefreshLayoutState,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            PListItem(
                                title = m.name,
                                desc = FormatHelper.formatBytes(m.size) + ", " +  m.updatedAt.formatDateTime(),
                                icon = AppHelper.getFileIconPath(m.name.getFilenameExtension()),
                                modifier = PlainTheme.getCardModifier(),
                                onClick = {
                                    if (m.path.isTextFile()) {
                                        if (m.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                                            TextEditorDialog(Uri.fromFile(File(m.path))).show()
                                        } else {
                                            DialogHelper.showMessage(R.string.text_file_size_limit)
                                        }
                                    } else if (m.path.isPdfFile()) {
                                        PdfViewerDialog(Uri.fromFile(File(m.path))).show()
                                    } else {
                                        navController.navigate("${RouteName.OTHER_FILE.name}?path=${m.path}")
                                    }
                                }
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        withIO { viewModel.moreAsync(context) }
                                    }
                                }
                            }
                            LoadMoreRefreshContent(viewModel.noMore.value)
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}


