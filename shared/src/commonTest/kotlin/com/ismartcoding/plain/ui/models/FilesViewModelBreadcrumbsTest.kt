package com.ismartcoding.plain.ui.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilesViewModelBreadcrumbsTest {

    private val rootPath = "/storage/emulated/0"
    private val rootName = "Internal Storage"

    @Test
    fun `root path returns single root breadcrumb with index zero`() {
        val (items, selectedIndex) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, rootPath)

        assertEquals(1, items.size)
        assertEquals(rootName, items[0].name)
        assertEquals(rootPath, items[0].path)
        assertEquals(0, selectedIndex)
    }

    @Test
    fun `direct child path produces two breadcrumbs with child selected`() {
        val target = "$rootPath/Music"
        val (items, selectedIndex) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, target)

        assertEquals(2, items.size)
        assertEquals(rootName, items[0].name)
        assertEquals(rootPath, items[0].path)
        assertEquals("Music", items[1].name)
        assertEquals(target, items[1].path)
        assertEquals(1, selectedIndex)
    }

    @Test
    fun `deeply nested path produces one breadcrumb per segment`() {
        val target = "$rootPath/Music/Albums/Best"
        val (items, selectedIndex) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, target)

        assertEquals(4, items.size)
        assertEquals(rootName, items[0].name)
        assertEquals("Music", items[1].name)
        assertEquals("Albums", items[2].name)
        assertEquals("Best", items[3].name)
        assertEquals("$rootPath/Music", items[1].path)
        assertEquals("$rootPath/Music/Albums", items[2].path)
        assertEquals(target, items[3].path)
        assertEquals(3, selectedIndex)
    }

    @Test
    fun `breadcrumb paths are cumulative and correctly joined`() {
        val target = "$rootPath/a/b/c"
        val (items, _) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, target)

        assertEquals(listOf(rootPath, "$rootPath/a", "$rootPath/a/b", "$rootPath/a/b/c"), items.map { it.path })
    }

    @Test
    fun `path equal to root with trailing slash still resolves to root`() {
        val target = "$rootPath/"
        val (items, selectedIndex) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, target)

        assertEquals(1, items.size)
        assertEquals(0, selectedIndex)
    }

    @Test
    fun `empty segments from double slashes are skipped`() {
        val target = "$rootPath//Music//Albums"
        val (items, _) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, target)

        // Root + Music + Albums (empty segments are skipped)
        assertEquals(3, items.size)
        assertEquals("Music", items[1].name)
        assertEquals("Albums", items[2].name)
    }

    @Test
    fun `selected index is last index for non-root path`() {
        val target = "$rootPath/Documents/Work/Notes"
        val (_, selectedIndex) = FilesViewModel.buildRegularBreadcrumbs(rootPath, rootName, target)

        assertTrue(selectedIndex > 0)
    }
}
