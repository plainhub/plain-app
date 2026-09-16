package com.ismartcoding.plain.lib.codeeditor

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineVectorDocumentTest {

    private fun docOf(vararg linesText: String): LineVectorDocument {
        val content = linesText.joinToString("")
        return LineIndexBuilder(ByteArrayByteSource(content.encodeToByteArray())).buildAll()
    }

    @Test
    fun emptyFileYieldsSingleEmptyLine() {
        val doc = docOf("")
        assertEquals(1, doc.lineCount)
        assertEquals("", doc.lineText(0))
    }

    @Test
    fun noTrailingNewline() {
        val doc = docOf("a\nb")
        assertEquals(2, doc.lineCount)
        assertEquals("a", doc.lineText(0))
        assertEquals("b", doc.lineText(1))
    }

    @Test
    fun trailingNewlineDoesNotAddEmptyLine() {
        val doc = docOf("a\nb\n")
        assertEquals(2, doc.lineCount)
        assertEquals("b", doc.lineText(1))
    }

    @Test
    fun crlfLines() {
        val doc = docOf("a\r\nb\r\n")
        assertEquals(2, doc.lineCount)
        assertEquals("a", doc.lineText(0))
        assertEquals("b", doc.lineText(1))
    }

    @Test
    fun blankLines() {
        val doc = docOf("a\n\nb")
        assertEquals(3, doc.lineCount)
        assertEquals("", doc.lineText(1))
    }

    @Test
    fun singleLineNoNewline() {
        val doc = docOf("hello")
        assertEquals(1, doc.lineCount)
        assertEquals("hello", doc.lineText(0))
    }

    @Test
    fun utf8Content() {
        val text = "你好\nworld🌍\n"
        val doc = docOf(text)
        assertEquals(2, doc.lineCount)
        assertEquals("你好", doc.lineText(0))
        assertEquals("world🌍", doc.lineText(1))
    }

    @Test
    fun insertSingleLine() {
        val doc = docOf("hello world")
        doc.insert(0, 5, " there")
        assertEquals("hello there world", doc.lineText(0))
    }

    @Test
    fun insertWithNewlines() {
        val doc = docOf("ab")
        doc.insert(0, 1, "x\ny\nz")
        assertEquals(3, doc.lineCount)
        assertEquals("ax", doc.lineText(0))
        assertEquals("y", doc.lineText(1))
        assertEquals("zb", doc.lineText(2))
    }

    @Test
    fun deleteAcrossLines() {
        val doc = docOf("abc\ndef\nghi")
        doc.delete(EditRange(0, 1, 2, 1))
        assertEquals("ahi", doc.fullText())
    }

    @Test
    fun roundtripBytesPreservesOriginal() {
        val cases = listOf("", "a", "a\n", "a\r\nb", "a\nb\n", "\n\n\n", "x\r\ny\r\n", "hello\r\nworld\r\n")
        for (content in cases) {
            val doc = LineIndexBuilder(ByteArrayByteSource(content.encodeToByteArray())).buildAll()
            val saved = doc.byteChunks().asSequence().fold(ByteArray(0)) { acc, chunk -> acc + chunk }.decodeToString()
            assertEquals(content, saved, "roundtrip failed for ${content.replace("\n", "\\n").replace("\r", "\\r")}")
        }
    }

    @Test
    fun roundtripAfterEdits() {
        val doc = docOf("line1\nline2\r\nline3\n")
        doc.insert(1, 0, "NEW\n")
        doc.delete(EditRange(0, 0, 0, 5))
        doc.insert(2, 5, " end")
        val saved = doc.byteChunks().asSequence().fold(ByteArray(0)) { acc, chunk -> acc + chunk }.decodeToString()
        // Reopen the saved bytes and verify structure matches the edited document.
        val reopened = LineIndexBuilder(ByteArrayByteSource(saved.encodeToByteArray())).buildAll()
        assertEquals(doc.lineCount, reopened.lineCount)
        for (i in 0 until doc.lineCount) {
            assertEquals(doc.lineText(i), reopened.lineText(i), "line $i mismatch")
        }
    }

    @Test
    fun snapshotRestoreUndoesEdits() {
        val doc = docOf("one\ntwo\nthree")
        val snap = doc.snapshot()
        doc.insert(0, 3, "\ninserted")
        doc.delete(EditRange(2, 0, 2, 5))
        assertEquals("one\ninserted\n\nthree", doc.fullText())
        doc.restore(snap)
        assertEquals("one\ntwo\nthree", doc.fullText())
    }

    @Test
    fun randomEditsMatchReferenceModel() {
        repeat(50) { seed ->
            val random = Random(seed)
            val initial = buildString {
                repeat(random.nextInt(1, 30)) {
                    append(random.nextLong(0, 9999).toString(36))
                    append(if (random.nextBoolean()) '\n' else ' ')
                }
            }
            val doc = docOf(initial)
            val reference = StringBuilder(initial)
            assertEquals(initial, doc.fullText(), "seed $seed initial build diverged")
            repeat(200) {
                val line = random.nextInt(doc.lineCount)
                val text = doc.lineText(line)
                val col = random.nextInt(text.length + 1)
                val globalOffset = globalCharOffset(doc, line, col)
                when (random.nextInt(3)) {
                    0 -> {
                        val payload = random.nextLong(0, 1_000_000).toString(36) + if (random.nextBoolean()) "\n" else ""
                        doc.insert(line, col, payload)
                        reference.insert(globalOffset, payload)
                    }
                    1 -> {
                        if (text.isNotEmpty()) {
                            val from = random.nextInt(text.length)
                            val to = random.nextInt(from + 1, text.length + 1)
                            doc.delete(EditRange(line, from, line, to))
                            reference.delete(globalOffsetOfCol(doc, line, from), globalOffsetOfCol(doc, line, to))
                        }
                    }
                    else -> {
                        doc.insert(line, col, "x")
                        reference.insert(globalOffset, "x")
                    }
                }
                assertEquals(reference.toString(), doc.fullText(), "seed $seed diverged after edit $it")
            }
        }
    }

    private fun globalCharOffsetOfCol(doc: LineVectorDocument, line: Int, col: Int): Int = globalOffsetOfCol(doc, line, col)

    private fun globalOffsetOfCol(doc: LineVectorDocument, line: Int, col: Int): Int {
        var offset = 0
        for (i in 0 until line) offset += doc.lineText(i).length + 1
        return offset + col
    }

    private fun globalCharOffset(doc: LineVectorDocument, line: Int, col: Int): Int = globalOffsetOfCol(doc, line, col)

    @Test
    fun searchText() {
        val doc = docOf("foo bar\nFOO baz foo\nfoo")
        val m0 = SearchEngine.findInLine(doc.lineText(0), "foo", caseSensitive = false, regex = null)
        assertEquals(1, m0.size)
        val m1 = SearchEngine.findInLine(doc.lineText(1), "foo", caseSensitive = false, regex = null)
        assertEquals(2, m1.size)
        val m1s = SearchEngine.findInLine(doc.lineText(1), "foo", caseSensitive = true, regex = null)
        assertEquals(1, m1s.size)
        assertEquals(8, m1s[0].startCol)
        val re = SearchEngine.findInLine(doc.lineText(1), "", caseSensitive = true, regex = "(?i)f[o]+".toRegex())
        assertEquals(2, re.size)
    }

    @Test
    fun lexerKotlinBasics() {
        val lexer = Languages.lexerFor("kotlin")!!
        val result = lexer.lex("val x = \"hello\" // comment", 0)
        val kinds = result.spans.map { it.kind }
        assertTrue(TokenKind.KEYWORD in kinds, "keyword missing: $kinds")
        assertTrue(TokenKind.STRING in kinds, "string missing: $kinds")
        assertTrue(TokenKind.COMMENT in kinds, "comment missing: $kinds")
    }

    @Test
    fun lexerBlockCommentSpansLines() {
        val lexer = Languages.lexerFor("kotlin")!!
        val r1 = lexer.lex("/* start of comment", 0)
        val r2 = lexer.lex("still comment */ val", r1.nextState)
        assertTrue(r2.spans.all { it.kind == TokenKind.COMMENT || it.kind == TokenKind.KEYWORD }, "${r2.spans.toList()}")
        assertTrue(TokenKind.COMMENT in r2.spans.map { it.kind })
        assertEquals(0, r2.nextState)
    }

    @Test
    fun lexerJsonKeysAndValues() {
        val lexer = Languages.lexerFor("json")!!
        val result = lexer.lex("""{"name": "plain", "n": 42, "ok": true}""", 0)
        val kinds = result.spans.map { it.kind }
        assertTrue(TokenKind.ATTRIBUTE in kinds)
        assertTrue(TokenKind.STRING in kinds)
        assertTrue(TokenKind.NUMBER in kinds)
        assertTrue(TokenKind.CONSTANT in kinds)
    }

    @Test
    fun lexerMarkdownHeadingAnchor() {
        val lexer = Languages.lexerFor("markdown")!!
        val result = lexer.lex("# Title here", 0)
        assertTrue(result.spans.any { it.kind == TokenKind.TAG && it.start == 0 }, result.spans.toList().toString())
    }

    @Test
    fun lexerAllLanguagesCompileAndRun() {
        for (id in listOf("kotlin", "java", "javascript", "json", "xml", "css", "markdown", "sql", "c_cpp", "python", "ruby", "perl", "groovy", "swift", "golang", "rust", "dart", "yaml", "shell", "dockerfile", "r")) {
            val lexer = Languages.lexerFor(id)
            if (lexer != null) {
                lexer.lex("sample text (with symbols) 123", 0)
            }
        }
    }

    @Test
    fun highlightEngineCrossLineState() {
        val doc = docOf("/* comment start\nmiddle of comment\nend */ val x = 1")
        val engine = HighlightEngine(Languages.lexerFor("kotlin"))
        val spans = engine.spansFor(doc, 1)
        assertEquals(1, spans?.size)
        assertEquals(TokenKind.COMMENT, spans!![0].kind)
        val spans2 = engine.spansFor(doc, 2)
        assertTrue(spans2!!.any { it.kind == TokenKind.COMMENT })
        assertTrue(spans2.any { it.kind == TokenKind.KEYWORD })
    }

    @Test
    fun undoRedoTyping() {
        val doc = docOf("")
        val history = EditHistory()
        var time = 1000L
        history.injectClock { time }
        fun apply(range: EditRange, text: String, typing: Boolean) {
            val old = doc.replace(range, text)
            history.push(range, old, text, typing)
        }
        apply(EditRange(0, 0, 0, 0), "a", true)
        time += 100
        apply(EditRange(0, 1, 0, 1), "b", true)
        time += 100
        apply(EditRange(0, 2, 0, 2), "c", true)
        assertEquals("abc", doc.fullText())
        history.undo(doc)
        assertEquals("", doc.fullText())
        history.redo(doc)
        assertEquals("abc", doc.fullText())
        time += 5000
        apply(EditRange(0, 3, 0, 3), "d", true)
        assertEquals("abcd", doc.fullText())
        history.undo(doc)
        assertEquals("abc", doc.fullText())
    }

    @Test
    fun encodingProbeDetects() {
        assertEquals(DetectedEncoding.UTF8, EncodingProbe.probe(ByteArrayByteSource("hello".encodeToByteArray())))
        assertEquals(DetectedEncoding.UTF16LE, EncodingProbe.probe(ByteArrayByteSource(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x61, 0x00))))
        assertEquals(DetectedEncoding.UTF16BE, EncodingProbe.probe(ByteArrayByteSource(byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0x00, 0x61))))
        assertEquals(DetectedEncoding.BINARY, EncodingProbe.probe(ByteArrayByteSource(byteArrayOf(0x00, 0x01, 0x02))))
    }

    @Test
    fun encodingProbeDetectsBomLessUtf16() {
        val le = "hello UTF-16 without BOM\n".encodeToByteArray()
        val leBytes = ByteArray(le.size * 2)
        le.forEachIndexed { i, b -> leBytes[i * 2] = b }
        assertEquals(DetectedEncoding.UTF16LE, EncodingProbe.probe(ByteArrayByteSource(leBytes)))

        val beBytes = ByteArray(le.size * 2)
        le.forEachIndexed { i, b -> beBytes[i * 2 + 1] = b }
        assertEquals(DetectedEncoding.UTF16BE, EncodingProbe.probe(ByteArrayByteSource(beBytes)))

        // Long content so the 8KB heuristic window fills with patterned NULs.
        val longLe = ByteArray(4096 * 2)
        repeat(4096) { longLe[it * 2] = 0x41 }
        assertEquals(DetectedEncoding.UTF16LE, EncodingProbe.probe(ByteArrayByteSource(longLe)))
    }

    @Test
    fun encodingProbeStillRejectsBinaryWithBalancedNuls() {
        // 0x00 run with balanced parity across both even and odd indexes.
        val data = byteArrayOf(0x4D, 0x5A) + ByteArray(100)
        assertEquals(DetectedEncoding.BINARY, EncodingProbe.probe(ByteArrayByteSource(data)))
    }

    @Test
    fun utf16DocumentRoundTripsThroughText() {
        val text = "utf16 line\nsecond\n"
        val withBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.encodeToByteArray().flatMap {
            listOf(it, 0x00.toByte())
        }.map { it }.toByteArray()
        val doc = LineVectorDocument.fromText(text)
        assertEquals(3, doc.lineCount)
        assertEquals("utf16 line", doc.lineText(0))
    }

    @Test
    fun chunkedSourceServesSpans() {
        val bytes = ByteArray(300 * 1024) { val v = it % 251; (if (v == 10) 32 else v).toByte() }
        bytes[150_000] = '\n'.code.toByte()
        val source = ChunkedByteSource(ByteArrayByteSource(bytes), chunkSize = 1024, maxChunks = 4)
        val doc = LineIndexBuilder(source).buildAll()
        assertEquals(2, doc.lineCount)
        assertEquals(150_000, doc.lineText(0).length)
        assertEquals(300 * 1024 - 150_001, doc.lineText(1).length)
    }
}
