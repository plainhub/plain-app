package com.ismartcoding.plain.platform.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.ismartcoding.plain.models.ManagedResource
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.io.EOFException

@OptIn(UnstableApi::class)
internal class PeerResourceDataSource(private val resource: ManagedResource) : BaseDataSource(true) {
    private val client = com.ismartcoding.plain.rust.graphql.createPeerResourceHttpClient(resource)
    private var call: Call? = null
    private var response: Response? = null
    private var input: InputStream? = null
    private var uri: Uri? = null
    private var remaining = C.LENGTH_UNSET.toLong()
    private var started = false

    override fun open(dataSpec: DataSpec): Long {
        check(dataSpec.uri.toString() == resource.url) { "Playback resource changed" }
        transferInitializing(dataSpec)
        val request = Request.Builder().url(dataSpec.uri.toString())
        dataSpec.httpRequestHeaders.forEach { (key, value) -> request.header(key, value) }
        if (dataSpec.position != 0L || dataSpec.length != C.LENGTH_UNSET.toLong()) {
            val end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) "" else (dataSpec.position + dataSpec.length - 1).toString()
            request.header("Range", "bytes=${dataSpec.position}-$end")
        }
        uri = dataSpec.uri
        val activeCall = client.newCall(request.build())
        call = activeCall
        try {
            val received = activeCall.execute()
            response = received
            if (!received.isSuccessful || dataSpec.position > 0 && received.code != 206) {
                throw IOException("Peer resource HTTP ${received.code}")
            }
            input = received.body.byteStream()
            remaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else received.body.contentLength()
            started = true
            transferStarted(dataSpec)
            return remaining
        } catch (error: Exception) {
            close()
            throw error
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val count = if (remaining == C.LENGTH_UNSET.toLong()) length else minOf(length.toLong(), remaining).toInt()
        val read = checkNotNull(input).read(buffer, offset, count)
        if (read == -1) {
            if (remaining > 0) throw EOFException("Peer resource ended early")
            return C.RESULT_END_OF_INPUT
        }
        if (remaining != C.LENGTH_UNSET.toLong()) remaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri
    override fun getResponseHeaders(): Map<String, List<String>> = response?.headers?.toMultimap().orEmpty()

    override fun close() {
        response?.close()
        call?.cancel()
        response = null
        call = null
        input = null
        uri = null
        if (started) {
            started = false
            transferEnded()
        }
    }
}
