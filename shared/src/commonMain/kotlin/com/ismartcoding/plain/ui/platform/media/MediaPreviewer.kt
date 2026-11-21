package com.ismartcoding.plain.ui.platform.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.ui.platform.system.exitImmersiveFullscreen
import com.ismartcoding.plain.ui.platform.system.setSystemBarsVisible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.ui.media.viewer.GestureScope
import com.ismartcoding.plain.ui.media.viewer.ImagePreviewActions
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.ViewMediaBottomSheet
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformContentView
import com.ismartcoding.plain.ui.media.viewer.previewer.ViewerContainerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformContentState
import com.ismartcoding.plain.ui.media.viewer.rememberViewerState
import com.ismartcoding.plain.ui.media.viewer.video.VideoPreviewActions
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Grace period before showing a loading icon: media that becomes ready within
// this window switches without any visible loading state (Douyin-like).
internal const val LOADING_GRACE_MILLIS = 1000L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewer(
    state: MediaPreviewerState,
    castVM: CastViewModel = viewModel { CastViewModel() },
    tagsVM: TagsViewModel? = null,
    tagsMap: Map<String, List<TagRelation>>? = null,
    tagsState: List<Tag> = emptyList(),
    onRenamed: () -> Unit = {},
    deleteAction: (PreviewItem) -> Unit = {},
    onTagsChanged: () -> Unit = {},
) {
    val items = MediaPreviewData.items
    LaunchedEffect(key1 = state.animateContainerVisibleState, key2 = state.animateContainerVisibleState.currentState) {
        state.onAnimateContainerStateChanged()
    }
    if (state.visible) {
        // System bar policy (see docs/media-viewer-system-bars.md):
        //  - Bottom system bar (gesture / 3-button) is ALWAYS hidden.
        //  - Status bar follows the control overlay, but only for video.
        //    Image preview has no top bar (floating chips only), so it stays
        //    fully immersive regardless of showActions.
        //  - Fullscreen video forces immersive (handled by VideoPlayerSurface).
        val currentIsVideo = items.getOrNull(state.pagerState.currentPage)?.isVideo() == true
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
                // Pre-compose adjacent pages so their media (image/video) is
                // loaded ahead of time — switching never shows a loading state.
                beyondViewportPageCount = 1,
                userScrollEnabled = state.pagerUserScrollEnabled,
            ) { page ->
                val item = items.getOrNull(page)
                key(item?.id) {
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
                                if (item != null) {
                                    MediaViewer(
                                        modifier = Modifier.fillMaxSize(),
                                        pagerState = state.pagerState,
                                        videoState = state.videoState,
                                        page = page,
                                        model = item,
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
                            val viewerMounted by viewerState.mountedFlow.collectAsState(initial = false)
                            if (showLoading) {
                                val placeholder = PreviewerPlaceholder()
                                // Show the loading icon only after the grace period:
                                // quick switches never flash a loading state, only
                                // genuinely slow loads reveal it (Douyin-like).
                                var showLoadingIcon by remember { mutableStateOf(false) }
                                LaunchedEffect(viewerMounted) {
                                    if (!viewerMounted) {
                                        delay(LOADING_GRACE_MILLIS)
                                        showLoadingIcon = true
                                    } else {
                                        showLoadingIcon = false
                                    }
                                }
                                AnimatedVisibility(
                                    visible = showLoadingIcon,
                                    enter = placeholder.enterTransition,
                                    exit = placeholder.exitTransition,
                                ) { placeholder.content() }
                            }
                        }
                    }
                }
            }
            val m = items.getOrNull(state.pagerState.currentPage)
            if (m != null) {
                if (m.isVideo()) {
                    VideoPreviewActions(m = m, state = state)
                } else if (m.pickedMediaType == null) {
                    ImagePreviewActions(castViewModel = castVM, m = m, state = state)
                }
            }
            PreviewerCloseButton(state)
            SpeedBoostIndicator(state)
        }
    }
    if (state.showMediaInfo) {
        val m = items.getOrNull(state.pagerState.currentPage)
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
