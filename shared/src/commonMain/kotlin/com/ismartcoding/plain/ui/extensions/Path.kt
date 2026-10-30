package com.ismartcoding.plain.ui.extensions

import androidx.compose.runtime.*
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.features.files.FilePaths
import com.ismartcoding.plain.features.files.FilePathResolver
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import kotlinx.coroutines.CancellationException

suspend fun String.getFinalPath(): String = FilePaths.resolve(this)

@Composable
fun rememberFilePath(reference: String, source: ContentSource = LocalContentSource.current): String {
    val external = reference.startsWith("https://", true) || reference.startsWith("http://", true) || reference.startsWith("data:", true)
    val path by produceState(if (source is ContentSource.Remote && !external || FilePathResolver.needsResolution(reference)) "" else reference, reference, source) {
        value = try {
            if (source is ContentSource.Remote && !external) source.client.resource(reference).url else FilePaths.resolve(reference)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { LogCat.e(error.toString()); "" }
    }
    return path
}

@Composable
fun rememberImageResource(reference: String, source: ContentSource = LocalContentSource.current): Any? {
    val external = reference.startsWith("https://", true) || reference.startsWith("http://", true) || reference.startsWith("data:", true)
    val initial: Any? = if (external || source is ContentSource.Local && !FilePathResolver.needsResolution(reference)) reference else null
    val value by produceState<Any?>(initial, reference, source) {
        value = try {
            if (source is ContentSource.Remote && !external) source.client.resource(reference) else FilePaths.resolve(reference)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { LogCat.e(error.toString()); null }
    }
    return value
}
