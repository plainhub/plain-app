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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.plain.R
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

// Default enter animation for placeholder
val DEFAULT_PLACEHOLDER_ENTER_TRANSITION = fadeIn(tween(200))

// Default exit animation for placeholder
val DEFAULT_PLACEHOLDER_EXIT_TRANSITION = fadeOut(tween(200))

// Default placeholder content
val DEFAULT_PREVIEWER_PLACEHOLDER_CONTENT = @Composable {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White.copy(0.2F))
    }
}
// File existence cache to avoid repeated checks
private val fileExistsCache = ConcurrentHashMap<String, Boolean>()

// Placeholder content during loading
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
        MediaPreviewData.items.getOrNull(index) ?: PreviewItem(index.toString())
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
                userScrollEnabled = state.pagerUserScrollEnabled,
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
                        }
                    },
                )
            }
            val m = remember(state.pagerState.currentPage, MediaPreviewData.items.size) {
                MediaPreviewData.items.getOrNull(state.pagerState.currentPage)
            }
            if (m != null) {
                if (m.isVideo()) {
                    VideoPreviewActions(context = context, castViewModel = castVM, m = m, state)
                } else {
                    ImagePreviewActions(context = context, castViewModel = castVM, m = m, state)
                }
            }
            // 2x speed boost indicator shown during long-press
            AnimatedVisibility(
                visible = state.videoState.isSpeedBoostActive,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.double_arrow_right),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = " 2x",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    if (state.showMediaInfo) {
        val m = MediaPreviewData.items.getOrNull(state.pagerState.currentPage)
        if (m != null) {
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
        } else {
            // No valid item; ensure the info panel is closed
            state.showMediaInfo = false
        }
    }

    state.ticket.Next()
}

@Composable
fun getModel(item: PreviewItem): Any? {
    val model: Any?
    if (item.isVideo() || item.path.isUrl()) {
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
            
            // Use cache to check file existence, avoid repeated file system calls
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
                        // If file access fails, remove the entry from cache
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