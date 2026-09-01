package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.db.AppFileDao
import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.copyFile
import com.ismartcoding.plain.platform.deleteFileAt
import com.ismartcoding.plain.platform.ensureDir
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.fileSize
import com.ismartcoding.plain.platform.getExtensionFromMimeType
import com.ismartcoding.plain.platform.getMimeTypeFromExtension
import com.ismartcoding.plain.platform.moveFile
import com.ismartcoding.plain.platform.sha256File
import com.ismartcoding.plain.platform.sha256FileEdges
import com.ismartcoding.plain.platform.writeBytesToPath

/**
 * Content-addressable store for chat files.
 *
 * Storage layout inside the app's external-files directory:
 *   {appDir}/{hash[0..1]}/{hash[2..3]}/{hash}.{ext}   (lowercase extension)
 *
 * URI scheme used in [com.ismartcoding.plain.db.DMessageFile.uri]:
 *   fid:{sha256hex}.{ext}   (extension derived from MIME type, lowercase)
 *
 * The fidSuffix (part after "fid:") encodes both the hash and extension so
 * path resolution never needs a database query.
 *
 * The `app_files.real_path` column stores the relative portion
 * (`{aa}/{bb}/{name}`) to avoid repeating the platform-specific `appDir()`
 * prefix on every row. Use [relativeDestPath] when persisting and
 * [realPathFromId] (or [resolveUri]) when an absolute path is required.
 *
 * All lowest-level file operations (copy/move/size/hash/ensureDir) are
 * delegated to the [platform primitives][com.ismartcoding.plain.platform]
 * so the store itself is platform-independent.
 */
object AppFileStore {
    /** Convert a SHA-256 hash and optional lowercase extension into a [fid:] URI. */
    fun toFidUri(fileId: String, ext: String = ""): String =
        if (ext.isNotEmpty()) "fid:$fileId.$ext" else "fid:$fileId"

    /** [fid:] URI for an imported record — derived from its stored [DAppFile.realPath]
     *  so the URI always matches the on-disk file name (whose extension may have
     *  come from the original upload file name rather than the MIME type). */
    fun toFidUri(dFile: DAppFile): String = "fid:" + dFile.realPath.substringAfterLast('/')

    /** Derive extension from a MIME type string (lowercase, empty string if unknown). */
    fun extFromMime(mimeType: String): String {
        if (mimeType.isEmpty()) return ""
        return getExtensionFromMimeType(mimeType).lowercase()
    }

    /**
     * File-name extension (lowercased), falling back to the MIME-derived one.
     * The original file name is the ground truth for the extension: clients
     * often send an empty or generic Content-Type for less-common extensions
     * (`properties`, `apk`, …), and the chunked-merge path has no MIME at all.
     * Only when the name carries no extension do we consult the MIME type.
     */
    fun extFromFileName(fileName: String, mimeType: String): String {
        val dot = fileName.lastIndexOf('.')
        val nameExt = if (dot > 0) fileName.substring(dot + 1).lowercase() else ""
        return nameExt.ifEmpty { extFromMime(mimeType) }
    }

    /**
     * Relative portion of the canonical storage path for a `{hash, ext}` pair —
     * `{aa}/{bb}/{name}` — stored in `app_files.real_path` so the column
     * doesn't repeat the platform-specific `appDir()` prefix on every row.
     */
    fun relativeDestPath(hash: String, ext: String = ""): String {
        val name = if (ext.isNotEmpty()) "$hash.$ext" else hash
        return "${hash.substring(0, 2)}/${hash.substring(2, 4)}/$name"
    }

    /**
     * Derive the absolute file-system path from a fidSuffix (the part after "fid:").
     * fidSuffix may be "{hash}" (legacy) or "{hash}.{ext}" (current).
     * Returns the absolute path whether or not the file currently exists.
     */
    fun realPathFromId(fidSuffix: String): String {
        val hash = fidSuffix.substringBefore(".")
        val base = appDir()
        return "$base/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$fidSuffix"
    }

    /**
     * Resolve a URI that may be:
     *   - "fid:{hash}.{ext}" → derived real path (no DB query)
     *   - "fid:{hash}"       → legacy path without extension (no DB query)
     *   - "app://{rel}"      → existing app:// resolution handled by getFinalPath
     *   - absolute path      → returned as-is
     */
    fun resolveUri(uri: String): String {
        if (uri.startsWith("fid:", ignoreCase = true)) {
            return realPathFromId(uri.removePrefix("fid:"))
        }
        return uri
    }

    // ── Import (dedup entry point) ──────────────────────────────────────────

