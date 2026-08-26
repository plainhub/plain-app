package com.ismartcoding.plain.ui.page.imageeditor

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DImageEditorProject
import com.ismartcoding.plain.platform.formatTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.models.ImageEditorViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorListPage(
    navController: NavHostController,
    vm: ImageEditorViewModel = viewModel { ImageEditorViewModel() },
) {
    val itemsState by vm.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { withIO { vm.loadAsync() }; setRefreshState(RefreshContentState.Finished) }
    }

    LaunchedEffect(Unit) { scope.launch { withIO { vm.loadAsync() } } }

    PScaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.image_editor),
                scrollBehavior = scrollBehavior,
                actions = {
                    PCapsuleMoreClose(
                        showMore = false,
                        onClose = { navController.navigateUp() },
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routing.ImageEditorDetail("")) }) {
                Icon(painter = painterResource(Res.drawable.plus), contentDescription = stringResource(Res.string.add))
            }
        },
    ) { paddingValues ->
        PullToRefresh(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            refreshLayoutState = topRefreshLayoutState,
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    ) {
                        item { TopSpace() }
                        items(itemsState, key = { it.id }) { project ->
                            ImageEditorProjectListItem(
                                project = project,
                                onClick = { navController.navigate(Routing.ImageEditorDetail(project.id)) },
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item { BottomSpace(paddingValues) }
                    }
                } else {
                    NoDataColumn(loading = vm.showLoading.value)
                }
            }
        }
    }
}

@Composable
private fun ImageEditorProjectListItem(
    project: DImageEditorProject,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val thumbModel = project.thumbnail?.let { "data:image/png;base64,$it" }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbModel != null) {
                AsyncImage(
                    model = thumbModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.image),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.image_editor_layer_count, project.layerCount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${project.canvasWidth}×${project.canvasHeight}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = project.updatedAt.formatTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
