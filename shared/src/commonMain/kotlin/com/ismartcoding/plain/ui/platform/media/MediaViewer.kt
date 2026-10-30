package com.ismartcoding.plain.ui.platform.media



import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.media.viewer.GestureScope
import com.ismartcoding.plain.ui.media.viewer.MediaViewerState
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.rememberMediaViewerGesture
import com.ismartcoding.plain.ui.media.viewer.rememberViewerState
import com.ismartcoding.plain.ui.media.viewer.video.MediaVideo
import com.ismartcoding.plain.ui.media.viewer.video.VideoState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: PreviewItem,
    state: MediaViewerState = rememberViewerState(),
    gesture: GestureScope,
    boundClip: Boolean = true,
) {
    androidx.compose.runtime.LaunchedEffect(model.path) {
        try {
            model.prepareResource()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            com.ismartcoding.plain.ui.components.DialogHelper.showMessage(error)
            state.mountedFlow.emit(true)
        }
    }
    if (model.resourcePath.isEmpty()) return
    val scope = rememberCoroutineScope()
    val (rawGesture, sizeChange) = rememberMediaViewerGesture(state, gesture, scope)
    val imageRenderer = LocalImageRenderer.current
    Box(modifier = modifier) {
        val onMounted: () -> Unit = { scope.launch { state.mountedFlow.emit(true) } }
        if (model.isVideo()) {
            MediaVideo(
                pagerState = pagerState, videoState = videoState, page = page, model = model,
                scale = state.scale.value, offsetX = state.offsetX.value, offsetY = state.offsetY.value,
                rotation = state.rotation.value, gesture = rawGesture, onMounted = onMounted,
                onSizeChange = sizeChange, boundClip = boundClip,
            )
        } else if (imageRenderer != null) {
            imageRenderer.Content(
                model, state.scale.value, state.offsetX.value, state.offsetY.value,
                state.rotation.value, rawGesture, sizeChange, onMounted, boundClip,
            )
        } else {
            MediaNormalImage(
                model = model, scale = state.scale.value,
                offsetX = state.offsetX.value, offsetY = state.offsetY.value,
                rotation = state.rotation.value, gesture = rawGesture,
                onSizeChange = sizeChange, onMounted = onMounted, boundClip = boundClip,
            )
        }
    }
}
