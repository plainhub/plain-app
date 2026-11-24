package com.ismartcoding.plain.ui.search

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobalSearchControlsTest {
    private fun searchSource(file: String): String {
        val path = "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/search/$file.kt"
        return generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .map { File(it, path) }.first { it.isFile }.readText()
    }

    @Test
    fun devicePickerRendersTheArrowWithAPainterAndReusesTheRoundCheckbox() {
        val source = searchSource("GlobalSearchDeviceFilter")
        assertTrue("painterResource(UiRes.drawable.chevron_down)" in source, "PIcon must receive a painter to render the arrow")
        assertTrue("CheckCircle(" in source, "The device rows must render the shared round checkbox")
        assertFalse("TriStateCheckbox(" in source)
    }

    @Test
    fun pressFeedbackIsClippedBeforeTheControlHandlesInput() {
        val picker = searchSource("GlobalSearchDeviceFilter").substringBefore("    if (open)")
        val tabs = searchSource("GlobalSearchTabs")
        assertTrue(
            Regex("""\.clip\(CircleShape\)\.background\(colors\.primaryContainer\)\s*\.clickable""").containsMatchIn(picker),
            "The device button must clip its press feedback to the same shape as its fill",
        )
        assertTrue(
            Regex("""\.clip\(MaterialTheme\.shapes\.small\)\s*\.selectable""").containsMatchIn(tabs),
            "Tabs must clip their press feedback to rounded corners",
        )
    }

    @Test
    fun emptySearchKeepsItsTopControlsWithoutExtraButtonsOrHistory() {
        val results = searchSource("GlobalSearchResults")
        assertFalse("Res.string.search_in_all_types" in results)
        assertFalse("Res.string.clear_search_term" in results)
        assertFalse("GlobalSearchRecentSection(" in results)
        val page = searchSource("GlobalSearchPage")
        assertFalse("GlobalSearchRecentSection(" in page)
        assertTrue("GlobalSearchTabs(" in page)
        assertTrue("Res.string.clear_search_term" in searchSource("GlobalSearchTopBar"))
        assertFalse("SearchHistoryClient" in searchSource("GlobalSearchViewModel"))
    }
}
