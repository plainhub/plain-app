package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.ui.preview.PreviewItem
import kotlinx.coroutines.launch
import java.io.File

class GalleryGestureScope(
    // 点击事件
    var onTap: () -> Unit = {},
    // 双击事件
    var onDoubleTap: () -> Boolean = { false },
    // 长按事件
    var onLongPress: () -> Unit = {},
)

/**
 * gallery图层对象
 */
class GalleryLayerScope(
    // viewer图层
    var viewerContainer: @Composable (
        page: Int, viewerState: MediaViewerState, viewer: @Composable () -> Unit
    ) -> Unit = { _, _, viewer -> viewer() },
    // 背景图层
    var background: @Composable ((Int) -> Unit) = {},
    // 前景图层
    var foreground: @Composable ((Int) -> Unit) = {},
)

@OptIn(ExperimentalFoundationApi::class)
class MediaGalleryState(
    val pagerState: PagerState,
) {
    var mediaViewerState by mutableStateOf<MediaViewerState?>(null)
        internal set
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGallery(
    modifier: Modifier = Modifier,
    state: MediaGalleryState,
    getItem: @Composable (Int) -> PreviewItem,
    detectGesture: GalleryGestureScope.() -> Unit = {},
    galleryLayer: GalleryLayerScope.() -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    // 手势相关
    val galleryGestureScope = remember { GalleryGestureScope() }
    detectGesture.invoke(galleryGestureScope)
    // 图层相关
    val galleryLayerScope = remember { GalleryLayerScope() }
    galleryLayer.invoke(galleryLayerScope)
    // 确保不会越界
    val currentPage = state.pagerState.currentPage

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        galleryLayerScope.background(currentPage)
        HorizontalPager(
            state = state.pagerState,
            modifier = Modifier
                .fillMaxSize(),
            pageSpacing = 16.dp,
        ) { page ->
            val viewerState = rememberViewerState()
            LaunchedEffect(key1 = currentPage) {
                // if (currentPage != page) viewerState.reset()
                if (currentPage == page) {
                    state.mediaViewerState = viewerState
                }
            }
            galleryLayerScope.viewerContainer(page, viewerState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    key(page) {
                        val item = getItem(page)
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
                                val inputStream = remember(item.path) { File(item.path).inputStream() }
                                val decoder = rememberDecoderImagePainter(inputStream = inputStream, rotation = rotation)
                                if (decoder != null) {
                                    item.intrinsicSize = IntSize(decoder.decoderWidth, decoder.decoderHeight)
                                }
                                model = decoder
                            }
                        }

                        MediaViewer(
                            modifier = Modifier.fillMaxSize(),
                            pagerState = state.pagerState,
                            page = page,
                            model = model,
                            state = viewerState,
                            boundClip = false,
                            detectGesture = {
                                this.onTap = {
                                    galleryGestureScope.onTap()
                                }
                                this.onDoubleTap = {
                                    val consumed = galleryGestureScope.onDoubleTap()
                                    if (!consumed) scope.launch {
                                        viewerState.toggleScale(it)
                                    }
                                }
                                this.onLongPress = { galleryGestureScope.onLongPress() }
                            },
                        )
                    }
                }
            }
        }
        galleryLayerScope.foreground(currentPage)
    }
}