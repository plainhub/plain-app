package com.ismartcoding.plain.platform

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponseBody
import coil3.network.NetworkResponse
import coil3.request.crossfade
import io.ktor.utils.io.ByteReadChannel
import com.ismartcoding.plain.platform.copyTo
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * iOS image loader. Coil ships no network fetcher by default here, so we
 * register [NetworkFetcher.Factory] with our own [IosNetworkClient] backed by
 * [PlainHttpClient] (NSURLSession).
 */
fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(NetworkFetcher.Factory(networkClient = { IosNetworkClient() }))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory((appDir() + "/image_cache").toPath())
                .maxSizePercent(1.0)
                .build()
        }
        .crossfade(100)
        .build()

fun setupIosImageLoader() {
    SingletonImageLoader.setSafe { context -> newImageLoader(context) }
}

private class IosNetworkClient : NetworkClient {
    private val client: PlainHttpClient = createBrowserHttpClient()

    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T {
        val headers = request.headers.asMap().mapNotNull { (key, values) ->
            values.lastOrNull()?.let { key to it }
        }.toMap()
        val response = client.request(
            PlainRequest(
                method = request.method,
                url = request.url,
                headers = headers,
            ),
        )
        return response.use { plain ->
            val networkResponse = NetworkResponse(
                code = plain.status.value,
                headers = plain.headers.entries.fold(NetworkHeaders.Builder()) { builder, (key, values) ->
                    builder.apply { values.forEach { add(key, it) } }
                }.build(),
                body = ChannelResponseBody(plain.channel),
                delegate = plain,
            )
            block(networkResponse)
        }
    }
}

private class ChannelResponseBody(
    private val channel: ByteReadChannel,
) : NetworkResponseBody {
    override suspend fun writeTo(sink: BufferedSink) {
        channel.copyTo { buffer, length -> sink.write(buffer, 0, length) }
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        fileSystem.write(path) { writeTo(this) }
    }

    override fun close() {
        // The PlainResponse owning the channel is closed by the caller.
    }
}
