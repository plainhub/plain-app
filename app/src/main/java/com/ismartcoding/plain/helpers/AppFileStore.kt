package com.ismartcoding.plain.helpers

import android.content.Context
import android.webkit.MimeTypeMap
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.db.AppFileDao
import java.io.File

/**
 * Content-addressable store for chat files.
 *
 * Storage layout inside the app's external-files directory:
 *   files/{hash[0..1]}/{hash[2..3]}/{hash}.{ext}   (lowercase extension)
 *
 * URI scheme used in [com.ismartcoding.plain.db.DMessageFile.uri]:
 *   fid:{sha256hex}.{ext}   (extension derived from MIME type, lowercase)
 *
 * The fidSuffix (part after "fid:") encodes both the hash and extension so
 * path resolution never needs a database query.
 */
object AppFileStore {
    /** Convert a SHA-256 hash and optional lowercase extension into a [fid:] URI. */
    fun toFidUri(fileId: String, ext: String = ""): String =
        if (ext.isNotEmpty()) "fid:$fileId.$ext" else "fid:$fileId"

    /** Derive extension from a MIME type string (lowercase, empty string if unknown). */
    fun extFromMime(mimeType: String): String {
        if (mimeType.isEmpty()) return ""
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.lowercase() ?: ""
    }

    /**
     * Derive the real file-system path from a fidSuffix (the part after "fid:").
     * fidSuffix may be "{hash}" (legacy) or "{hash}.{ext}" (current).
     * Returns the absolute path whether or not the file currently exists.
     */
    fun realPathFromId(context: Context, fidSuffix: String): String {
        val hash = fidSuffix.substringBefore(".")
        val base = context.appDir()
        return "$base/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$fidSuffix"
    }

