package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import com.ismartcoding.plain.lib.codeeditor.EditHistory
import com.ismartcoding.plain.lib.codeeditor.EditRange
import com.ismartcoding.plain.lib.codeeditor.HighlightEngine
import com.ismartcoding.plain.lib.codeeditor.HorizontalPan
import com.ismartcoding.plain.lib.codeeditor.Languages
import com.ismartcoding.plain.lib.codeeditor.LineIndexBuilder
import com.ismartcoding.plain.lib.codeeditor.LineVectorDocument
import com.ismartcoding.plain.lib.codeeditor.SearchEngine
import com.ismartcoding.plain.lib.codeeditor.SearchMatch
import com.ismartcoding.plain.lib.codeeditor.Span
import com.ismartcoding.plain.lib.codeeditor.DetectedEncoding
import com.ismartcoding.plain.lib.codeeditor.EncodingProbe
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.openByteSource
import com.ismartcoding.plain.platform.writeByteChunksStreaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EditorLoadState {
    data object Idle : EditorLoadState()
    data class Loading(val progress: Float) : EditorLoadState()
    data object Ready : EditorLoadState()
    data class Error(val message: String) : EditorLoadState()
}

/**
 * Orchestrates the code editor: owns the document, undo history, search session and the
 * scroll/caret/selection state the composables render. Heavy work (index build, search,
 * highlighting) runs on Dispatchers.Default; edits apply on the main thread.
 */
class EditorController(private val scope: kotlinx.coroutines.CoroutineScope) {
    val loadState = mutableStateOf<EditorLoadState>(EditorLoadState.Idle)
    val docVersion = mutableStateOf(0)
    val mapperVersion = mutableStateOf(0)
    val lineCount = mutableStateOf(0)
    val wrapContent = mutableStateOf(true)
    val readOnly = mutableStateOf(true)
    val isDirty = mutableStateOf(false)
    val fontSizeSp = mutableStateOf(14)
    val statusBarVisible = mutableStateOf(true)
    val encodingLabel = mutableStateOf("UTF-8")
    val canUndo = mutableStateOf(false)
    val canRedo = mutableStateOf(false)
    val highlightVersion = mutableStateOf(0)
    val searchVisible = mutableStateOf(false)
    val searchComplete = mutableStateOf(false)
    val searchTruncated = mutableStateOf(false)
    val matchesVersion = mutableStateOf(0)

    // Caret / selection
    val activeLine = mutableStateOf(0)
    val activeCol = mutableStateOf(0)
    val activeChunk = mutableStateOf(0)
    val selection = mutableStateOf<EditRange?>(null)

    // Search
    val searchQuery = mutableStateOf("")
    val searchCaseSensitive = mutableStateOf(false)
    val searchRegex = mutableStateOf(false)
    val currentMatchIndex = mutableStateOf(0)

    // Field binding for the persistent input surface
    val fieldText = mutableStateOf("")
    val fieldSelection = mutableStateOf(TextRange.Zero)

    // Layout metrics published by the viewport (monospace grid)
    var charWidthPx: Float = 8f
    var lineHeightPx: Float = 44f

    val listState = LazyListState()
    val contentWidthPx = mutableStateOf(0f)
    // Width of the pannable text cell (viewport minus gutter); the pan bound must use this,
    // not the full viewport, so fully-panned lines stop short of the screen edge.
    var pannableWidthPx = 0f
    val hPan = HorizontalPan()
    val hPanOffset = androidx.compose.runtime.mutableFloatStateOf(0f)
    val mapper = VisualLineMapper()

    fun syncPan() {
        if (hPan.updateBounds(contentWidthPx.value, pannableWidthPx)) {
            hPanOffset.floatValue = hPan.offsetPx
        }
    }

    internal var doc: LineVectorDocument? = null
        private set
    private var highlighter: HighlightEngine? = null
    private var history = EditHistory()
    private var snapshot: com.ismartcoding.plain.lib.codeeditor.DocSnapshot? = null
    private var searchJob: Job? = null
    private var mapperJob: Job? = null
    private var suppressFieldSync = false

    var openFileSize: Long = 0
        private set

    private var matchesInternal: List<SearchMatch> = emptyList()
    private var matchesByLineInternal: Map<Int, List<SearchMatch>> = emptyMap()
    private var path: String = ""

