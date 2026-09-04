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

    // ── toolbar actions ───

    @Test
    fun `toggleWrap wraps selection and unwraps again`() {
        val s = stateWith("hello world")
        s.buffer.edit { selection = TextRange(0, 5) }
        s.toggleWrap("**")
        assertEquals("**hello** world", s.text())
        s.buffer.edit { selection = TextRange(0, 9) }
        s.toggleWrap("**")
        assertEquals("hello world", s.text())
    }

    @Test
    fun `toggleLinePrefix toggles heading levels`() {
        val s = stateWith("title")
        s.toggleLinePrefix("## ")
        assertEquals("## title", s.text())
        s.toggleLinePrefix("# ")
        assertEquals("# title", s.text())
        s.toggleLinePrefix("# ")
        assertEquals("title", s.text())
    }

    @Test
    fun `toggleLinePrefix strips list and callout markers`() {
        val s = stateWith("- [ ] task")
        s.toggleLinePrefix("")
        assertEquals("task", s.text())
        s.toggleLinePrefix("> [!note] ")
        assertEquals("> [!note] task", s.text())
        s.toggleLinePrefix("")
        assertEquals("task", s.text())
    }

    // ── list continuation ───

    @Test
    fun `enter after a bulleted item continues the marker`() {
        val s = stateWith("- one")
        s.buffer.edit { append("\n"); setSelection(6) }
        s.splitActiveBlock()
        assertEquals(listOf("- one", "- "), s.blocks.map { it.content() })
    }

    @Test
    fun `enter after a numbered item increments the number`() {
        val s = stateWith("3. item")
        s.buffer.edit { append("\n"); setSelection(8) }
        s.splitActiveBlock()
        assertEquals(listOf("3. item", "4. "), s.blocks.map { it.content() })
    }

    @Test
    fun `enter on a marker-only line clears the marker`() {
        val s = stateWith("- ")
        s.buffer.edit { append("\n"); setSelection(3) }
        s.splitActiveBlock()
        assertEquals(listOf(""), s.blocks.map { it.content() })
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
    fun `loadText activates the first block`() {
        val s = BlockEditorState()
        s.loadText("hello")
        assertEquals(s.blocks[0].id, s.focusedBlockId.value)
        assertEquals("hello", s.buffer.text.toString())
    }

    // ── Enter / multiline split ───

    @Test
    fun `newline in a text block splits into two blocks with caret in the second`() {
        val s = stateWith("")
        s.buffer.edit {
            append("first\nsecond")
            setSelection(6)
        }
        s.splitActiveBlock()
        assertEquals("first\nsecond", s.text())
        assertEquals(2, s.blocks.size)
        val target = s.blocks.first { it.content() == "second" }
        assertEquals(target.id, s.focusedBlockId.value)
        assertEquals(TextRange(0), s.buffer.selection)
        assertEquals("second", s.buffer.text.toString())
    }

    @Test
    fun `single image line becomes an atomic image block`() {
        val s = stateWith("")
        s.buffer.edit { append("![pic](u.png)") }
        s.splitActiveBlock()
        // no newline: nothing to split, but the image line should become an IMAGE block
        s.normalize()
        assertEquals(1, s.blocks.size)
        assertEquals(MdBlockKind.IMAGE, s.blocks[0].kind)
    }

    @Test
    fun `toolbar code insert splits with the code block activated`() {
        val s = stateWith("hello")
        s.buffer.edit { append("```\n\n```"); setSelection(length) }
        s.splitActiveBlock()
        assertEquals(listOf(MdBlockKind.TEXT, MdBlockKind.TEXT, MdBlockKind.CODE), s.blocks.map { it.kind })
        val code = s.blocks[2]
        assertEquals(code.id, s.focusedBlockId.value)
        assertEquals("```", s.buffer.text.toString())
        assertEquals("hello```\n\n```", s.text())
    }

    // ── backspace merges ───

    @Test
    fun `backspace in empty block deletes it and activates previous end`() {
        val s = stateWith("hello", "")
        s.activate(s.blocks[1], 0)
        assertTrue(s.backspaceAtStart())
        assertEquals(1, s.blocks.size)
        assertEquals("hello", s.buffer.text.toString())
        assertEquals(s.blocks[0].id, s.focusedBlockId.value)
    }

    @Test
    fun `backspace at start of non-empty block merges previous block into the buffer`() {
        val s = stateWith("hello", "world")
        s.activate(s.blocks[1], 0)
        assertTrue(s.backspaceAtStart())
        assertEquals(1, s.blocks.size)
        assertEquals("helloworld", s.text())
        assertEquals(s.blocks[0].id, s.focusedBlockId.value)
        assertEquals(TextRange(5), s.buffer.selection)
    }

    @Test
    fun `backspace at start of first block is a no-op`() {
        val s = stateWith("hello")
        assertFalse(s.backspaceAtStart())
        assertEquals(1, s.blocks.size)
    }

    @Test
    fun `backspace after a rendered atomic block opens it in source mode`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```\n")
        val atomic = s.blocks[0]
        s.activate(s.blocks[1], 0)
        assertTrue(s.backspaceAtStart())
        assertEquals(MdBlockKind.CODE, atomic.kind)
        assertEquals(atomic.id, s.focusedBlockId.value)
        assertEquals("```\ncode\n```", s.buffer.text.toString())
    }

    // ── atomic commit ───

    @Test
    fun `committing an atomic block whose content is no longer atomic splits it back`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```\ntail")
        s.buffer.edit {
            replace(0, length, "just text now")
        }
        s.activate(s.blocks[1], 0)
        assertEquals(2, s.blocks.size)
        assertEquals(MdBlockKind.TEXT, s.blocks[0].kind)
        assertEquals("just text now\ntail", s.text())
    }

    @Test
    fun `committing an unchanged atomic block keeps it rendered`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```\ntail")
        s.activate(s.blocks[1], 0)
        assertEquals(MdBlockKind.CODE, s.blocks[0].kind)
        assertEquals("```\ncode\n```\ntail", s.text())
    }

    // ── normalize ───

    @Test
    fun `normalize merges separately typed table lines into one atomic block`() {
        // start as three plain text lines (middle row not yet a separator), then type the separator
        val s = stateWith("| a | b |", "| x |", "| 1 | 2 |")
        s.activate(s.blocks[1], s.blocks[1].content().length)
        s.buffer.edit {
            replace(0, length, "|---|---|")
        }
        s.normalize()
        assertEquals(1, s.blocks.size)
        assertEquals(MdBlockKind.TABLE, s.blocks[0].kind)
        assertEquals("| a | b |\n|---|---|\n| 1 | 2 |", s.blocks[0].content())
        assertEquals(s.blocks[0].id, s.focusedBlockId.value)
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
    fun `insert into the active block wraps the caret`() {
        val s = stateWith("hello")
        s.buffer.edit { setSelection(5) }
        s.insertAtFocused("**", "**")
        assertEquals("hello****", s.text())
    }

    @Test
    fun `insert into a rendered atomic block creates a text block after it`() {
        val s = BlockEditorState()
        s.loadText("```\ncode\n```")
        s.insertText("tail")
        assertEquals("```\ncode\n```\ntail", s.text())
        assertEquals(MdBlockKind.TEXT, s.blocks[1].kind)
        assertEquals("tail", s.buffer.text.toString())
    }

    // ── undo / redo ───

    @Test
    fun `undo restores the text before an edit`() {
        val s = BlockEditorState()
        s.loadText("a")
        s.buffer.edit { append("b") }
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
        s.activate(s.blocks[1], 0)
        s.blocks[0].state.edit { append("!") }
        assertTrue(s.backspaceAtStart())
        assertEquals("hello!world", s.text())
        s.undo()
        assertEquals("hello\nworld", s.text())
        assertEquals(2, s.blocks.size)
    }

    @Test
    fun `typing after undo abandons the old redo branch`() {
        val s = BlockEditorState()
        s.loadText("a")
        s.buffer.edit { append("b") }
        s.undo()
        assertTrue(s.canRedo.value)
        s.buffer.edit { append("c") }
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
