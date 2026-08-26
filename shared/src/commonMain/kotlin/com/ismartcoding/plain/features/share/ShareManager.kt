package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.db.ShareRoot
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.httpserver.ExpiringCache
import com.ismartcoding.plain.httpserver.ShareFileParams
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.getCanonicalPath
import com.ismartcoding.plain.platform.statFile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

/** Cached per-share auth: the share row plus its derived `shared_token`. */
data class SharedAuth(
    val share: DShare,
    val token: ByteArray,
)

/**
 * Business logic for creating, managing, and resolving shared file links.
 *
 * Responsibilities:
 * - persist `shares` (whitelisted roots are stored as JSON in `data`)
 * - derive the link (`/s/<shared_id>#<shared_token>`)
 * - resolve a requested `virtualPath` to a real device path, enforcing the
 *   root whitelist (no arbitrary path reads)
 */
object ShareManager {
    private const val SHARE_TTL_MS = 24 * 60 * 60 * 1000L // 24h
    private const val NEGATIVE_TTL_MS = 5 * 1000L // 5s unknown-share negative TTL

    /**
     * Cached guest-share auth: the share snapshot plus its derived `shared_token`,
     * keyed by `shared_id`. Loads lazily from the DB and derives the token once,
     * so per-request `/guest_graphql` auth avoids both a DB read and an HMAC.
     *
     * [DShare.isActive] is (re)evaluated from the cached snapshot's `expiresAt`
     * at request time, so time-based expiration stays exact. Revocation via
     * [deleteShare] invalidates the entry immediately.
     */
    val authCache = ExpiringCache<String, SharedAuth>(
        positiveTtlMillis = SHARE_TTL_MS,
        negativeTtlMillis = NEGATIVE_TTL_MS,
        load = { id -> loadAuth(id) },
    )

    private suspend fun loadAuth(id: String): SharedAuth? {
        val share = AppDatabase.instance.shareDao().getById(id) ?: return null
        return SharedAuth(share, ShareCrypto.deriveSharedToken(id))
    }

    suspend fun createShare(
        name: String,
        realPaths: List<String>,
        urlToken: String,
        readOnly: Boolean,
        expiresAt: Instant?,
    ): DShare {
        val shareId = ShareCrypto.newSharedId()
        val now = TimeHelper.now()
        val share = DShare(id = shareId).apply {
            this.name = name
            this.urlToken = urlToken
            this.readOnly = readOnly
            this.expiresAt = expiresAt
            this.data = realPaths.map { realPath ->
                val stat = statFile(realPath)
                ShareRoot(
                    virtualPath = realPath.substringAfterLast('/') + (if (stat?.isDir == true) "/" else ""),
                    realPath = realPath,
                    isDir = stat?.isDir ?: false,
                )
            }
            createdAt = now
            updatedAt = now
        }
        AppDatabase.instance.shareDao().insert(share)
        return share
    }

    /** Update the editable fields of a share (name / expiry). Returns null if the id is unknown. */
    suspend fun updateShare(id: String, name: String, expiresAt: Instant?): DShare? {
        val dao = AppDatabase.instance.shareDao()
        val share = dao.getById(id) ?: return null
        share.name = name
        share.expiresAt = expiresAt
        share.updatedAt = TimeHelper.now()
        dao.update(share)
        authCache.invalidate(id)
        return share
    }

    suspend fun deleteShare(id: String) {
        AppDatabase.instance.shareDao().delete(id)
        authCache.invalidate(id)
    }

    suspend fun listShares(): List<DShare> {
        return AppDatabase.instance.shareDao().getAll()
    }

    suspend fun getShare(id: String): DShare? {
        return AppDatabase.instance.shareDao().getById(id)
    }

    /**
     * Build the share link:
     * `https://<host>:<httpsPort>/s/<shared_id>#<shared_token>`.
     */
    suspend fun buildLink(share: DShare, host: String = getHost()): String {
        val sharedToken = ShareCrypto.deriveSharedTokenEncoded(share.id)
        return "https://$host:${TempData.httpsPort.value}/s/${share.id}#$sharedToken"
    }

    private fun getHost(): String {
        return TempData.mdnsHostname
    }

    /**
     * Resolve a requested [virtualPath] (e.g. `假期照片/IMG_1.jpg`) to the real
     * device path. Enforces that the path is under one of the share's roots.
     * Returns null if the path is outside any root or the share is invalid.
     */
    fun resolveVirtualPath(share: DShare, virtualPath: String): String? {
        if (!share.isActive) return null
        val normalized = virtualPath.trim().trimStart('/')
        val roots = share.data
        // "/" or empty requests the root itself.
        if (normalized.isEmpty()) {
            return roots.firstOrNull()?.realPath
        }
        val sep = normalized.indexOf('/')
        val top = if (sep == -1) normalized else normalized.substring(0, sep)
        val rest = if (sep == -1) "" else normalized.substring(sep + 1)
        val root = roots.firstOrNull { it.virtualPath.trimEnd('/') == top } ?: return null
        val realRoot = getCanonicalPath(root.realPath.trimEnd('/'))
        val candidate = getCanonicalPath(resolveUnder(realRoot, rest))
        // Block traversal outside the root.
        return if (isUnder(realRoot, candidate)) candidate else null
    }

    /**
     * Authenticate a guest `/fs` / `/zip/dir` request and resolve [id]
     * (a [ShareFileParams] payload ChaCha20-encrypted with the share's
     * `url_token`, [sid] carries the public `shared_id`) to a real device
     * path. Returns null when the request is not authorized: service off,
     * unknown/inactive share, id bound to a different share, or a virtual
     * path outside the roots.
     */
    suspend fun resolveSharedPath(sid: String, id: String): String? {
        if (!TempData.serviceEnabled.value) return null
        val share = authCache.get(sid)?.share ?: return null
        if (!share.isActive) return null
        return try {
            @OptIn(ExperimentalEncodingApi::class)
            val key = Base64.decode(share.urlToken)
            val params = jsonDecode<ShareFileParams>(UrlHelper.decrypt(id, key))
            if (params.sharedId != sid) null else resolveVirtualPath(share, params.virtualPath)
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Collapse `.` and `..` segments relative to [root] purely in commonMain so
     * a `..` can never escape the root, independent of how the platform
     * canonicalizes paths (e.g. iOS `getCanonicalPath` is identity).
     */
    private fun resolveUnder(root: String, relPath: String): String {
        val segments = mutableListOf<String>()
        root.split('/').filter { it.isNotEmpty() }.forEach { segments.add(it) }
        relPath.split('/').forEach { seg ->
            when (seg) {
                "", "." -> {}
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(seg)
            }
        }
        return "/" + segments.joinToString("/")
    }

    private fun isUnder(root: String, candidate: String): Boolean {
        return candidate == root || candidate.startsWith("$root/")
    }
}
