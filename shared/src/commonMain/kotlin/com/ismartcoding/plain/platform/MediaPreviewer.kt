package com.ismartcoding.plain.platform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.double_arrow_right
import com.ismartcoding.plain.ui.components.mediaviewer.GestureScope
import com.ismartcoding.plain.ui.components.mediaviewer.ImagePreviewActions
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.ViewMediaBottomSheet
import com.ismartcoding.plain.ui.components.mediaviewer.rememberViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformContentView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.ViewerContainerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformContentState
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPreviewActions
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewer(
    state: MediaPreviewerState,
    castVM: CastViewModel = viewModel { CastViewModel() },
    tagsVM: TagsViewModel? = null,
    tagsMap: Map<String, List<DTagRelation>>? = null,
    tagsState: List<DTag> = emptyList(),
    onRenamed: () -> Unit = {},
    deleteAction: (PreviewItem) -> Unit = {},
    onTagsChanged: () -> Unit = {},
) {
    LaunchedEffect(key1 = state.animateContainerVisibleState, key2 = state.animateContainerVisibleState.currentState) {
        state.onAnimateContainerStateChanged()
    }
    LaunchedEffect(state.visible) {
        state.videoState.isPreviewerOpen = state.visible
    }
    if (state.visible) {
        // System bar policy (see docs/media-viewer-system-bars.md):
        //  - Bottom system bar (gesture / 3-button) is ALWAYS hidden.
        //  - Status bar follows the control overlay, but only for video.
        //    Image preview has no top bar (floating chips only), so it stays
        //    fully immersive regardless of showActions.
        //  - Fullscreen video forces immersive (handled by VideoPlayerSurface).
        val currentIsVideo = remember(state.pagerState.currentPage, MediaPreviewData.items.size) {
            MediaPreviewData.items.getOrNull(state.pagerState.currentPage)?.isVideo() == true
        }
        LaunchedEffect(currentIsVideo, state.showActions, state.videoState.isFullscreenMode) {
            when {
                state.videoState.isFullscreenMode -> setSystemBarsVisible(false)
                currentIsVideo -> setSystemBarsVisible(state.showActions)
                else -> setSystemBarsVisible(false)
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                exitImmersiveFullscreen()
            }
        }
    }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visibleState = state.animateContainerVisibleState,
        enter = DEFAULT_PREVIEWER_ENTER_TRANSITION,
        exit = DEFAULT_PREVIEWER_EXIT_TRANSITION,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    state.verticalDrag(this)
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(state.uiAlpha.value)
                    .background(Color.Black),
            )
            HorizontalPager(
                state = state.pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                userScrollEnabled = state.pagerUserScrollEnabled,
            ) { page ->
                val viewerContainerState = rememberSaveable(saver = ViewerContainerState.Saver) { ViewerContainerState() }
                viewerContainerState.scope = scope
                viewerContainerState.viewerState = rememberViewerState()
                viewerContainerState.transformState = rememberTransformContentState()
                LaunchedEffect(key1 = state.pagerState.currentPage) {
                    if (state.pagerState.currentPage == page) {
                        state.viewerContainerState = viewerContainerState
                    }
                }

                viewerContainerState.apply {
                    Box(
                        modifier = Modifier
                            .alpha(state.viewerAlpha.value)
                            .fillMaxSize()
                            .onGloballyPositioned { containerSize = it.size }
                            .graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                                translationX = offsetX.value
                                translationY = offsetY.value
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(transformContentAlpha.value),
                        ) {
                            TransformContentView(transformState)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(viewerContainerAlpha.value),
                        ) {
                            key(page) {
                                val item = remember(page, MediaPreviewData.items.size) {
                                    MediaPreviewData.items.getOrNull(page)
                                }
                                if (item != null) {
                                    MediaViewer(
                                        modifier = Modifier.fillMaxSize(),
                                        pagerState = state.pagerState,
                                        videoState = state.videoState,
                                        page = page,
                                        model = getModel(item),
                                        state = viewerState,
                                        boundClip = false,
                                        gesture = GestureScope(
                                            onTap = { state.showActions = !state.showActions },
                                            onDoubleTap = {
                                                scope.launch { viewerState.toggleScale(it) }
                                                false
                                            },
                                            onLongPress = {},
                                        ),
                                    )
                                }
                            }
                        }
                        val viewerMounted by viewerState.mountedFlow.collectAsState(initial = false)
                        if (showLoading) {
                            val placeholder = PreviewerPlaceholder()
                            AnimatedVisibility(
                                visible = !viewerMounted,
                                enter = placeholder.enterTransition,
                                exit = placeholder.exitTransition,
                            ) { placeholder.content() }
                        }
                    }
                }
            }
            val m = remember(state.pagerState.currentPage, MediaPreviewData.items.size) {
                MediaPreviewData.items.getOrNull(state.pagerState.currentPage)
            }
            if (m != null) {
                if (m.isVideo()) {
                    VideoPreviewActions(m = m, state = state)
                } else {
                    ImagePreviewActions(castViewModel = castVM, m = m, state = state)
                }
            }
            SpeedBoostIndicator(state)
        }
    }
    if (state.showMediaInfo) {
        val m = MediaPreviewData.items.getOrNull(state.pagerState.currentPage)
        if (m != null) {
            ViewMediaBottomSheet(
                m,
                tagsVM,
                tagsMap,
                tagsState,
                onDismiss = { state.showMediaInfo = false },
                onRenamedAsync = onRenamed,
                deleteAction = { deleteAction(m) },
                onTagsChangedAsync = onTagsChanged,
                castViewModel = castVM,
            )
        } else {
            state.showMediaInfo = false
        }
    }
    state.ticket.Next()
}

@Composable
private fun SpeedBoostIndicator(state: MediaPreviewerState) {
    AnimatedVisibility(
        visible = state.videoState.isSpeedBoostActive,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp), enter = fadeIn(tween(150)), exit = fadeOut(tween(150))
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(Res.drawable.double_arrow_right), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(text = " 2x", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}