    fun open(filePath: String, gotoEnd: Boolean) {
        path = filePath
        loadState.value = EditorLoadState.Loading(0f)
        scope.launch {
            try {
                val languageId = Languages.pathToLanguageId(filePath)
                val loaded = withIO { loadDocument(filePath) }
                finishOpen(loaded, languageId, gotoEnd)
            } catch (e: Exception) {
                loadState.value = EditorLoadState.Error(e.message ?: "failed to open file")
            }
        }
    }

    /** Opens in-memory text (e.g. chat message content) instead of a file. */
    fun openText(content: String, languageId: String) {
        openFileSize = content.encodeToByteArray().size.toLong()
        loadState.value = EditorLoadState.Loading(0f)
        scope.launch(Dispatchers.Default) {
            val loaded = LineVectorDocument.fromText(content)
            withContext(Dispatchers.Main) { finishOpen(loaded, languageId, gotoEnd = false) }
        }
    }

    private fun finishOpen(loaded: LineVectorDocument, languageId: String, gotoEnd: Boolean) {
        doc = loaded
        highlighter = HighlightEngine(Languages.lexerFor(languageId))
        lineCount.value = loaded.lineCount
        docVersion.value++
        setActive(0, 0)
        // Publish the mapper BEFORE Ready: the first composition must already see the full
        // visual count. Going 0 -> N via a later state write lost the recomposition race
        // and left a blank viewport on huge files.
        mapper.rebuild(loaded)
        mapperVersion.value++
        loadState.value = EditorLoadState.Ready
        if (gotoEnd) gotoLine(loaded.lineCount - 1) else gotoLine(0)
    }

    private suspend fun loadDocument(filePath: String): LineVectorDocument {
        val source = openByteSource(filePath)
        openFileSize = source.size
        val encoding = EncodingProbe.probe(source)
        encodingLabel.value = when (encoding) {
            DetectedEncoding.UTF16LE, DetectedEncoding.UTF16BE -> "UTF-16"
            else -> "UTF-8"
        }
        return when (encoding) {
            DetectedEncoding.UTF8 -> LineIndexBuilder(source).buildAll()
            DetectedEncoding.UTF16LE, DetectedEncoding.UTF16BE -> {
                // UTF-16 path: convert wholesale with a size guard (streamed read).
                if (source.size > 50L * 1024 * 1024) throw IllegalArgumentException("UTF-16 file too large")
                val bytes = source.readAt(0, source.size.toInt())
                LineVectorDocument.fromText(decodeUtf16(bytes, little = encoding == DetectedEncoding.UTF16LE))
            }
            DetectedEncoding.BINARY -> throw IllegalArgumentException("binary file")
        }
    }

    private fun decodeUtf16(bytes: ByteArray, little: Boolean): String {
        if (bytes.size < 2) return ""
        val hasBom = (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) ||
            (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())
        val start = if (hasBom) 2 else 0
        val sb = StringBuilder((bytes.size - start) / 2)
        var i = start
        while (i + 1 < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val code = if (little) b0 or (b1 shl 8) else (b0 shl 8) or b1
            sb.append(code.toChar())
            i += 2
        }
        return sb.toString()
    }

    fun close() {
        searchJob?.cancel()
        mapperJob?.cancel()
        val d = doc
        doc = null
        if (d != null) {
            scope.launch(Dispatchers.Default) { runCatching { d.close() } }
        }
    }

    // ---- rendering accessors (main thread) ----

    fun lineText(n: Int): String = doc?.lineText(n) ?: ""

    fun rowLine(visual: Int): Int = mapper.rowLine(visual)

    fun rowChunk(visual: Int): Int = mapper.rowChunk(visual)

    fun visualCount(): Int = mapper.visualCount

    fun rowText(visual: Int): String {
        val line = mapper.rowLine(visual)
        val chunk = mapper.rowChunk(visual)
        val d = doc ?: return ""
        return if (mapper.chunked(line)) d.lineChunkText(line, chunk) else d.lineText(line)
    }

    /** Row text with chunk start col folded in (for caret math). */
    fun rowCharBase(visual: Int): Int {
        val line = mapper.rowLine(visual)
        val chunk = mapper.rowChunk(visual)
        val d = doc ?: return 0
        return if (mapper.chunked(line)) d.lineChunkCharStart(line, chunk) else 0
    }

    fun cachedSpans(line: Int): Array<Span>? {
        val d = doc ?: return null
        // Extremely long lines skip syntax highlighting: lexing a 10MB line would stall.
        if (d.lineChunkCount(line) > 1) return null
        return highlighter?.spansFor(d, line)
    }