    /**
     * Resolve a URI that may be:
     *   - "fid:{hash}.{ext}" → derived real path (no DB query)
     *   - "fid:{hash}"       → legacy path without extension (no DB query)
     *   - "app://{rel}"      → existing app:// resolution handled by getFinalPath
     *   - absolute path      → returned as-is
     */
    fun resolveUri(context: Context, uri: String): String {
        if (uri.startsWith("fid:", ignoreCase = true)) {
            return realPathFromId(context, uri.removePrefix("fid:"))
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
     * - Otherwise copies/moves [srcFile] into the store directory and inserts
     *   a new [DAppFile] row.
     *
     * @param srcFile    Source file to import. Caller retains ownership; this
     *                   method copies the content (does not delete srcFile).
     * @param mimeType   Optional MIME type override. Guessed from extension if
     *                   blank.
     * @param deleteSrc  When true the srcFile is deleted after a successful
     *                   copy (move semantics).
     */
    fun importFile(
        context: Context,
        srcFile: File,
        mimeType: String = "",
        deleteSrc: Boolean = false,
    ): DAppFile {
        val dao = AppDatabase.instance.appFileDao()
        val size = srcFile.length()
        val strongHash by lazy { FileHashHelper.strongHash(srcFile) }

        // ── Step 1: weak check ────────────────────────────────────────────
        val weakHash = FileHashHelper.weakHash(srcFile)
        val candidates = dao.findByWeakKey(size, weakHash)

        if (candidates.isNotEmpty()) {
            // ── Step 2: strong check ──────────────────────────────────────
            tryReuseExisting(context, dao, srcFile, strongHash, deleteSrc)?.let { return it }
            // Weak matched but strong differs – fall through to insert new
            return insertNew(context, dao, srcFile, size, weakHash, strongHash, mimeType, deleteSrc)
        }

        // No weak match. Double-check by id in case another thread raced us.
        tryReuseExisting(context, dao, srcFile, strongHash, deleteSrc)?.let { return it }

        return insertNew(context, dao, srcFile, size, weakHash, strongHash, mimeType, deleteSrc)
    }

    /**
     * Import raw bytes (e.g. a completed download) into the store.
     */
    fun importBytes(
        context: Context,
        data: ByteArray,
        mimeType: String = "",
    ): DAppFile {
        val dao = AppDatabase.instance.appFileDao()
        val size = data.size.toLong()
        val strongHash = FileHashHelper.strongHash(data)

        val existing = dao.getById(strongHash)
        if (existing != null) {
            dao.incrementRefCount(strongHash)
            return existing
        }

        // Compute weak hash from the same data
        val weakHash = FileHashHelper.weakHash(data)

        val effectiveMime = mimeType.ifEmpty { "application/octet-stream" }
        val ext = extFromMime(effectiveMime)
        val destFile = destFile(context, strongHash, ext)
        destFile.parentFile?.mkdirs()
        destFile.writeBytes(data)

        val record = DAppFile(strongHash).apply {
            this.size = size
            this.mimeType = effectiveMime
            this.realPath = destFile.absolutePath
            this.refCount = 1
            this.weakHash = weakHash
        }
        dao.insert(record)
        return record
    }

    // ── Reference counting ──────────────────────────────────────────────────

    /**
     * Decrement the reference count for [fidSuffix] (the part after "fid:",
     * e.g. "{hash}.{ext}" or legacy "{hash}").
     * When refCount reaches 0 the physical file is deleted.
     */
    fun release(context: Context, fidSuffix: String) {
        val hash = fidSuffix.substringBefore(".")
        val dao = AppDatabase.instance.appFileDao()
        dao.decrementRefCount(hash)
        val updated = dao.getById(hash) ?: return
        if (updated.refCount <= 0) {
            dao.delete(hash)
            File(updated.realPath).delete()
            LogCat.d("ChatFileStore: deleted orphan file $hash")
        }
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private fun destFile(context: Context, hash: String, ext: String = ""): File {
        val base = context.appDir()
        val name = if (ext.isNotEmpty()) "$hash.$ext" else hash
        return File("$base/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$name")
    }

    private fun tryReuseExisting(
        context: Context,
        dao: AppFileDao,
        srcFile: File,
        strongHash: String,
        deleteSrc: Boolean,
    ): DAppFile? {
        val existing = dao.getById(strongHash) ?: return null
        val ext = extFromMime(existing.mimeType)
        val targetFile = destFile(context, strongHash, ext)

        // DB row may exist while the backing file was deleted; restore it.
        if (!targetFile.exists()) {
            // Check if old file without extension exists (pre-migration) and rename it
            val legacyFile = destFile(context, strongHash)
            if (legacyFile.exists()) {
                legacyFile.renameTo(targetFile)
                LogCat.d("ChatFileStore: renamed legacy file $strongHash to include extension")
            } else {
                storeSourceFile(srcFile, targetFile, deleteSrc)
                LogCat.d("ChatFileStore: restored missing file $strongHash")
            }
        } else if (deleteSrc) {
            srcFile.delete()
        }

        if (existing.realPath != targetFile.absolutePath) {
            existing.realPath = targetFile.absolutePath
            dao.update(existing)
        }

        dao.incrementRefCount(strongHash)
        existing.refCount += 1
        LogCat.d("ChatFileStore: reusing file $strongHash (refCount ${existing.refCount})")
        return existing
    }

    private fun storeSourceFile(
        srcFile: File,
        destFile: File,
        deleteSrc: Boolean,
    ) {
        destFile.parentFile?.mkdirs()
        if (deleteSrc) {
            // renameTo is atomic but fails silently across mount points
            // (e.g. cacheDir → getExternalFilesDir()).  Fall back to copy+delete.
            val renamed = srcFile.renameTo(destFile)
            if (!renamed) {
                srcFile.copyTo(destFile, overwrite = true)
                srcFile.delete()
            }
        } else {
            srcFile.copyTo(destFile, overwrite = true)
        }
    }

    private fun insertNew(
        context: Context,
        dao: AppFileDao,
        srcFile: File,
        size: Long,
        weakHash: String,
        strongHash: String,
        mimeType: String,
        deleteSrc: Boolean,
    ): DAppFile {
        val effectiveMime = mimeType.ifEmpty {
            val srcExt = srcFile.name.getFilenameExtension()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(srcExt) ?: "application/octet-stream"
        }
        val ext = extFromMime(effectiveMime)
        val destFile = destFile(context, strongHash, ext)
        storeSourceFile(srcFile, destFile, deleteSrc)

        val record = DAppFile(strongHash).apply {
            this.size = size
            this.mimeType = effectiveMime
            this.realPath = destFile.absolutePath
            this.refCount = 1
            this.weakHash = weakHash
        }
        dao.insert(record)
        LogCat.d("ChatFileStore: stored new file $strongHash (${size} bytes)")
        return record
    }
}
