package com.ismartcoding.plain.helpers

import android.content.Context
import android.webkit.MimeTypeMap
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DFile
import com.ismartcoding.plain.db.FileDao
import java.io.File

/**
 * Content-addressable store for chat files.
 *
 * Storage layout inside the app's external-files directory:
 *   files/{hash[0..1]}/{hash[2..3]}/{hash}   (no extension)
 *
 * URI scheme used in [com.ismartcoding.plain.db.DMessageFile.uri]:
 *   fid:{sha256hex}
 *
 * Path derivation is fully deterministic from the fileId so the UI can
 * resolve a real path without any database query.
 */
object AppFileStore {
    /** Convert a SHA-256 hash (fileId) into a [fid:] URI. */
    fun toFidUri(fileId: String): String = "fid:$fileId"

    /**
     * Derive the real file-system path from a fileId without a DB query.
     * Returns the absolute path whether or not the file currently exists.
     */
    fun realPathFromId(context: Context, fileId: String): String {
        val base = context.getExternalFilesDir(null)!!.absolutePath
        return "$base/${fileId.substring(0, 2)}/${fileId.substring(2, 4)}/$fileId"
    }

    /**
     * Resolve a URI that may be:
     *   - "fid:{hash}"      → derived real path (no DB query)
     *   - "app://{rel}"       → existing app:// resolution handled by getFinalPath
     *   - absolute path       → returned as-is
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
     *   the existing [DFile].
     * - Otherwise copies/moves [srcFile] into the store directory and inserts
     *   a new [DFile] row.
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
    ): DFile {
        val dao = AppDatabase.instance.fileDao()
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
    ): DFile {
        val dao = AppDatabase.instance.fileDao()
        val size = data.size.toLong()
        val strongHash = FileHashHelper.strongHash(data)

        val existing = dao.getById(strongHash)
        if (existing != null) {
            dao.incrementRefCount(strongHash)
            return existing
        }

        // Compute weak hash from the same data
        val weakHash = FileHashHelper.weakHash(data)

        val destFile = destFile(context, strongHash)
        destFile.parentFile?.mkdirs()
        destFile.writeBytes(data)

        val effectiveMime = mimeType.ifEmpty { "application/octet-stream" }
        val record = DFile(strongHash).apply {
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
     * Decrement the reference count for [fileId].
     * When refCount reaches 0 the physical file is deleted.
     */
    fun release(context: Context, fileId: String) {
        val dao = AppDatabase.instance.fileDao()
        dao.decrementRefCount(fileId)
        val updated = dao.getById(fileId) ?: return
        if (updated.refCount <= 0) {
            dao.delete(fileId)
            destFile(context, fileId).delete()
            LogCat.d("ChatFileStore: deleted orphan file $fileId")
        }
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private fun destFile(context: Context, fileId: String): File {
        val base = context.getExternalFilesDir(null)!!.absolutePath
        return File("$base/${fileId.substring(0, 2)}/${fileId.substring(2, 4)}/$fileId")
    }

    private fun tryReuseExisting(
        context: Context,
        dao: FileDao,
        srcFile: File,
        strongHash: String,
        deleteSrc: Boolean,
    ): DFile? {
        val existing = dao.getById(strongHash) ?: return null
        val targetFile = destFile(context, strongHash)

        // DB row may exist while the backing file was deleted; restore it.
        if (!targetFile.exists()) {
            storeSourceFile(srcFile, targetFile, deleteSrc)
            LogCat.d("ChatFileStore: restored missing file $strongHash")
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
        dao: FileDao,
        srcFile: File,
        size: Long,
        weakHash: String,
        strongHash: String,
        mimeType: String,
        deleteSrc: Boolean,
    ): DFile {
        val destFile = destFile(context, strongHash)
        storeSourceFile(srcFile, destFile, deleteSrc)

        val effectiveMime = mimeType.ifEmpty {
            val ext = srcFile.name.getFilenameExtension()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }

        val record = DFile(strongHash).apply {
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
