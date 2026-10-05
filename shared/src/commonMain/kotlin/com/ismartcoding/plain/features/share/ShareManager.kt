package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.db.ShareRoot
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.time.Instant

object ShareManager {
    private const val FIELDS = "id name password urlToken readOnly expiresAt createdAt updatedAt roots { virtualPath realPath isDir }"

    suspend fun createShare(name: String, realPaths: List<String>, urlToken: String, readOnly: Boolean, expiresAt: Instant?): DShare =
        RustContentApi.mutate("createShare(name: ${gql(name)}, realPaths: ${gqlIds(realPaths)}, urlToken: ${gql(urlToken)}, readOnly: $readOnly, expiresAt: ${expiry(expiresAt)}) { $FIELDS }")
            .getValue("createShare").share()

    suspend fun updateShare(id: String, name: String, expiresAt: Instant?, realPaths: List<String>? = null): DShare =
        RustContentApi.mutate("updateShare(id: ${gql(id)}, name: ${gql(name)}, expiresAt: ${expiry(expiresAt)}, realPaths: ${realPaths?.let(::gqlIds) ?: "null"}) { $FIELDS }")
            .getValue("updateShare").share()

    suspend fun deleteShare(id: String) { RustContentApi.mutate("deleteShare(id: ${gql(id)})") }
    suspend fun listShares(): List<DShare> = RustContentApi.query("shareRecords { $FIELDS }").getValue("shareRecords").jsonArray.map { it.share() }
    suspend fun getShare(id: String): DShare? = RustContentApi.query("shareRecord(id: ${gql(id)}) { $FIELDS }")["shareRecord"]?.takeUnless { it is JsonNull }?.share()
    suspend fun sharedToken(id: String): String = RustContentApi.query("shareToken(id: ${gql(id)})").string("shareToken")

    suspend fun buildLink(share: DShare, host: String? = null): String = SharedLinkClient.pageUrl(SharedLinkClient.ownLink(share.id, host))

    suspend fun loadAuth(id: String): SharedAuth? {
        val auth = RustContentApi.query("shareAuth(id: ${gql(id)}) { token share { $FIELDS } }")["shareAuth"]?.takeUnless { it is JsonNull }?.jsonObject ?: return null
        return SharedAuth(auth.getValue("share").share(), Base64.UrlSafe.decode(auth.string("token")))
    }
    suspend fun resolveVirtualPath(share: DShare, virtualPath: String): String? =
        RustContentApi.query("resolveSharePath(id: ${gql(share.id)}, virtualPath: ${gql(virtualPath)})")["resolveSharePath"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

    suspend fun resolveSharedPath(sid: String, id: String): String? =
        RustContentApi.query("resolveShareFile(id: ${gql(sid)}, fileId: ${gql(id)})")["resolveShareFile"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

    suspend fun browse(id: String, virtualPath: String): Pair<DShare, List<ShareRoot>> {
        val row = RustContentApi.query("browseShare(id: ${gql(id)}, virtualPath: ${gql(virtualPath)}) { share { $FIELDS } entries { virtualPath realPath isDir } }").getValue("browseShare").jsonObject
        return row.getValue("share").share() to row.getValue("entries").jsonArray.map { it.root() }
    }
    private fun JsonElement.root(): ShareRoot = jsonObject.let { ShareRoot(it.string("virtualPath"), it.string("realPath"), it.getValue("isDir").jsonPrimitive.boolean) }
    suspend fun zipEntries(id: String, fileId: String): List<com.ismartcoding.plain.platform.ZipStreamEntry> =
        RustContentApi.query("shareZipEntries(id: ${gql(id)}, fileId: ${gql(fileId)}) { virtualPath realPath }").getValue("shareZipEntries").jsonArray.map {
            it.jsonObject.let { row -> com.ismartcoding.plain.platform.ZipStreamEntry(row.string("realPath"), row.string("virtualPath")) }
        }
    private fun expiry(value: Instant?): String = value?.let { gql(it.toString()) } ?: "null"
    private fun JsonElement.share(): DShare = jsonObject.let { row ->
        DShare(id = row.string("id"), name = row.string("name"), password = row.string("password"), urlToken = row.string("urlToken"),
            readOnly = row.getValue("readOnly").jsonPrimitive.boolean, expiresAt = row.nullableInstant("expiresAt"),
            createdAt = row.instant("createdAt"), updatedAt = row.instant("updatedAt"),
            data = row.getValue("roots").jsonArray.map { it.root() })
    }
}
