package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.annotation.IntRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.components.mediaviewer.MediaGallery
import com.ismartcoding.plain.ui.components.mediaviewer.MediaGalleryState
import com.ismartcoding.plain.ui.components.mediaviewer.ViewImageBottomSheet
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.preview.PreviewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope


val DEFAULT_SOFT_ANIMATION_SPEC = tween<Float>(320)

@OptIn(ExperimentalFoundationApi::class)
class MediaPreviewerState(
    scope: CoroutineScope = MainScope(),
    galleryState: MediaGalleryState,
) : PreviewerVerticalDragState(scope, galleryState = galleryState) {


    companion object {
        fun getSaver(galleryState: MediaGalleryState): Saver<MediaPreviewerState, *> {
            return mapSaver(
                save = {
                    mapOf<String, Any>(
                        it.galleryState.pagerState::currentPage.name to it.galleryState.pagerState.currentPage,
                        it::animateContainerVisibleState.name to it.animateContainerVisibleState.currentState,
                        it::uiAlpha.name to it.uiAlpha.value,
                        it::visible.name to it.visible,
                    )
                },
                restore = {
                    val previewerState = MediaPreviewerState(galleryState = galleryState)
                    previewerState.animateContainerVisibleState =
                        MutableTransitionState(it[MediaPreviewerState::animateContainerVisibleState.name] as Boolean)
                    previewerState.uiAlpha = Animatable(it[MediaPreviewerState::uiAlpha.name] as Float)
                    previewerState.visible = it[MediaPreviewerState::visible.name] as Boolean
                    previewerState
                }
            )
        }
    }
}

/**
 * 记录预览组件状态
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPreviewerState(
    scope: CoroutineScope = rememberCoroutineScope(),
    verticalDragType: VerticalDragType = VerticalDragType.UpAndDown,
    @IntRange(from = 0) initialPage: Int = 0,
    pageCount: () -> Int = { MediaPreviewData.items.size },
    getKey: (Int) -> Any = { MediaPreviewData.items[it].id },
): MediaPreviewerState {
    val pagerState = rememberPagerState(initialPage, pageCount = pageCount)
    val galleryState = remember { MediaGalleryState(pagerState) }
    val mediaPreviewerState = rememberSaveable(saver = MediaPreviewerState.getSaver(galleryState)) {
        MediaPreviewerState(galleryState = galleryState)
    }
    mediaPreviewerState.scope = scope
    mediaPreviewerState.getKey = getKey
    mediaPreviewerState.verticalDragType = verticalDragType
    return mediaPreviewerState
}

/**
 * 默认的弹出预览时的动画效果
 */
val DEFAULT_PREVIEWER_ENTER_TRANSITION =
    scaleIn(tween(180)) + fadeIn(tween(240))

/**
 * 默认的关闭预览时的动画效果
 */
val DEFAULT_PREVIEWER_EXIT_TRANSITION =
    scaleOut(tween(320)) + fadeOut(tween(240))

// 默认淡入淡出动画窗格
val DEFAULT_CROSS_FADE_ANIMATE_SPEC: AnimationSpec<Float> = tween(80)

// 加载占位默认的进入动画
val DEFAULT_PLACEHOLDER_ENTER_TRANSITION = fadeIn(tween(200))

// 加载占位默认的退出动画
val DEFAULT_PLACEHOLDER_EXIT_TRANSITION = fadeOut(tween(200))

// 默认的加载占位
val DEFAULT_PREVIEWER_PLACEHOLDER_CONTENT = @Composable {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White.copy(0.2F))
    }
}

// 加载时的占位内容
class PreviewerPlaceholder(
    var enterTransition: EnterTransition = DEFAULT_PLACEHOLDER_ENTER_TRANSITION,
    var exitTransition: ExitTransition = DEFAULT_PLACEHOLDER_EXIT_TRANSITION,
    var content: @Composable () -> Unit = DEFAULT_PREVIEWER_PLACEHOLDER_CONTENT,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreviewer(
    modifier: Modifier = Modifier,
    state: MediaPreviewerState,
    getItem: @Composable (Int) -> PreviewItem = { index ->
        MediaPreviewData.items[index]
    },
    castViewModel: CastViewModel = viewModel(),
    tagsViewModel: TagsViewModel? = null,
    tagsMap: Map<String, List<DTagRelation>>? = null,
    tagsState: List<DTag> = emptyList(),
    onRenamed: () -> Unit = {},
    deleteAction: (PreviewItem) -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(
        key1 = state.animateContainerVisibleState,
        key2 = state.animateContainerVisibleState.currentState
    ) {
        state.onAnimateContainerStateChanged()
    }
    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visibleState = state.animateContainerVisibleState,
        enter = DEFAULT_PREVIEWER_ENTER_TRANSITION,
        exit = DEFAULT_PREVIEWER_EXIT_TRANSITION,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.getKey) {
                    state.verticalDrag(this)
                }
        ) {
            MediaGallery(
                modifier = modifier.fillMaxSize(),
                state = state.galleryState,
                getItem = getItem,
                detectGesture = {
                    onTap = {
                        state.showActions = !state.showActions
                    }
                },
                galleryLayer = {
                    this.viewerContainer = { page, viewerState, viewer ->
                        val viewerContainerState = rememberViewerContainerState(
                            viewerState = viewerState,
                        )
                        LaunchedEffect(key1 = state.galleryState.pagerState.currentPage) {
                            if (state.galleryState.pagerState.currentPage == page) {
                                state.viewerContainerState = viewerContainerState
                                state.currentViewerState = viewerState
                            }
                        }
                        MediaViewerContainer(
                            modifier = Modifier.alpha(state.viewerAlpha.value),
                            containerState = viewerContainerState,
                            placeholder = PreviewerPlaceholder(),
                            viewer = viewer,
                        )
                    }
                    this.background = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(state.uiAlpha.value)
                                .background(Color.Black)
                        )
                    }
                    this.foreground = { page ->
                        val m = getItem(page)
                        if (!m.path.isVideoFast()) {
                            ImagePreviewActions(context = context, castViewModel = castViewModel, m = m, getViewerState = { state.currentViewerState }, state)
                        }
                    }
                },
            )
        }
    }
    if (state.showMediaInfo) {
        val m = getItem(state.galleryState.pagerState.currentPage)
        ViewImageBottomSheet(m,
            tagsViewModel, tagsMap, tagsState,
            onDismiss = {
                state.showMediaInfo = false
            },
            onRenamed = onRenamed,
            deleteAction = {
                deleteAction(m)
            })
    }

    state.ticket.Next()
}