    /**
     * Import a file into the store with two-step dedup.
     *
     * 1. Fast weak check  (size + edge hash)
     * 2. Full SHA-256 check only when weak matches.
     *
     * - If an identical file already exists, increments refCount and returns
     *   the existing [DAppFile].
     * - Otherwise copies/moves [srcPath] into the store directory and inserts
     *   a new [DAppFile] row.
     *
     * @param srcPath    Source file path to import. Caller retains ownership; this
     *                   method copies the content (does not delete srcFile).
     * @param fileName   Original upload file name — its extension is the primary
     *                   source for the on-disk extension.
     * @param mimeType   Optional MIME type override. Guessed from extension if
     *                   blank.
     * @param deleteSrc  When true the source file is deleted after a successful
     *                   copy (move semantics).
     */
    suspend fun importFile(
        srcPath: String,
        fileName: String = "",
        mimeType: String = "",
        deleteSrc: Boolean = false,
    ): DAppFile = withIO {
        val dao = AppDatabase.instance.appFileDao()
        val size = fileSize(srcPath)
        val strongHash = sha256File(srcPath)

        // ── Step 1: weak check ────────────────────────────────────────────
        val weakHash = sha256FileEdges(srcPath, size)
        val candidates = dao.findByWeakKey(size, weakHash)

        if (candidates.isNotEmpty()) {
            // ── Step 2: strong check ──────────────────────────────────────
            tryReuseExisting(dao, srcPath, strongHash, deleteSrc)?.let { return@withIO it }
            // Weak matched but strong differs – fall through to insert new
            return@withIO insertNew(dao, srcPath, size, weakHash, strongHash, fileName, mimeType, deleteSrc)
        }

        // No weak match. Double-check by id in case another thread raced us.
        tryReuseExisting(dao, srcPath, strongHash, deleteSrc)?.let { return@withIO it }

        insertNew(dao, srcPath, size, weakHash, strongHash, fileName, mimeType, deleteSrc)
    }

    /**
     * Import raw bytes (e.g. a completed download) into the store.
     */
    suspend fun importBytes(
        data: ByteArray,
        mimeType: String = "",
    ): DAppFile = withIO {
        val dao = AppDatabase.instance.appFileDao()
        val size = data.size.toLong()
        val strongHash = FileHashHelper.strongHash(data)

        val existing = dao.getById(strongHash)
        if (existing != null) {
            dao.incrementRefCount(strongHash)
            return@withIO existing
        }

        // Compute weak hash from the same data
        val weakHash = FileHashHelper.weakHash(data)

        val effectiveMime = mimeType.ifEmpty { "application/octet-stream" }
        val destPath = destPath(strongHash, extFromMime(effectiveMime))
        ensureParentFor(destPath)
        writeBytesToPath(destPath, data)

        insertRecord(dao, strongHash, size, weakHash, "", effectiveMime)
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private fun destPath(hash: String, ext: String = ""): String {
        val base = appDir()
        return "$base/${relativeDestPath(hash, ext)}"
    }

    private suspend fun tryReuseExisting(
        dao: AppFileDao,
        srcPath: String,
        strongHash: String,
        deleteSrc: Boolean,
    ): DAppFile? {
        val existing = dao.getById(strongHash) ?: return null
        val targetPath = "${appDir()}/${existing.realPath}"

        // DB row may exist while the backing file was deleted; restore it.
        if (!fileExists(targetPath)) {
            // Check if old file without extension exists (pre-migration) and rename it
            val legacyPath = destPath(strongHash)
            if (legacyPath != targetPath && fileExists(legacyPath)) {
                moveFile(legacyPath, targetPath)
                LogCat.d("ChatFileStore: renamed legacy file $strongHash to include extension")
            } else {
                storeSourceFile(srcPath, targetPath, deleteSrc)
                LogCat.d("ChatFileStore: restored missing file $strongHash")
            }
        } else if (deleteSrc) {
            deleteFileAt(srcPath)
        }

        dao.incrementRefCount(strongHash)
        existing.refCount += 1
        LogCat.d("ChatFileStore: reusing file $strongHash (refCount ${existing.refCount})")
        return existing
    }

    private fun storeSourceFile(srcPath: String, destPath: String, deleteSrc: Boolean) {
        ensureParentFor(destPath)
        if (deleteSrc) {
            // renameTo is atomic but fails silently across mount points
            // (e.g. cacheDir → getExternalFilesDir()). Fall back to copy+delete.
            if (!moveFile(srcPath, destPath)) {
                copyFile(srcPath, destPath)
                deleteFileAt(srcPath)
            }
        } else {
            copyFile(srcPath, destPath)
        }
    }

    private suspend fun insertNew(
        dao: AppFileDao,
        srcPath: String,
        size: Long,
        weakHash: String,
        strongHash: String,
        fileName: String,
        mimeType: String,
        deleteSrc: Boolean,
    ): DAppFile {
        val effectiveMime = mimeType.ifEmpty {
            val srcExt = fileName.ifEmpty { srcPath }.getFilenameExtension()
            getMimeTypeFromExtension(srcExt).ifEmpty { "application/octet-stream" }
        }
        val dest = destPath(strongHash, extFromFileName(fileName, effectiveMime))
        storeSourceFile(srcPath, dest, deleteSrc)

        val record = insertRecord(dao, strongHash, size, weakHash, fileName, effectiveMime)
        LogCat.d("ChatFileStore: stored new file $strongHash (${size} bytes)")
        return record
    }

    private suspend fun insertRecord(
        dao: AppFileDao,
        strongHash: String,
        size: Long,
        weakHash: String,
        fileName: String,
        effectiveMime: String,
    ): DAppFile {
        val record = DAppFile(strongHash).apply {
            this.size = size
            this.mimeType = effectiveMime
            this.realPath = relativeDestPath(strongHash, extFromFileName(fileName, effectiveMime))
            this.refCount = 1
            this.weakHash = weakHash
        }
        dao.insert(record)
        return record
    }

    private fun ensureParentFor(path: String) {
        val parent = path.substringBeforeLast('/', "")
        if (parent.isNotEmpty()) {
            ensureDir(parent)
        }
    }
}