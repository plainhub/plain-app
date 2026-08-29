package com.ismartcoding.plain.ui.base.mdeditor.blocks

import androidx.compose.ui.text.TextRange
import com.ismartcoding.plain.ui.extensions.setSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockEditorStateTest {

    private fun stateWith(vararg lines: String): BlockEditorState {
        val s = BlockEditorState()
        s.loadText(lines.joinToString("\n"))
        return s
    }

    // ── load / serialize ───

    @Test
    fun `loadText then text roundtrips the document`() {
        val s = BlockEditorState()
        val doc = "# Title\n\ntext\n\n```kotlin\nval a = 1\n```\n"
        s.loadText(doc)
        assertEquals(doc, s.text())
    }

    @Test
    fun `loadText parses atomic blocks`() {
        val s = BlockEditorState()
        s.loadText("a\n```\ncode\n```\nb")
        assertEquals(3, s.blocks.size)
        assertEquals(MdBlockKind.TEXT, s.blocks[0].kind)
        assertEquals(MdBlockKind.CODE, s.blocks[1].kind)
        assertEquals(MdBlockKind.TEXT, s.blocks[2].kind)
    }

    @Test
    fun `loadText queues focus on the first block`() {
        val s = BlockEditorState()
        s.loadText("hello")
        assertEquals(s.blocks[0].id, s.pendingFocus.value)
    }

    // ── Enter / multiline split ───

    @Test
    fun `newline in a text block splits into two blocks with caret in the second`() {
        val s = stateWith("")
        val b = s.blocks[0]
        b.state.edit {
            append("first\nsecond")
            setSelection(6)
        }
        s.splitMultilineBlock(b)
        assertEquals("first\nsecond", s.text())
        assertEquals(2, s.blocks.size)
        val target = s.blocks.first { it.content() == "second" }
        assertEquals(target.id, s.focusedBlockId.value)
        assertEquals(TextRange(0), target.state.selection)
    }

    @Test
    fun `split morphing a single line into an atomic block keeps it editable`() {
        val s = stateWith("")
        val b = s.blocks[0]
        s.focusedBlockId.value = b.id
        b.state.edit { append("![pic](u.png)") }
        s.splitMultilineBlock(b)
        // no newline: nothing to split, but the image line should become an IMAGE block in source mode
        s.normalize()
        assertEquals(1, s.blocks.size)
        assertEquals(MdBlockKind.IMAGE, s.blocks[0].kind)
        assertTrue(s.blocks[0].editing)
    }

    @Test
    fun `toolbar code insert splits with the code block opened in source mode`() {
        val s = stateWith("hello")
        val b = s.blocks[0]
        s.focusedBlockId.value = b.id
        b.state.edit { append("```\n\n```") }
        s.splitMultilineBlock(b)
        assertEquals(listOf(MdBlockKind.TEXT, MdBlockKind.TEXT, MdBlockKind.CODE), s.blocks.map { it.kind })
        val code = s.blocks[2]
        assertTrue(code.editing)
        assertEquals(code.id, s.focusedBlockId.value)
        assertEquals("hello```\n\n```", s.text())
    }

    // ── backspace merges ───

    @Test
    fun `backspace in empty block deletes it and focuses previous end`() {
        val s = stateWith("hello", "")
        val removed = s.blocks[1]
        s.focusedBlockId.value = removed.id
        assertTrue(s.backspaceAtStart(removed))
        assertEquals(1, s.blocks.size)
        assertEquals("hello", s.focusedBlock()?.content())
    }

    @Test
    fun `backspace at start of non-empty block merges into previous text block`() {
        val s = stateWith("hello", "world")
        val second = s.blocks[1]
        second.state.edit { setSelection(0) }
        s.focusedBlockId.value = second.id
        assertTrue(s.backspaceAtStart(second))
        assertEquals(1, s.blocks.size)
        assertEquals("helloworld", s.text())
        assertEquals(s.blocks[0].id, s.focusedBlockId.value)
        assertEquals(TextRange(5), s.blocks[0].state.selection)
    }

    @Test
    fun `backspace at start of first block is a no-op`() {
        val s = stateWith("hello")
        assertFalse(s.backspaceAtStart(s.blocks[0]))
        assertEquals(1, s.blocks.size)
    }

    @Test
    fun `backspace after a rendered atomic block opens it in source mode`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```\n")
        val atomic = s.blocks[0]
        val empty = s.blocks[1]
        s.focusedBlockId.value = empty.id
        assertTrue(s.backspaceAtStart(empty))
        assertTrue(atomic.editing)
        assertEquals(atomic.id, s.focusedBlockId.value)
    }

    // ── atomic commit ───

    @Test
    fun `committing an atomic block whose content is no longer atomic splits it back`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```")
        val atomic = s.blocks[0]
        atomic.editing = true
        atomic.state.edit {
            replace(0, length, "just text now")
        }
        s.commitAtomic(atomic)
        assertFalse(atomic.editing)
        assertEquals(1, s.blocks.size)
        assertEquals(MdBlockKind.TEXT, s.blocks[0].kind)
        assertEquals("just text now", s.text())
    }

    @Test
    fun `committing an unchanged atomic block keeps it rendered`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```")
        val atomic = s.blocks[0]
        atomic.editing = true
        s.commitAtomic(atomic)
        assertFalse(atomic.editing)
        assertEquals(MdBlockKind.CODE, s.blocks[0].kind)
        assertEquals("```\ncode\n```", s.text())
    }

    // ── normalize ───

    @Test
    fun `normalize merges separately typed table lines into one atomic block`() {
        // start as three plain text lines (middle row not yet a separator), then type the separator
        val s = stateWith("| a | b |", "| x |", "| 1 | 2 |")
        val middle = s.blocks[1]
        s.focusedBlockId.value = middle.id
        middle.state.edit {
            replace(0, length, "|---|---|")
        }
        s.normalize()
        assertEquals(1, s.blocks.size)
        assertEquals(MdBlockKind.TABLE, s.blocks[0].kind)
        assertEquals("| a | b |\n|---|---|\n| 1 | 2 |", s.blocks[0].content())
        assertEquals(s.blocks[0].id, s.focusedBlockId.value)
        assertTrue(s.blocks[0].editing)
    }

    @Test
    fun `normalize keeps text structure stable`() {
        val s = stateWith("a", "b")
        val ids = s.blocks.map { it.id }
        s.normalize()
        assertEquals(ids, s.blocks.map { it.id })
        assertEquals("a\nb", s.text())
    }

    // ── toolbar inserts ───

    @Test
    fun `insert without focus appends to a new text block at the end`() {
        val s = stateWith("hello")
        assertNull(s.focusedBlockId.value)
        s.insertAtFocused("**", "**")
        assertEquals("hello\n****", s.text())
    }

    @Test
    fun `insert into a rendered atomic block creates a text block after it`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```")
        s.focusedBlockId.value = s.blocks[0].id
        s.insertText("tail")
        assertEquals("```\ncode\n```\ntail", s.text())
    }

    // ── undo / redo ───

    @Test
    fun `undo restores the text before an edit`() {
        val s = BlockEditorState()
        s.loadText("a")
        s.blocks[0].state.edit { append("b") }
        s.undo()
        assertEquals("a", s.text())
        assertFalse(s.canUndo.value)
        assertTrue(s.canRedo.value)
        s.redo()
        assertEquals("ab", s.text())
    }

    @Test
    fun `undo covers structural merges`() {
        val s = stateWith("hello", "world")
        s.focusedBlockId.value = s.blocks[1].id
        s.blocks[1].state.edit { setSelection(0) }
        s.blocks[0].state.edit { append("!") }
        assertTrue(s.backspaceAtStart(s.blocks[1]))
        assertEquals("hello!world", s.text())
        s.undo()
        assertEquals("hello\nworld", s.text())
        assertEquals(2, s.blocks.size)
    }

    @Test
    fun `typing after undo abandons the old redo branch`() {
        val s = BlockEditorState()
        s.loadText("a")
        s.blocks[0].state.edit { append("b") }
        s.undo()
        assertTrue(s.canRedo.value)
        s.blocks[0].state.edit { append("c") }
        s.undo()
        assertEquals("a", s.text())
        // the only redoable state is the second typing, not the abandoned "ab" branch
        s.redo()
        assertEquals("ac", s.text())
    }

    // ── cross-block selection ───

    @Test
    fun `selection spans whole blocks and produces document text`() {
        val s = stateWith("hello", "brave", "world")
        s.enterSelectionMode()
        s.tapBlockInSelection(s.blocks[0])
        s.tapBlockInSelection(s.blocks[2])
        assertEquals("hello\nbrave\nworld", s.selectedText())
        assertTrue(s.isBlockSelected(s.blocks[1].id))
    }

    @Test
    fun `reversed selection still yields ordered range`() {
        val s = stateWith("aa", "bb", "cc")
        s.enterSelectionMode()
        s.tapBlockInSelection(s.blocks[2])
        s.tapBlockInSelection(s.blocks[0])
        assertEquals("aa\nbb\ncc", s.selectedText())
    }

    @Test
    fun `boundary refinement crops selected text`() {
        val s = stateWith("hello", "world")
        s.enterSelectionMode()
        s.tapBlockInSelection(s.blocks[0])
        s.tapBlockInSelection(s.blocks[1])
        s.startRefine(s.blocks[0])
        assertEquals(TextRange(0, 5), s.blocks[0].state.selection)
        // simulate a handle drag inside the refining block
        s.blocks[0].state.edit { selection = TextRange(1, 5) }
        s.updateSelectionBoundary(s.blocks[0])
        assertEquals("ello\nworld", s.selectedText())
    }

    @Test
    fun `delete selected range removes content and is undoable`() {
        val s = stateWith("aa", "bb", "cc")
        s.enterSelectionMode()
        s.tapBlockInSelection(s.blocks[0])
        s.tapBlockInSelection(s.blocks[2])
        s.deleteSelectedRange()
        assertEquals("", s.text())
        assertFalse(s.selectionMode)
        s.undo()
        assertEquals("aa\nbb\ncc", s.text())
    }

    @Test
    fun `partial boundary selection crops first and last blocks`() {
        val s = stateWith("hello", "mid", "world")
        s.enterSelectionMode()
        s.tapBlockInSelection(s.blocks[0])
        s.tapBlockInSelection(s.blocks[2])
        s.selectionAnchor = BlockAnchor(s.blocks[0].id, 2)
        s.selectionFocus = BlockAnchor(s.blocks[2].id, 3)
        assertEquals("llo\nmid\nwor", s.selectedText())
        s.deleteSelectedRange()
        // the deleted range swallows both newlines inside it, joining the remainders
        assertEquals("held", s.text())
    }

    @Test
    fun `select all covers the whole document`() {
        val s = stateWith("aa", "```\ncode\n```")
        s.enterSelectionMode()
        s.selectAllBlocks()
        assertEquals("aa\n```\ncode\n```", s.selectedText())
    }
}
