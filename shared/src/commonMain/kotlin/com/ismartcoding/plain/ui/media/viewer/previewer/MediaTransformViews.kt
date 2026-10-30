package com.ismartcoding.plain.ui.media.viewer.previewer

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.platform.media.applyForceVideoDecoder
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_video as ui_drawable_file_video
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.i18n.image

@Composable
fun TransformImageViewWithUri(
    modifier: Modifier = Modifier,
    path: String, fileName: String, key: String, uri: String? = null,
    itemState: TransformItemState, previewerState: MediaPreviewerState,
    widthPx: Int, forceVideoDecoder: Boolean = false,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val resource by com.ismartcoding.plain.ui.navigation.rememberContentResource(source, path, widthPx)
    TransformImageView(modifier = modifier, key = key, itemState = itemState, contentState = previewerState.transformState) { itemKey ->
        key(itemKey) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) uri ?: path else resource?.getOrNull()).size(widthPx)
                    .applyForceVideoDecoder(forceVideoDecoder && source is com.ismartcoding.plain.rust.graphql.ContentSource.Local).build(),
                filterQuality = FilterQuality.None
            )
            val imageState by painter.state.collectAsState()
            LaunchedEffect(imageState) {
                if (imageState is AsyncImagePainter.State.Success) itemState.updateIntrinsicSize(painter.intrinsicSize)
            }
            val imageModifier = Modifier.fillMaxSize()
            if (resource?.isFailure == true || painter.state.value is AsyncImagePainter.State.Error) {
                Image(
                    modifier = imageModifier,
                    painter = painterResource(if (fileName.isImageFast()) UiRes.drawable.ui_drawable_image else UiRes.drawable.ui_drawable_file_video),
                    contentDescription = path,
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    modifier = if (fileName.endsWith(".svg", true)) imageModifier.background(Color.White) else imageModifier,
                    painter = painter,
                    contentDescription = path,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun TransformImageView(
    modifier: Modifier = Modifier, key: Any, itemState: TransformItemState = rememberTransformItemState(),
    previewerState: MediaPreviewerState, content: @Composable (Any) -> Unit,
) = TransformImageView(modifier, key, itemState, previewerState.transformState, content)

@Composable
fun TransformImageView(
    modifier: Modifier = Modifier, key: Any, itemState: TransformItemState = rememberTransformItemState(),
    contentState: TransformContentState? = rememberTransformContentState(), content: @Composable (Any) -> Unit,
) {
    TransformItemView(modifier = modifier, key = key, itemState = itemState, contentState = contentState) { content(key) }
}

@Composable
fun TransformItemView(
    modifier: Modifier = Modifier, key: Any, itemState: TransformItemState = rememberTransformItemState(),
    contentState: TransformContentState?, content: @Composable (Any) -> Unit,
) {
    val scope = rememberCoroutineScope()
    itemState.key = key; itemState.blockCompose = content
    DisposableEffect(key) {
        scope.launch { itemState.addItem() }
        onDispose { itemState.removeItem() }
    }
    Box(
        modifier = modifier
            .onGloballyPositioned { itemState.onPositionChange(position = it.positionInRoot(), size = it.size) }
            .fillMaxSize()) {
        if (contentState?.itemState != itemState || !contentState.onAction) {
            itemState.blockCompose(key)
        }
    }
}

@Composable
fun TransformContentView(transformContentState: TransformContentState = rememberTransformContentState()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                transformContentState.containerSize = it.size
                transformContentState.containerOffset = it.positionInRoot()
            }) {
        if (transformContentState.srcCompose != null && transformContentState.onAction) {
            Box(
                modifier = Modifier
                    .offset(
                        x = LocalDensity.current.run { transformContentState.offsetX.value.toDp() },
                        y = LocalDensity.current.run { transformContentState.offsetY.value.toDp() })
                    .size(
                        width = LocalDensity.current.run { transformContentState.displayWidth.value.toDp() },
                        height = LocalDensity.current.run { transformContentState.displayHeight.value.toDp() })
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0F, 0F)
                        scaleX = transformContentState.graphicScaleX.value
                        scaleY = transformContentState.graphicScaleY.value
                    }
            ) { transformContentState.srcCompose!!(transformContentState.itemState?.key ?: Unit) }
        }
    }
}
