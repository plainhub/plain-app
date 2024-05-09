package com.ismartcoding.plain.ui.base.mediaviewer.previewer

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.mediaviewer.GalleryGestureScope
import com.ismartcoding.plain.ui.base.mediaviewer.ImageGallery
import com.ismartcoding.plain.ui.base.mediaviewer.ImageGalleryState
import com.ismartcoding.plain.ui.base.mediaviewer.rememberImageGalleryState
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.preview.PreviewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

val DEEP_DARK_FANTASY = Color(0xFF000000)
val DEEP_LIGHT_FANTASY = Color(0xFFFFFFFF)

// 图片间的默认间隔
val DEFAULT_ITEM_SPACE = 12.dp

// 比较轻柔的动画窗格
val DEFAULT_SOFT_ANIMATION_SPEC = tween<Float>(320)

@OptIn(ExperimentalFoundationApi::class)
class ImagePreviewerState(
    // 协程作用域
    scope: CoroutineScope = MainScope(),
    // 默认动画窗格
    defaultAnimationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC,
    // 预览状态
    galleryState: ImageGalleryState,
) : PreviewerVerticalDragState(scope, defaultAnimationSpec, galleryState = galleryState) {
    companion object {
        fun getSaver(galleryState: ImageGalleryState): Saver<ImagePreviewerState, *> {
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
                    val previewerState = ImagePreviewerState(galleryState = galleryState)
                    previewerState.animateContainerVisibleState =
                        MutableTransitionState(it[ImagePreviewerState::animateContainerVisibleState.name] as Boolean)
                    previewerState.uiAlpha =
                        Animatable(it[ImagePreviewerState::uiAlpha.name] as Float)
                    previewerState.visible = it[ImagePreviewerState::visible.name] as Boolean
                    previewerState
                }
            )
        }
    }
}

/**
 * 记录预览组件状态
 */
@Composable
fun rememberPreviewerState(
    // 协程作用域
    scope: CoroutineScope = rememberCoroutineScope(),
    // 动画窗格
    animationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC,
    // 开启垂直手势的类型
    verticalDragType: VerticalDragType = VerticalDragType.UpAndDown,
    // 初始页码
    @IntRange(from = 0) initialPage: Int = 0,
    // 获取页数
    pageCount: () -> Int,
    // 提供给组件用于获取key的方法
    getKey: ((Int) -> Any)? = null,
): ImagePreviewerState {
    val galleryState = rememberImageGalleryState(initialPage, pageCount)
    val imagePreviewerState = rememberSaveable(saver = ImagePreviewerState.getSaver(galleryState)) {
        ImagePreviewerState(galleryState = galleryState)
    }
    imagePreviewerState.scope = scope
    imagePreviewerState.getKey = getKey
    imagePreviewerState.defaultAnimationSpec = animationSpec
    imagePreviewerState.verticalDragType = verticalDragType
    return imagePreviewerState
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
    state: ImagePreviewerState,
    getItem: @Composable (Int) -> PreviewItem = { index ->
        MediaPreviewData.items[index]
    },
    itemSpacing: Dp = DEFAULT_ITEM_SPACE,
    enter: EnterTransition = DEFAULT_PREVIEWER_ENTER_TRANSITION,
    exit: ExitTransition = DEFAULT_PREVIEWER_EXIT_TRANSITION,
    detectGesture: GalleryGestureScope.() -> Unit = {},
    foreground: @Composable ((page: Int) -> Unit) = { _ -> },
) {
    state.apply {
        LaunchedEffect(
            key1 = animateContainerVisibleState,
            key2 = animateContainerVisibleState.currentState
        ) {
            onAnimateContainerStateChanged()
        }
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visibleState = animateContainerVisibleState,
            enter = enterTransition ?: enter,
            exit = exitTransition ?: exit,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(getKey) {
                        verticalDrag(this)
                    }
            ) {
                @Composable
                fun UIContainer(content: @Composable () -> Unit) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(uiAlpha.value)
                    ) {
                        content()
                    }
                }
                ImageGallery(
                    modifier = modifier.fillMaxSize(),
                    state = galleryState,
                    getItem = getItem,
                    itemSpacing = itemSpacing,
                    detectGesture = detectGesture,
                    galleryLayer = {
                        this.viewerContainer = { page, viewerState, viewer ->
                            val viewerContainerState = rememberViewerContainerState(
                                viewerState = viewerState,
                                animationSpec = defaultAnimationSpec
                            )
                            LaunchedEffect(key1 = galleryState.pagerState.currentPage) {
                                if (galleryState.pagerState.currentPage == page) {
                                    state.viewerContainerState = viewerContainerState
                                }
                            }
                            ImageViewerContainer(
                                modifier = Modifier.alpha(viewerAlpha.value),
                                containerState = viewerContainerState,
                                placeholder = PreviewerPlaceholder(),
                                viewer = viewer,
                            )
                        }
                        this.background = { page ->
                            val item = getItem(page)
                            UIContainer {
                                Box(
                                    modifier = Modifier
                                        .background(if (item.path.endsWith(".svg", true)) DEEP_LIGHT_FANTASY else DEEP_DARK_FANTASY)
                                        .fillMaxSize()
                                )
                            }
                        }
                        this.foreground = {
                            UIContainer {
                                foreground(it)
                            }
                        }
                    },
                )
                if (!visible)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { } }) { }
            }
        }
        ticket.Next()
    }
}