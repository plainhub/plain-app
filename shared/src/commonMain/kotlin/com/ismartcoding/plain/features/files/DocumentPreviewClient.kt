package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.ManagedResource
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.readHttpResource
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeCall
import io.ktor.http.URLBuilder
import kotlinx.serialization.Serializable

internal object DocumentPreviewClient {
    suspend fun read(source: ContentSource, path: String): String {
        val resource = when (source) {
            ContentSource.Local -> {
                val access = source.api.execute(GraphqlRequest<PreviewAccess>(
                    "query { app { urlToken httpsPort tlsCertificateSha256 } }", field = "app",
                ))
                val id = nativeCall<String> {
                    RustNativeBindings.encryptResourceReference(access.urlToken, JsonHelper.jsonEncode(FileReference(path)))
                }
                val url = URLBuilder("https://127.0.0.1:${access.httpsPort}/fs").apply { parameters.append("id", id); parameters.append("action", FileAction.PREVIEW.queryValue) }.buildString()
                ManagedResource(url, checkNotNull(access.tlsCertificateSha256) { "HTTP server is unavailable" })
            }
            is ContentSource.Remote -> source.client.resource(path)
        }
        val response = readHttpResource(resource)
        check(response.status in 200..299) { "Document preview HTTP ${response.status}" }
        return response.body.decodeToString(throwOnInvalidSequence = true)
    }
}

@Serializable
private data class PreviewAccess(val urlToken: String, val httpsPort: Int, val tlsCertificateSha256: String?)

@Serializable
private data class FileReference(val path: String)
