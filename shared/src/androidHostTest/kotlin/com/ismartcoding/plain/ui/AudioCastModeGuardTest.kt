package com.ismartcoding.plain.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Locks the audio cast-mode interaction rules (2026-09-28, T68):
 * 1. the Cast Mode menu entry only shows on views with an audio list — the
 *    audio home sections (quick actions / artists / playlists) must not offer
 *    it; MediaTopBar gates the entry behind showCastModeMenu and AudioHomePage
 *    passes it only while a sidebar filter flattens the list;
 * 2. tapping an audio item in cast mode casts it immediately — castItem must
 *    not add the tapped track to the cast playlist and the row must not fall
 *    back to on-device playback (playAsync);
 * 3. the trailing queue toggle stays visible in cast mode but operates on the
 *    cast queue;
 * 4. the cast playlist opens from the corner FAB (CastQueueFab); no bottom
 *    cast player bar remains to be mistaken for on-device playback;
 * 5. every audio list page (all-items, artist, playlist) uses MediaTopBar so
 *    the cast-mode top bar (title + secondary container) behaves the same, and
 *    exposes sort + cast actions.
 * 6. a finished cast track auto-advances: next cast queue item, or the audio
 *    playback order when the cast queue is empty (DlnaRoutes
 *    advanceCastToNextTrack + castItem's onPlaying) — audio casts only.
 */
class AudioCastModeGuardTest {

    private val topBarPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/components/MediaTopBar.kt"
    private val homePath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/audio/AudioHomePage.kt"
    private val allPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/audio/AudioAllPage.kt"
    private val artistPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/audio/AudioArtistPage.kt"
    private val playlistPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/playlist/PlaylistDetailPage.kt"
    private val itemPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/audio/components/AudioListItem.kt"
    private val actionsPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/audio/components/AudioListItemActions.kt"
    private val castVmPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/models/CastViewModel.kt"
    private val dlnaRoutesPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/httpserver/routes/DlnaRoutes.kt"
    private val fabPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/cast/CastQueueFab.kt"

    private fun source(relPath: String): String {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        for (i in 0 until 4) {
            val f = File(dir, relPath)
            if (f.isFile) return f.readText()
            dir = dir.parentFile ?: break
        }
        fail("source not found: $relPath (from ${System.getProperty("user.dir")})")
    }

    /** Brace-matched body of top-level/class function [name], or the whole file when absent. */
    private fun functionBody(source: String, name: String): String? {
        val m = Regex("fun $name\\(").find(source) ?: return null
        val open = source.indexOf('{', m.range.last)
        if (open < 0) return null
        var depth = 0
        for (i in open until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(open, i + 1)
                }
            }
        }
        return null
    }

    @Test
    fun castModeMenuNeedsAListToCastFrom() {
        val topBar = source(topBarPath)
        assertTrue(
            "showCastModeMenu: Boolean = true" in topBar,
            "MediaTopBar must expose showCastModeMenu so callers without an audio list can hide the Cast Mode entry",
        )
        val castMenuLine = topBar.lineSequence().firstOrNull { "Res.string.cast_mode" in it }
            ?: fail("Cast Mode menu row not found in $topBarPath")
        val castMenuIndex = topBar.lineSequence().takeWhile { it != castMenuLine }.count()
        val guardLine = topBar.lineSequence().take(castMenuIndex).lastOrNull { it.trimStart().startsWith("if (") }
        assertTrue(
            guardLine != null && "showCastModeMenu" in guardLine,
            "the Cast Mode menu row must sit behind a condition mentioning showCastModeMenu, found guard: $guardLine",
        )
        val home = source(homePath)
        assertTrue(
            "showCastModeMenu = sidebarFilterActive" in home,
            "AudioHomePage must show the Cast Mode menu only while a sidebar filter flattens the list; the home sections view has no list to cast from",
        )
    }

    @Test
    fun listPagesUseSharedMediaTopBarWithSortAndCastActions() {
        for (path in listOf(allPath, artistPath, playlistPath)) {
            val src = source(path)
            assertTrue(
                "MediaTopBar(" in src,
                "$path must build its top bar through MediaTopBar so cast-mode title, tinted top bar and action hiding stay in sync across audio pages",
            )
            assertTrue(
                "showSortAndBrowseDialog.value = true" in src,
                "$path must expose a sort action opening the shared sort dialog",
            )
            assertTrue(
                "showCastDialog.value = true" in src,
                "$path shows an audio list and must expose a cast action",
            )
        }
    }

    @Test
    fun castPlaylistOpensFromCornerFab() {
        val fab = source(fabPath)
        assertTrue(
            "FloatingActionButton(" in fab && "AudioCastPlaylistPage(" in fab,
            "CastQueueFab is the cast-playlist entry: a corner FAB opening the sheet, replacing the old bottom cast player bar",
        )
        for (path in listOf(homePath, allPath, artistPath, playlistPath)) {
            val src = source(path)
            assertTrue(
                "CastQueueFab(" in src && "AudioCastPlayerBar(" !in src,
                "$path must host CastQueueFab and must not bring back a bottom cast player bar",
            )
        }
    }

    @Test
    fun rowToggleKeepsWorkingOnTheCastQueue() {
        val item = source(itemPath)
        assertTrue(
            "!dragSelectState.selectMode && !castVM.castMode.value" !in item,
            "the trailing queue toggle must stay visible in cast mode; it switches to cast-queue membership instead",
        )
        val actions = source(actionsPath)
        assertTrue(
            "if (castMode)" in actions && "onCastToggle" in actions,
            "AudioListItemActions must toggle cast-queue membership in cast mode",
        )
    }

    @Test
    fun tappingARowCastsWithoutQueueingIt() {
        val item = source(itemPath)
        val castBranch = "else if (castVM.castMode.value) { castVM.cast(item) }"
        val castIdx = item.indexOf(castBranch)
        if (castIdx < 0) fail("AudioListItem onClick must branch to castVM.cast(item) in cast mode")
        val playIdx = item.indexOf("playAsync(")
        assertTrue(
            playIdx < 0 || playIdx > castIdx,
            "playAsync may only run in the non-cast branch, after the cast branch",
        )
        val body = functionBody(source(castVmPath), "castItem")
            ?: fail("castItem not found in $castVmPath")
        assertTrue(
            "addItem(" !in body,
            "castItem casts the tapped track immediately; auto-adding it to the cast playlist is queue mutation the user did not ask for (the row toggle adds on purpose)",
        )
        assertTrue(
            "setAVTransportURIAsync(" in body && "playAVTransportAsync(" in body,
            "castItem must set the transport URI and start playback on the device",
        )
    }

    @Test
    fun trackEndFallsBackToThePlaybackOrderWhenCastQueueIsEmpty() {
        val routes = source(dlnaRoutesPath)
        val body = functionBody(routes, "advanceCastToNextTrack")
            ?: fail("advanceCastToNextTrack not found in $dlnaRoutesPath")
        assertTrue(
            "resolveNext(" in body,
            "tap-to-cast no longer populates the cast playlist, so a finished cast track must fall back to AudioQueueManager.resolveNext — otherwise nothing auto-plays after the first track",
        )
        assertTrue(
            "isAudioFast()" in body,
            "the playback-order fallback must be audio-only: a finished video/image cast must never pull in an audio track",
        )
        val castVm = source(castVmPath)
        val castBody = functionBody(castVm, "castItem")
            ?: fail("castItem not found in $castVmPath")
        assertTrue(
            "onPlaying(" in castBody,
            "castItem must mark the cast track current (onPlaying) so the fallback resolves the track after the one actually casting, not after the last locally played track",
        )
    }
}
