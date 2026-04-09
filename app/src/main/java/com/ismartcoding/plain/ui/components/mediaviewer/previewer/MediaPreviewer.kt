package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import com.ismartcoding.plain.ui.components.mediaviewer.toggleScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.components.mediaviewer.GestureScope
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewer
import com.ismartcoding.plain.ui.components.mediaviewer.ViewMediaBottomSheet
import com.ismartcoding.plain.ui.components.mediaviewer.rememberViewerState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewer(
    state: MediaPreviewerState,
    getItem: (Int) -> PreviewItem = { index -> MediaPreviewData.items.getOrNull(index) ?: PreviewItem(index.toString()) },
    castVM: CastViewModel = viewModel(LocalContext.current as ComponentActivity),
    tagsVM: TagsViewModel? = null, tagsMap: Map<String, List<DTagRelation>>? = null, tagsState: List<DTag> = emptyList(),
    onRenamed: () -> Unit = {}, deleteAction: (PreviewItem) -> Unit = {}, onTagsChanged: () -> Unit = {},
) {
    LaunchedEffect(key1 = state.animateContainerVisibleState, key2 = state.animateContainerVisibleState.currentState) {
        state.onAnimateContainerStateChanged()
    }
    LaunchedEffect(state.visible) {
        state.videoState.isPreviewerOpen = state.visible
    }

    AnimatedVisibility(modifier = Modifier.fillMaxSize(), visibleState = state.animateContainerVisibleState,
        enter = DEFAULT_PREVIEWER_ENTER_TRANSITION, exit = DEFAULT_PREVIEWER_EXIT_TRANSITION) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(state.getKey) { state.verticalDrag(this) }) {
            val scope = rememberCoroutineScope()
            Box(modifier = Modifier.fillMaxSize().alpha(state.uiAlpha.value).background(Color.Black))
            HorizontalPager(state = state.pagerState, modifier = Modifier.fillMaxSize(), pageSpacing = 16.dp,
                userScrollEnabled = state.pagerUserScrollEnabled) { page ->
                val viewerState = rememberViewerState()
                val viewerContainerState = rememberViewerContainerState(viewerState = viewerState)
                LaunchedEffect(key1 = state.pagerState.currentPage) {
                    if (state.pagerState.currentPage == page) state.viewerContainerState = viewerContainerState
                }
                MediaViewerContainer(modifier = Modifier.alpha(state.viewerAlpha.value), containerState = viewerContainerState,
                    placeholder = PreviewerPlaceholder(), viewer = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            key(page) {
                                val item = remember(page, MediaPreviewData.items.size) { MediaPreviewData.items.getOrNull(page) }
                                if (item != null) {
                                    MediaViewer(modifier = Modifier.fillMaxSize(), pagerState = state.pagerState,
                                        videoState = state.videoState, page = page, model = getModel(item), state = viewerState, boundClip = false,
                                        gesture = GestureScope(onTap = { state.showActions = !state.showActions },
                                            onDoubleTap = { scope.launch { viewerState.toggleScale(it) }; false }, onLongPress = {}))
                                }
                            }
                        }
                    })
            }
            val m = remember(state.pagerState.currentPage, MediaPreviewData.items.size) { MediaPreviewData.items.getOrNull(state.pagerState.currentPage) }
            if (m != null) {
                if (m.isVideo()) VideoPreviewActions(context = LocalContext.current, castViewModel = castVM, m = m, state)
                else ImagePreviewActions(context = LocalContext.current, castViewModel = castVM, m = m, state)
            }
            SpeedBoostIndicator(state)
        }
    }
    if (state.showMediaInfo) {
        val m = MediaPreviewData.items.getOrNull(state.pagerState.currentPage)
        if (m != null) {
            ViewMediaBottomSheet(m, tagsVM, tagsMap, tagsState, onDismiss = { state.showMediaInfo = false },
                onRenamedAsync = onRenamed, deleteAction = { deleteAction(m) }, onTagsChangedAsync = onTagsChanged)
        } else { state.showMediaInfo = false }
    }
    state.ticket.Next()
}

@Composable
private fun SpeedBoostIndicator(state: MediaPreviewerState) {
    AnimatedVisibility(visible = state.videoState.isSpeedBoostActive,
        modifier = Modifier.statusBarsPadding().padding(top = 16.dp), enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
        Row(modifier = Modifier.background(color = Color.Black.copy(alpha = 0.6f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.double_arrow_right), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text = " 2x", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
