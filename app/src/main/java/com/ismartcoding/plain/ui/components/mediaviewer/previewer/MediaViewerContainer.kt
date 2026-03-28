package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.rememberViewerState
import kotlinx.coroutines.*

internal class ViewerContainerState(
    var scope: CoroutineScope = MainScope(),
    var transformState: TransformContentState = TransformContentState(),
    var viewerState: MediaViewerState = MediaViewerState(),
) {
    internal var transformContentAlpha = Animatable(0F)
    internal var viewerContainerAlpha = Animatable(1F)
    internal var showLoading by mutableStateOf(true)
    internal var openTransformJob: Deferred<Unit>? = null

    internal fun cancelOpenTransform() { openTransformJob?.cancel(); openTransformJob = null }

    internal suspend fun awaitOpenTransform() = doAwaitOpenTransform()

    internal suspend fun transformSnapToViewer(isViewer: Boolean) {
        if (isViewer) { transformContentAlpha.snapTo(0F); viewerContainerAlpha.snapTo(1F) }
        else { transformContentAlpha.snapTo(1F); viewerContainerAlpha.snapTo(0F) }
    }

    internal suspend fun copyViewerContainerStateToTransformState() = doCopyViewerContainerStateToTransformState()
    internal suspend fun copyViewerPosToContent(itemState: TransformItemState) = doCopyViewerPosToContent(itemState)

    var containerSize: IntSize by mutableStateOf(IntSize.Zero)
    var offsetX = Animatable(0F)
    var offsetY = Animatable(0F)
    var scale = Animatable(1F)

    suspend fun reset(animationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC) {
        scope.apply {
            listOf(async { offsetX.animateTo(0F, animationSpec) }, async { offsetY.animateTo(0F, animationSpec) },
                async { scale.animateTo(1F, animationSpec) }).awaitAll()
        }
    }

    suspend fun resetImmediately() { offsetX.snapTo(0F); offsetY.snapTo(0F); scale.snapTo(1F) }

    companion object {
        val Saver: Saver<ViewerContainerState, *> = mapSaver(
            save = { mapOf<String, Any>(it::offsetX.name to it.offsetX.value, it::offsetY.name to it.offsetY.value, it::scale.name to it.scale.value) },
            restore = {
                ViewerContainerState().also { s ->
                    s.offsetX = Animatable(it[s::offsetX.name] as Float)
                    s.offsetY = Animatable(it[s::offsetY.name] as Float)
                    s.scale = Animatable(it[s::scale.name] as Float)
                }
            }
        )
    }
}

@Composable
internal fun rememberViewerContainerState(
    scope: CoroutineScope = rememberCoroutineScope(),
    viewerState: MediaViewerState = rememberViewerState(),
    transformContentState: TransformContentState = rememberTransformContentState(),
): ViewerContainerState {
    val state = rememberSaveable(saver = ViewerContainerState.Saver) { ViewerContainerState() }
    state.scope = scope; state.viewerState = viewerState; state.transformState = transformContentState
    return state
}

@Composable
internal fun MediaViewerContainer(
    modifier: Modifier = Modifier, containerState: ViewerContainerState,
    placeholder: PreviewerPlaceholder = PreviewerPlaceholder(), viewer: @Composable () -> Unit,
) {
    containerState.apply {
        Box(modifier = modifier.fillMaxSize().onGloballyPositioned { containerSize = it.size }
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value; translationX = offsetX.value; translationY = offsetY.value }
        ) {
            Box(modifier = Modifier.fillMaxSize().alpha(transformContentAlpha.value)) { TransformContentView(transformState) }
            Box(modifier = Modifier.fillMaxSize().alpha(viewerContainerAlpha.value)) { viewer() }
            val viewerMounted by viewerState.mountedFlow.collectAsState(initial = false)
            if (showLoading) {
                AnimatedVisibility(visible = !viewerMounted, enter = placeholder.enterTransition, exit = placeholder.exitTransition) { placeholder.content() }
            }
        }
    }
}
