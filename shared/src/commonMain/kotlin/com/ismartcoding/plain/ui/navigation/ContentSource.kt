package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.AnimatedContentScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ismartcoding.plain.rust.graphql.ContentSource

internal val LocalContentSource = staticCompositionLocalOf<ContentSource> { ContentSource.Local }

internal inline fun <reified T : Any> NavGraphBuilder.contentDestination(
    noinline source: @Composable () -> ContentSource?,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        source()?.let { value ->
            CompositionLocalProvider(LocalContentSource provides value) { content(entry) }
        }
    }
}

@Composable
internal fun rememberContentResource(
    source: ContentSource,
    path: String,
    size: com.ismartcoding.plain.features.chat.peer.ResourceSize? = null,
): androidx.compose.runtime.State<Result<Any>?> {
    if (source is ContentSource.Local) return androidx.compose.runtime.rememberUpdatedState(Result.success(path))
    return androidx.compose.runtime.produceState<Result<Any>?>(null, source, path, size) {
        value = try {
            Result.success((source as ContentSource.Remote).client.resource(path, size))
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
