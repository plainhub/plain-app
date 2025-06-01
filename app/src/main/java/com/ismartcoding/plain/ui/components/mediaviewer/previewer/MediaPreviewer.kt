package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.ui.components.mediaviewer.GestureScope
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewer
import com.ismartcoding.plain.ui.components.mediaviewer.ViewMediaBottomSheet
import com.ismartcoding.plain.ui.components.mediaviewer.rememberDecoderImagePainter
import com.ismartcoding.plain.ui.components.mediaviewer.rememberViewerState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap


val DEFAULT_SOFT_ANIMATION_SPEC = tween<Float>(320)

val DEFAULT_PREVIEWER_ENTER_TRANSITION =
    scaleIn(tween(180)) + fadeIn(tween(240))

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

// 文件存在性缓存，避免重复检查
private val fileExistsCache = ConcurrentHashMap<String, Boolean>()

// 加载时的占位内容
class PreviewerPlaceholder(
    var enterTransition: EnterTransition = DEFAULT_PLACEHOLDER_ENTER_TRANSITION,
    var exitTransition: ExitTransition = DEFAULT_PLACEHOLDER_EXIT_TRANSITION,
    var content: @Composable () -> Unit = DEFAULT_PREVIEWER_PLACEHOLDER_CONTENT,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewer(
    state: MediaPreviewerState,
    getItem: (Int) -> PreviewItem = { index ->
        MediaPreviewData.items[index]
    },
    castVM: CastViewModel = viewModel(),
    tagsVM: TagsViewModel? = null,
    tagsMap: Map<String, List<DTagRelation>>? = null,
    tagsState: List<DTag> = emptyList(),
    onRenamed: () -> Unit = {},
    deleteAction: (PreviewItem) -> Unit = {},
    onTagsChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(
        key1 = state.animateContainerVisibleState,
        key2 = state.animateContainerVisibleState.currentState
    ) {
        state.onAnimateContainerStateChanged()
    }

    // Previewer -> ViewerContainer -> Viewer -> NormalImage/HugeImage/Video
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
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(state.uiAlpha.value)
                    .background(Color.Black)
            )
            HorizontalPager(
                state = state.pagerState,
                modifier = Modifier
                    .fillMaxSize(),
                pageSpacing = 16.dp,
            ) { page ->
                val viewerState = rememberViewerState()
                val viewerContainerState = rememberViewerContainerState(
                    viewerState = viewerState,
                )
                LaunchedEffect(key1 = state.pagerState.currentPage) {
                    if (state.pagerState.currentPage == page) {
                        state.viewerContainerState = viewerContainerState
                    }
                }
                MediaViewerContainer(
                    modifier = Modifier.alpha(state.viewerAlpha.value),
                    containerState = viewerContainerState,
                    placeholder = PreviewerPlaceholder(),
                    viewer = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),

                            ) {
                            key(page) {
                                val item = remember(page) {
                                    getItem(page)
                                }
                                MediaViewer(
                                    modifier = Modifier.fillMaxSize(),
                                    pagerState = state.pagerState,
                                    videoState = state.videoState,
                                    page = page,
                                    model = getModel(item),
                                    state = viewerState,
                                    boundClip = false,
                                    gesture = GestureScope(
                                        onTap = {
                                            state.showActions = !state.showActions
                                        },
                                        onDoubleTap = {
                                            scope.launch {
                                                viewerState.toggleScale(it)
                                            }
                                            false
                                        },
                                        onLongPress = {}
                                    ),
                                )
                            }
                        }
                    },
                )
            }
            val m = remember(state.pagerState.currentPage) {
                getItem(state.pagerState.currentPage)
            }
            if (m.path.isVideoFast()) {
                VideoPreviewActions(context = context, castViewModel = castVM, m = m, state)
            } else {
                ImagePreviewActions(context = context, castViewModel = castVM, m = m, state)
            }
        }
    }
    if (state.showMediaInfo) {
        val m = getItem(state.pagerState.currentPage)
        ViewMediaBottomSheet(
            m,
            tagsVM, tagsMap, tagsState,
            onDismiss = {
                state.showMediaInfo = false
            },
            onRenamedAsync = onRenamed,
            deleteAction = {
                deleteAction(m)
            },
            onTagsChangedAsync = onTagsChanged
        )
    }

    state.ticket.Next()
}

@Composable
fun getModel(item: PreviewItem): Any? {
    val model: Any?
    if (item.path.isVideoFast() || item.path.isUrl()) {
        model = item
    } else if (item.size <= 2000 * 1000) {
        // If the image size is less than 2MB, load the image directly
        model = item
    } else {
        val imageType = remember { ImageHelper.getImageType(item.path) }
        if (imageType.isApplicableAnimated() || imageType == ImageType.SVG) {
            model = item
        } else {
            val rotation = remember {
                if (item.rotation == -1) {
                    item.rotation = ImageHelper.getRotation(item.path)
                }
                item.rotation
            }
            
            // 使用缓存检查文件是否存在，避免重复文件系统调用
            val fileExists = remember(item.path) {
                fileExistsCache.getOrPut(item.path) {
                    File(item.path).exists()
                }
            }
            
            if (!fileExists) {
                // If file doesn't exist, return the item itself to handle gracefully
                model = item
            } else {
                val inputStream = remember(item.path) { 
                    try {
                        File(item.path).inputStream()
                    } catch (e: Exception) {
                        // If there's any error opening the file, return null to handle gracefully
                        // 如果文件访问失败，从缓存中移除该条目
                        fileExistsCache.remove(item.path)
                        null
                    }
                }
                
                val decoder = if (inputStream != null) {
                    rememberDecoderImagePainter(inputStream = inputStream, rotation = rotation)
                } else {
                    null
                }
                
                if (decoder != null) {
                    item.intrinsicSize = IntSize(decoder.decoderWidth, decoder.decoderHeight)
                }
                model = decoder ?: item
            }
        }
    }
    return model
}