    fun computeHighlights(lines: IntRange) {
        val d = doc ?: return
        val h = highlighter ?: return
        var changed = false
        for (line in lines) {
            if (line < 0 || line >= d.lineCount) continue
            if (d.lineChunkCount(line) > 1) continue
            val spans = h.spansFor(d, line)
            if (spans != null) changed = true
        }
        if (changed) highlightVersion.value++
    }

    fun gutterDigits(): Int = lineCount.value.toString().length

    fun matchLineInfo(line: Int): List<SearchMatch> = matchesByLineInternal[line] ?: emptyList()

    fun currentMatch(): SearchMatch? {
        val idx = currentMatchIndex.value
        return matchesInternal.getOrNull(idx)
    }

    // ---- scroll ----

    fun gotoLine(line: Int) {
        val l = line.coerceIn(0, (lineCount.value - 1).coerceAtLeast(0))
        scope.launch {
            listState.scrollToItem(mapper.logicalToVisual(l).coerceIn(0, (mapper.visualCount - 1).coerceAtLeast(0)))
        }
    }

    fun jumpToLine(line: Int) {
        val l = line.coerceIn(1, lineCount.value)
        setActive(l - 1, 0)
        gotoLine(l - 1)
    }

    fun gotoTop() = gotoLine(0)

    fun gotoEnd() = gotoLine((lineCount.value - 1).coerceAtLeast(0))

    fun ensureActiveVisible() {
        val visual = mapper.logicalToVisual(activeLine.value)
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return
        val item = visible.firstOrNull { it.index == visual }
        val viewportStart = info.viewportStartOffset
        val viewportEnd = info.viewportEndOffset
        val estimate = lineHeightPx
        when {
            item == null -> scope.launch { listState.scrollToItem(visual.coerceIn(0, (mapper.visualCount - 1).coerceAtLeast(0))) }
            item.offset < viewportStart + estimate * 2 -> scope.launch { listState.scrollToItem(visual, 0) }
            item.offset + item.size > viewportEnd - estimate * 2 -> scope.launch {
                listState.scrollToItem(visual, (item.size - (viewportEnd - viewportStart) / 2).coerceAtLeast(0))
            }
        }
    }

    // ---- config ----

    fun toggleWrap(persist: (Boolean) -> Unit = {}) {
        wrapContent.value = !wrapContent.value
        persist(wrapContent.value)
    }

    // ---- edit mode ----

    fun enterEditMode() {
        val d = doc ?: return
        snapshot = d.snapshot()
        readOnly.value = false
        history.clear()
        canUndo.value = false
        canRedo.value = false
        setActive(0, 0)
    }

    fun exitEditMode(discardChanges: Boolean) {
        val d = doc ?: return
        if (discardChanges && snapshot != null) {
            d.restore(snapshot!!)
            onDocChanged(0)
        }
        snapshot = d.snapshot()
        isDirty.value = false
        readOnly.value = true
        selection.value = null
        setActive(0, 0)
        gotoTop()
    }

