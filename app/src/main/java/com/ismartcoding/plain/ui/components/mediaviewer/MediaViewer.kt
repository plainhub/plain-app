package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.ImageDecoder
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.MediaHugeImage
import com.ismartcoding.plain.ui.components.mediaviewer.video.MediaVideo
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: Any?,
    state: MediaViewerState = rememberViewerState(),
    gesture: GestureScope,
    boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val (rawGesture, sizeChange) = rememberMediaViewerGesture(state, gesture, scope)

    Box(modifier = modifier) {
        val onMounted: () -> Unit = { scope.launch { state.mountedFlow.emit(true) } }

        when (model) {
            is PreviewItem -> {
                if (model.isVideo() && !model.path.isUrl()) {
                    MediaVideo(
                        pagerState = pagerState, videoState = videoState, page = page, model = model,
                        scale = state.scale.value, offsetX = state.offsetX.value, offsetY = state.offsetY.value,
                        rotation = state.rotation.value, gesture = rawGesture, onMounted = onMounted,
                        onSizeChange = sizeChange, boundClip = boundClip,
                    )
                } else {
                    MediaNormalImage(
                        model = model, scale = state.scale.value, offsetX = state.offsetX.value,
                        offsetY = state.offsetY.value, rotation = state.rotation.value, gesture = rawGesture,
                        onSizeChange = sizeChange, onMounted = onMounted, boundClip = boundClip,
                    )
                }
            }
            is ImageDecoder -> {
                MediaHugeImage(
                    imageDecoder = model, scale = state.scale.value, offsetX = state.offsetX.value,
                    offsetY = state.offsetY.value, rotation = state.rotation.value, gesture = rawGesture,
                    onSizeChange = sizeChange, onMounted = onMounted, boundClip = boundClip,
                )
            }
        }
    }
}
