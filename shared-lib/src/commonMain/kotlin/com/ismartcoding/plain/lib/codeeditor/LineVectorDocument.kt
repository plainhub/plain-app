package com.ismartcoding.plain.lib.codeeditor

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Editable text document backed by a line vector: parallel primitive arrays hold, per line,
 * the byte range in the immutable original file plus a sparse map of lines rewritten by edits.
 * Memory stays O(lineCount * 8 bytes + edit size) — a 100MB/2M-line file costs ~16MB of
 * primitive metadata and the original bytes are never materialized as a whole string.
 *
 * Coordinate model: lines are addressed by index; columns are UTF-16 char offsets within the
 * decoded line (what String.length counts). Internal lengths are bytes (UTF-8).
 *
 * Thread confinement: mutations happen on the owner thread; reads synchronize so background
 * search/highlight observe a consistent snapshot.
 */
class LineVectorDocument internal constructor(
    internal val source: ByteSource?,
    starts: IntList,
    lens: IntList,
    crlf: BitList,
    internal val dominantCrlf: Boolean,
    internal var lastLineHasTerminator: Boolean,
) : SynchronizedObject() {
    internal val starts = starts
    internal val lens = lens
    internal val crlf = crlf

    // Lines overridden by edits (content without terminator). Absent = file-backed.
    internal val edited = HashMap<Int, String>()
    private val editedByteLens = HashMap<Int, Int>()

    var version: Int = 0
        private set

    val lineCount: Int
        get() = synchronized(this) { starts.size }

    fun isEmpty(): Boolean = synchronized(this) { starts.size == 1 && (editedByteLens[0] ?: lens.get(0)) == 0 }

    fun lineLengthBytes(n: Int): Long = synchronized(this) { editedByteLens[n]?.toLong() ?: lens.get(n).toLong() }

    fun lineText(n: Int): String = synchronized(this) { lineTextLocked(n) }

    private fun lineTextLocked(n: Int): String {
        edited[n]?.let { return it }
        val start = starts.get(n)
        val len = lens.get(n)
        if (len == 0) return ""
        return source!!.readAt(start.toLong(), len).decodeToString()
    }

    fun lineContentBytes(n: Int): ByteArray = synchronized(this) {
        edited[n]?.let { return it.encodeToByteArray() }
        val len = lens.get(n)
        if (len == 0) return ByteArray(0)
        source!!.readAt(starts.get(n).toLong(), len)
    }

    /** Display chunking for extremely long lines; a logical line spans [lineChunkCount] visual rows. */
    fun lineChunkCount(n: Int): Int = synchronized(this) {
        val byteLen = editedByteLens[n] ?: lens.get(n)
        ((byteLen + CHUNK_BYTES - 1) / CHUNK_BYTES).coerceAtLeast(1)
    }

    fun lineChunkText(n: Int, chunkIdx: Int): String = synchronized(this) {
        edited[n]?.let { text ->
            val from = (chunkIdx * CHUNK_BYTES).coerceAtMost(text.length)
            val to = minOf(text.length, from + CHUNK_BYTES)
            return text.substring(from, to)
        }
        val byteLen = lens.get(n)
        if (byteLen <= CHUNK_BYTES) return lineTextLocked(n)
        val from = extendUtf8Boundary(n, chunkIdx * CHUNK_BYTES)
        val to = extendUtf8Boundary(n, minOf(byteLen, (chunkIdx + 1) * CHUNK_BYTES))
        if (to <= from) return ""
        source!!.readAt((starts.get(n) + from).toLong(), to - from).decodeToString()
    }

    /** Char offset of a chunk start within the logical line (for caret mapping). */
    fun lineChunkCharStart(n: Int, chunkIdx: Int): Int = synchronized(this) {
        edited[n]?.let { return minOf(it.length, chunkIdx * CHUNK_BYTES) }
        val from = extendUtf8Boundary(n, chunkIdx * CHUNK_BYTES)
        if (from <= 0) return 0
        source!!.readAt(starts.get(n).toLong(), from).decodeToString().length
    }

    private fun extendUtf8Boundary(n: Int, pos: Int): Int {
        var p = pos
        val start = starts.get(n)
        val total = lens.get(n)
        val src = source!!
        while (p < total) {
            val b = src.readAt((start + p).toLong(), 1)[0].toInt() and 0xFF
            if (b and 0xC0 == 0x80) p++ else break
        }
        return p
    }

    fun getText(startLine: Int, startCol: Int, endLine: Int, endCol: Int): String = synchronized(this) {
        val sb = StringBuilder()
        appendTextLocked(sb, startLine, startCol, endLine, endCol)
        sb.toString()
    }

    private fun appendTextLocked(sb: StringBuilder, startLine: Int, startCol: Int, endLine: Int, endCol: Int) {
        if (startLine == endLine) {
            val text = lineTextLocked(startLine)
            val from = startCol.coerceIn(0, text.length)
            val to = endCol.coerceIn(from, text.length)
            sb.append(text, from, to)
            return
        }
        val first = lineTextLocked(startLine)
        sb.append(first, startCol.coerceIn(0, first.length), first.length)
        for (line in startLine + 1 until endLine) {
            sb.append('\n')
            sb.append(lineTextLocked(line))
        }
        sb.append('\n')
        val last = lineTextLocked(endLine)
        sb.append(last, 0, endCol.coerceIn(0, last.length))
    }

    /**
     * Replaces [range] with [newText] (which may contain '\n') and returns the previous text.
     * Terminator style of surviving/original lines is preserved; new lines inherit the file's
     * dominant style.
     */
    fun replace(range: EditRange, newText: String): String = synchronized(this) {
        val startLine = range.startLine.coerceIn(0, starts.size - 1)
        val endLine = range.endLine.coerceIn(startLine, starts.size - 1)
        val firstText = lineTextLocked(startLine)
        val lastText = lineTextLocked(endLine)
        val startCol = range.startCol.coerceIn(0, firstText.length)
        val endCol = range.endCol.coerceIn(0, lastText.length)

        val oldTextBuilder = StringBuilder()
        appendTextLocked(oldTextBuilder, startLine, startCol, endLine, endCol)
        val oldText = oldTextBuilder.toString()

        val merged = StringBuilder()
            .append(firstText, 0, startCol)
            .append(newText)
            .append(lastText, endCol, lastText.length)
            .toString()
        val newLines = merged.split('\n')

        val removedCount = endLine - startLine + 1
        for (i in startLine..endLine) {
            edited.remove(i)
            editedByteLens.remove(i)
        }
        // Shift edit overrides for lines after the replaced span.
        if (removedCount != newLines.size) {
            shiftEdited(startLine + removedCount, newLines.size - removedCount)
        }
        starts.removeRange(startLine, startLine + removedCount)
        lens.removeRange(startLine, startLine + removedCount)
        crlf.removeRange(startLine, startLine + removedCount)
        var idx = startLine
        for (text in newLines) {
            starts.insert(idx, 0)
            val byteLen = text.encodeToByteArray().size
            lens.insert(idx, byteLen)
            crlf.insert(idx, dominantCrlf)
            edited[idx] = text
            editedByteLens[idx] = byteLen
            idx++
        }
        // lastLineHasTerminator is intentionally untouched: the virtual terminator after EOF
        // sits outside any content range, so replaces never add or remove it.

        version++
        oldText
    }

    private fun shiftEdited(fromLine: Int, delta: Int) {
        if (delta > 0) {
            val keys = edited.keys.filter { it >= fromLine }.sortedDescending()
            for (key in keys) {
                edited[key + delta] = edited.remove(key)!!
                editedByteLens[key + delta] = editedByteLens.remove(key)!!
            }
        } else if (delta < 0) {
            val keys = edited.keys.filter { it >= fromLine }.sorted()
            for (key in keys) {
                val target = key + delta
                if (target >= fromLine + delta) {
                    edited[target] = edited.remove(key)!!
                    editedByteLens[target] = editedByteLens.remove(key)!!
                } else {
                    edited.remove(key)
                    editedByteLens.remove(key)
                }
            }
        }
    }

    fun insert(line: Int, col: Int, text: String): String = replace(EditRange(line, col, line, col), text)

    fun delete(range: EditRange): String = replace(range, "")

    fun fullText(): String = synchronized(this) {
        val sb = StringBuilder()
        for (i in 0 until starts.size) {
            if (i > 0) sb.append('\n')
            sb.append(lineTextLocked(i))
        }
        if (lastLineHasTerminator) sb.append('\n')
        sb.toString()
    }

    fun close() = synchronized(this) {
        source?.close()
    }

    /** Deep-ish copy for cancel-edit restore: primitive arrays are copied, edited lines shared. */
    fun snapshot(): DocSnapshot = synchronized(this) {
        DocSnapshot(starts.snapshot(), lens.snapshot(), crlf.snapshot(), HashMap(edited), lastLineHasTerminator, version)
    }

    fun restore(snapshot: DocSnapshot) = synchronized(this) {
        val lensSnap = snapshot.lens
        starts.clear()
        lens.clear()
        starts.addAll(snapshot.starts)
        lens.addAll(lensSnap)
        crlf.clear()
        for (i in 0 until lensSnap.size) {
            crlf.insert(i, snapshot.crlf.get(i))
        }
        edited.clear()
        editedByteLens.clear()
        edited.putAll(snapshot.edited)
        for ((line, text) in edited) {
            editedByteLens[line] = text.encodeToByteArray().size
        }
        lastLineHasTerminator = snapshot.lastLineHasTerminator
        version = snapshot.version + 1
    }

    /** Streaming serialization for save; never holds the whole file in memory. */
    fun byteChunks(chunkSize: Int = 256 * 1024): Iterator<ByteArray> {
        val doc = this
        return object : Iterator<ByteArray> {
            var line = 0
            var pending: ByteArray? = null
            var pendingPos = 0
            var bufPos = 0
            var buf = ByteArray(0)

            /** Advances to the next line that yields bytes; null when exhausted. */
            fun fetchNext(): ByteArray? {
                while (line < doc.lineCount) {
                    val current = line
                    val content = doc.lineContentBytes(current)
                    val last = current == doc.lineCount - 1
                    val hasTerminator = !last || doc.lastLineHasTerminator
                    line++
                    if (content.isEmpty() && !hasTerminator) continue
                    val terminator = if (doc.dominantCrlf) CRLF_BYTES else LF_BYTES
                    return if (hasTerminator) content + terminator else content
                }
                return null
            }

            fun scratch(): ByteArray {
                if (buf.size != chunkSize) buf = ByteArray(chunkSize)
                return buf
            }

            fun fill() {
                while (bufPos < chunkSize) {
                    var bytes = pending
                    if (bytes == null) {
                        bytes = fetchNext() ?: return
                        pending = bytes
                        pendingPos = 0
                    }
                    val copied = minOf(bytes.size - pendingPos, chunkSize - bufPos)
                    if (copied > 0) {
                        bytes.copyInto(scratch(), bufPos, pendingPos, pendingPos + copied)
                        bufPos += copied
                        pendingPos += copied
                    }
                    if (pendingPos >= bytes.size) pending = null
                }
            }

            override fun hasNext(): Boolean = synchronized(doc) {
                if (pending != null || bufPos > 0) return true
                if (line < doc.lineCount - 1) return true
                if (line == doc.lineCount - 1) {
                    return doc.lineLengthBytes(line) > 0 || doc.lastLineHasTerminator
                }
                false
            }

            override fun next(): ByteArray = synchronized(doc) {
                fill()
                if (bufPos == 0) throw NoSuchElementException()
                val out = scratch().copyOf(bufPos)
                bufPos = 0
                out
            }
        }
    }

    companion object {
        private val LF_BYTES = byteArrayOf('\n'.code.toByte())
        private val CRLF_BYTES = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
        const val CHUNK_BYTES = 2000

        fun fromText(text: String): LineVectorDocument {
            val lines = text.split('\n')
            val starts = IntList(lines.size)
            val lens = IntList(lines.size)
            val crlf = BitList(lines.size)
            val doc = LineVectorDocument(null, starts, lens, crlf, dominantCrlf = false, lastLineHasTerminator = text.isNotEmpty() && text.endsWith("\n"))
            for (line in lines) {
                starts.add(0)
                val byteLen = line.encodeToByteArray().size
                lens.add(byteLen)
                crlf.add(false)
                doc.edited[doc.starts.size - 1] = line
                doc.editedByteLens[doc.starts.size - 1] = byteLen
            }
            return doc
        }
    }
}

data class EditRange(val startLine: Int, val startCol: Int, val endLine: Int, val endCol: Int) {
    fun normalized(): EditRange {
        if (startLine < endLine) return this
        if (startLine > endLine) return EditRange(endLine, endCol, startLine, startCol)
        return EditRange(startLine, minOf(startCol, endCol), endLine, maxOf(startCol, endCol))
    }
}

class DocSnapshot internal constructor(
    internal val starts: IntList,
    internal val lens: IntList,
    internal val crlf: BitList,
    internal val edited: Map<Int, String>,
    internal val lastLineHasTerminator: Boolean,
    internal val version: Int,
)
