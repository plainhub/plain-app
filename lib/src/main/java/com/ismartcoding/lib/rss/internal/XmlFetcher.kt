package com.ismartcoding.lib.rss.internal

import com.ismartcoding.lib.rss.HttpException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class XmlFetcher(
    private val callFactory: Call.Factory,
    private val charset: Charset?,
) {

    suspend fun fetchXml(url: String): ParserInput {
        val request = createRequest(url)
        return ParserInput(
            inputStream = callFactory.newCall(request).await()
        )
    }

    fun generateParserInputFromString(rawRssFeed: String): ParserInput {
        val inputStream: InputStream = rawRssFeed.byteInputStream(charset ?: Charsets.UTF_8)
        return ParserInput(inputStream)
    }

    private fun createRequest(url: String): Request =
        Request.Builder()
            .url(url)
            .build()

    private suspend fun Call.await(): InputStream = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }

        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = requireNotNull(response.body)
                    continuation.resume(body.byteStream())
                } else {
                    val exception = HttpException(
                        code = response.code,
                        message = response.message,
                    )
                    continuation.resumeWithException(exception)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}
