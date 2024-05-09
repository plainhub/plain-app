package com.ismartcoding.plain.ui.base.mediaviewer

import androidx.annotation.IntRange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.ismartcoding.plain.enums.ImageType
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.DEFAULT_ITEM_SPACE
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
        page: Int, viewerState: ImageViewerState, viewer: @Composable () -> Unit
    ) -> Unit = { _, _, viewer -> viewer() },
    // 背景图层
    var background: @Composable ((Int) -> Unit) = {},
    // 前景图层
    var foreground: @Composable ((Int) -> Unit) = {},
)

@OptIn(ExperimentalFoundationApi::class)
open class ImageGalleryState constructor(
    val pagerState: PagerState,
) {
    var imageViewerState by mutableStateOf<ImageViewerState?>(null)
        internal set
}

/**
 * 记录gallery状态
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberImageGalleryState(
    @IntRange(from = 0) initialPage: Int = 0,
    pageCount: () -> Int,
): ImageGalleryState {
    val imagePagerState = rememberPagerState(initialPage, pageCount = pageCount)
    return remember { ImageGalleryState(imagePagerState) }
}

/**
 * 图片gallery,基于Pager实现的一个图片查看列表组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGallery(
    // 编辑参数
    modifier: Modifier = Modifier,
    // gallery状态
    state: ImageGalleryState,
    // 图片加载器
    getItem: @Composable (Int) -> PreviewItem,
    // 每张图片之间的间隔
    itemSpacing: Dp = DEFAULT_ITEM_SPACE,
    // 检测手势
    detectGesture: GalleryGestureScope.() -> Unit = {},
    // gallery图层
    galleryLayer: GalleryLayerScope.() -> Unit = {},
) {
//    require(count >= 0) { "imageCount must be >= 0" }
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
            pageSpacing = itemSpacing,
        ) { page ->
            val imageState = rememberViewerState()
            LaunchedEffect(key1 = currentPage) {
                if (currentPage != page) imageState.reset()
                if (currentPage == page) {
                    state.imageViewerState = imageState
                }
            }
            galleryLayerScope.viewerContainer(page, imageState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    key(page) {
                        val item = getItem(page)
                        var model: Any? = null
                        if (item.size <= 2000 * 1000) {
                            // If the image size is less than 2MB, load the image directly
                            model = item
                        } else {
                            val imageType = remember { ImageHelper.getImageType(item.path) }
                            if (imageType.isApplicableAnimated() || imageType == ImageType.SVG) {
                                model = item
                            } else {
                                val inputStream = remember { File(item.path).inputStream() }
                                val rotation = remember {
                                    if (item.rotation == -1) {
                                        item.rotation = ImageHelper.getImageRotation(item.path)
                                    }
                                    item.rotation
                                }
                                model = rememberDecoderImagePainter(inputStream = inputStream, rotation = rotation)
                            }
                        }

                        ImageViewer(
                            modifier = Modifier.fillMaxSize(),
                            model = model,
                            state = imageState,
                            boundClip = false,
                            detectGesture = {
                                this.onTap = {
                                    galleryGestureScope.onTap()
                                }
                                this.onDoubleTap = {
                                    val consumed = galleryGestureScope.onDoubleTap()
                                    if (!consumed) scope.launch {
                                        imageState.toggleScale(it)
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