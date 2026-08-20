package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.db.ShareRoot
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.httpserver.ExpiringCache
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.getCanonicalPath
import com.ismartcoding.plain.platform.statFile
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

    suspend fun deleteShare(id: String) {
        AppDatabase.instance.shareDao().delete(id)
        authCache.invalidate(id)
    }

    suspend fun listShares(): List<DShare> {
        return AppDatabase.instance.shareDao().getAll()
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
    suspend fun resolveVirtualPath(share: DShare, virtualPath: String): String? {
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
        val candidate = getCanonicalPath(
            if (rest.isEmpty()) root.realPath else "${root.realPath.trimEnd('/')}/$rest",
        )
        // Block traversal outside the root.
        return if (isUnder(realRoot, candidate)) candidate else null
    }

    private fun isUnder(root: String, candidate: String): Boolean {
        return candidate == root || candidate.startsWith("$root/")
    }
}