    suspend fun saveAsync(): Result<Unit> {
        val d = doc ?: return Result.failure(IllegalStateException("no document"))
        return withIO {
            val ok = writeByteChunksStreaming(path, d.byteChunks())
            if (ok) {
                snapshot = d.snapshot()
                isDirty.value = false
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("save failed"))
            }
        }
    }

    // ---- caret & selection ----

    fun setActive(line: Int, col: Int) {
        val d = doc ?: return
        val l = line.coerceIn(0, d.lineCount - 1)
        val textLen = d.lineText(l).length
        val c = col.coerceIn(0, textLen)
        activeLine.value = l
        activeCol.value = c
        selection.value = null
        syncField()
        if (!readOnly.value) ensureActiveVisible()
    }

    fun beginSelectionAt(line: Int, col: Int) {
        val d = doc ?: return
        val l = line.coerceIn(0, d.lineCount - 1)
        val c = col.coerceIn(0, d.lineText(l).length)
        selection.value = EditRange(l, c, l, c)
    }

    fun moveCaretRelative(deltaLines: Int, deltaCols: Int) {
        val d = doc ?: return
        val targetLine = (activeLine.value + deltaLines).coerceIn(0, d.lineCount - 1)
        val targetCol = (activeCol.value + deltaCols).coerceIn(0, d.lineText(targetLine).length)
        setActive(targetLine, targetCol)
    }

    fun extendSelectionTo(line: Int, col: Int) {
        val d = doc ?: return
        val l = line.coerceIn(0, d.lineCount - 1)
        val c = col.coerceIn(0, d.lineText(l).length)
        val anchor = selection.value?.let { EditRange(it.startLine, it.startCol, it.startLine, it.startCol) }
            ?: EditRange(activeLine.value, activeCol.value, activeLine.value, activeCol.value)
        selection.value = EditRange(anchor.startLine, anchor.startCol, l, c).normalized()
        activeLine.value = selection.value!!.endLine
        activeCol.value = selection.value!!.endCol
    }

    fun selectedText(): String? {
        val sel = selection.value?.normalized() ?: return null
        val d = doc ?: return null
        return d.getText(sel.startLine, sel.startCol, sel.endLine, sel.endCol).ifEmpty { null }
    }

    fun selectAll() {
        val d = doc ?: return
        selection.value = EditRange(0, 0, d.lineCount - 1, d.lineText(d.lineCount - 1).length)
    }

    // ---- editing (driven by the input surface) ----

    fun onFieldChange(newText: String, newSelection: TextRange) {
        if (suppressFieldSync) return
        val oldText = fieldText.value
        if (newText == oldText) {
            activeCol.value = newSelection.start.coerceIn(0, newText.length)
            fieldSelection.value = newSelection
            return
        }
        // Diff old -> new
        var start = 0
        while (start < oldText.length && start < newText.length && oldText[start] == newText[start]) start++
        var endOld = oldText.length
        var endNew = newText.length
        while (endOld > start && endNew > start && oldText[endOld - 1] == newText[endNew - 1]) {
            endOld--; endNew--
        }
        val removed = oldText.substring(start, endOld)
        val inserted = newText.substring(start, endNew)

        val sel = selection.value?.normalized()
        val base = fieldCharBase()
        val range = if (sel != null) sel else EditRange(activeLine.value, base + start, activeLine.value, base + start + removed.length)
        applyEdit(range, inserted, typing = inserted.length == 1 && removed.isEmpty())
        selection.value = null
    }

    private fun fieldCharBase(): Int {
        val d = doc ?: return 0
        val line = activeLine.value
        return if (d.lineChunkCount(line) > 1) d.lineChunkCharStart(line, activeChunk.value) else 0
    }

    fun applyEdit(range: EditRange, text: String, typing: Boolean) {
        val d = doc ?: return
        val old = d.replace(range, text)
        history.push(range, old, text, typing)
        canUndo.value = history.canUndo
        canRedo.value = history.canRedo
        isDirty.value = true
        val lines = text.split('\n')
        activeLine.value = range.startLine + lines.size - 1
        activeCol.value = if (lines.size == 1) range.startCol + lines[0].length else lines.last().length
        onDocChanged(range.startLine)
        syncField()
        if (!readOnly.value) ensureActiveVisible()
    }

    fun insertAtCursor(text: String) {
        val sel = selection.value?.normalized()
        val range = sel ?: EditRange(activeLine.value, activeCol.value, activeLine.value, activeCol.value)
        applyEdit(range, text, typing = false)
    }

    fun deleteSelection() {
        val sel = selection.value?.normalized() ?: return
        applyEdit(sel, "", typing = false)
        selection.value = null
    }

    fun joinWithPreviousLine() {
        val line = activeLine.value
        if (line == 0) return
        val d = doc!!
        val prevLen = d.lineText(line - 1).length
        applyEdit(EditRange(line - 1, prevLen, line, 0), "", typing = false)
    }

    fun undo() {
        val d = doc ?: return
        val restored = history.undo(d) ?: return
        canUndo.value = history.canUndo
        canRedo.value = history.canRedo
        activeLine.value = restored.startLine
        activeCol.value = restored.startCol
        selection.value = null
        onDocChanged(restored.startLine)
        syncField()
    }

    fun redo() {
        val d = doc ?: return
        val after = history.redo(d) ?: return
        canUndo.value = history.canUndo
        canRedo.value = history.canRedo
        activeLine.value = after.endLine
        activeCol.value = after.endCol
        selection.value = null
        onDocChanged(after.startLine)
        syncField()
    }

    private fun syncField() {
        val d = doc ?: return
        val line = activeLine.value.coerceIn(0, d.lineCount - 1)
        suppressFieldSync = true
        if (d.lineChunkCount(line) > 1) {
            val chunk = chunkForCol(d, line, activeCol.value)
            val base = d.lineChunkCharStart(line, chunk)
            activeChunk.value = chunk
            val text = d.lineChunkText(line, chunk)
            fieldText.value = text
            fieldSelection.value = TextRange((activeCol.value - base).coerceIn(0, text.length))
        } else {
            activeChunk.value = 0
            val text = d.lineText(line)
            fieldText.value = text
            fieldSelection.value = TextRange(activeCol.value.coerceIn(0, text.length))
        }
        suppressFieldSync = false
    }

    private fun chunkForCol(d: LineVectorDocument, line: Int, col: Int): Int {
        val chunks = d.lineChunkCount(line)
        var c = 0
        while (c < chunks - 1) {
            if (col < d.lineChunkCharStart(line, c + 1)) return c
            c++
        }
        return chunks - 1
    }

    private fun onDocChanged(fromLine: Int) {
        val d = doc ?: return
        docVersion.value++
        lineCount.value = d.lineCount
        highlighter?.invalidateFrom(fromLine)
        highlightVersion.value++
        scheduleMapperRebuild()
        scheduleSearchRefresh()
    }

    private fun scheduleMapperRebuild() {
        mapperJob?.cancel()
        val d = doc ?: return
        if (d.lineCount <= 200_000) {
            mapper.rebuild(d)
            mapperVersion.value++
        } else {
            mapperJob = scope.launch(Dispatchers.Default) {
                delay(150)
                mapper.rebuild(d)
                withContext(Dispatchers.Main) { mapperVersion.value++ }
            }
        }
    }

    // ---- search ----

    fun setSearchVisible(visible: Boolean) {
        searchVisible.value = visible
        if (!visible) {
            searchJob?.cancel()
            matchesInternal = emptyList()
            matchesByLineInternal = emptyMap()
            currentMatchIndex.value = 0
            matchesVersion.value++
        }
    }

    fun requestSearch(query: String, caseSensitive: Boolean, regex: Boolean) {
        searchQuery.value = query
        searchCaseSensitive.value = caseSensitive
        searchRegex.value = regex
        // Debounce: cancel-and-restart while typing so a 5-char query triggers one scan, not five.
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            runSearch()
        }
    }

    private fun scheduleSearchRefresh() {
        if (!searchVisible.value || searchQuery.value.isEmpty()) return
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            runSearch()
        }
    }

    private fun runSearch() {
        val d = doc ?: return
        val query = searchQuery.value
        if (query.isEmpty()) {
            matchesInternal = emptyList()
            matchesByLineInternal = emptyMap()
            currentMatchIndex.value = 0
            searchComplete.value = false
            matchesVersion.value++
            return
        }
        searchComplete.value = false
        searchTruncated.value = false
        searchJob = scope.launch(Dispatchers.Default) {
            val cs = searchCaseSensitive.value
            val rx = if (searchRegex.value) {
                try {
                    query.toRegex()
                } catch (e: Exception) {
                    null
                }
            } else null
            val needle = if (cs) null else query.lowercase()
            val found = ArrayList<SearchMatch>(256)
            var truncated = false
            var line = 0
            val batch = 512
            while (line < d.lineCount) {
                ensureActive()
                val end = minOf(line + batch, d.lineCount)
                val texts = d.lineTextBatch(line, end)
                for (idx in texts.indices) {
                    SearchEngine.findInLine(texts[idx], query, cs, rx, needle).forEach { found.add(SearchMatch(line + idx, it.startCol, it.length)) }
                }
                if (found.size >= MAX_MATCHES) {
                    truncated = true
                    break
                }
                line = end
            }
            val byLine = found.groupBy { it.line }
            // The job may be cancelled by a newer requestSearch between scan end and commit
            // (IME composing fires extra callbacks); the finished scan must still land.
            withContext(Dispatchers.Main + kotlinx.coroutines.NonCancellable) {
                matchesInternal = found
                matchesByLineInternal = byLine
                searchComplete.value = !truncated
                searchTruncated.value = truncated
                currentMatchIndex.value = 0
                matchesVersion.value++
            }
        }
    }

    fun matchCount(): Int = matchesInternal.size

    fun nextMatch(): SearchMatch? {
        if (matchesInternal.isEmpty()) return null
        val idx = (currentMatchIndex.value + 1).mod(matchesInternal.size)
        currentMatchIndex.value = idx
        scrollToMatch(matchesInternal[idx])
        return matchesInternal[idx]
    }

    fun prevMatch(): SearchMatch? {
        if (matchesInternal.isEmpty()) return null
        val idx = (currentMatchIndex.value - 1).mod(matchesInternal.size)
        currentMatchIndex.value = idx
        scrollToMatch(matchesInternal[idx])
        return matchesInternal[idx]
    }

    private fun scrollToMatch(match: SearchMatch) {
        gotoLine(match.line)
    }

    companion object {
        const val MAX_MATCHES = 10_000
    }
}
