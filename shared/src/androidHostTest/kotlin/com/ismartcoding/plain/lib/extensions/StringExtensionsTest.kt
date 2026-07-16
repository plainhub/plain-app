package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the file/path/url helpers in [StringExtensions.kt] — pure
 * functions that drive file-type detection, URL validation, and HTML→text
 * conversion throughout the app.
 *
 * Methods that depend on platform `expect/actual` (e.g. `getFinalPath()`)
 * are deliberately NOT covered here — they require an Android Robolectric
 * environment or iOS host.
 */
class StringExtensionsTest {

    // ---------- filename helpers ----------

    @Test
    fun getFilenameFromPath_simple() {
        assertEquals("file.txt", "/path/to/file.txt".getFilenameFromPath())
    }

    @Test
    fun getFilenameFromPath_noSlash() {
        assertEquals("file.txt", "file.txt".getFilenameFromPath())
    }

    @Test
    fun getFilenameFromPath_trailingSlash() {
        // lastIndexOf("/") finds the trailing slash, +1 => empty
        assertEquals("", "/path/to/".getFilenameFromPath())
    }

    @Test
    fun getFilenameWithoutExtensionFromPath() {
        assertEquals("file", "/path/to/file.txt".getFilenameWithoutExtensionFromPath())
        // substringBeforeLast(".") only strips the LAST extension — for a
        // .tar.gz filename you get "archive.tar", not "archive".
        assertEquals("archive.tar", "archive.tar.gz".getFilenameWithoutExtensionFromPath())
    }

    @Test
    fun getFilenameWithoutExtension() {
        assertEquals("file", "file.txt".getFilenameWithoutExtension())
        assertEquals("noext", "noext".getFilenameWithoutExtension())
    }

    @Test
    fun getFilenameExtension_lowercaseAlways() {
        assertEquals("txt", "file.TXT".getFilenameExtension())
        assertEquals("mp4", "/a/b/c.MP4".getFilenameExtension())
        // When there's no '.', lastIndexOf returns -1, +1 = 0, substring(0)
        // returns the whole string (lowercased). Document this quirk so a
        // future refactor doesn't silently break callers that depend on it.
        assertEquals("noext", "noext".getFilenameExtension())
    }

    @Test
    fun getFilenameExtension_multipleDots() {
        assertEquals("gz", "archive.tar.gz".getFilenameExtension())
    }

    // ---------- cut ----------

    @Test
    fun cut_shorterThanLengthReturnsOriginal() {
        assertEquals("hello", "hello".cut(10))
    }

    @Test
    fun cut_exactLengthAppendsEllipsis() {
        // The implementation only short-circuits when `length > this.length`;
        // at exact equality it still appends "..." (substring(0, length) is
        // the full string plus an ellipsis).
        assertEquals("hello...", "hello".cut(5))
    }

    @Test
    fun cut_longerAppendsEllipsis() {
        assertEquals("hel...", "hello".cut(3))
    }

    @Test
    fun cut_zero() {
        assertEquals("...", "hello".cut(0))
    }

    // ---------- getParentPath ----------

    @Test
    fun getParentPath_simple() {
        assertEquals("/a/b", "/a/b/c.txt".getParentPath())
    }

    @Test
    fun getParentPath_noSlash() {
        assertEquals("file.txt", "file.txt".getParentPath())
    }

    @Test
    fun getParentPath_rootSlash() {
        // substringBeforeLast("/") on "/file.txt" -> ""
        assertEquals("", "/file.txt".getParentPath())
    }

    // ---------- file type detection ----------

    @Test
    fun isTextFile_commonExtensions() {
        assertTrue("notes.txt".isTextFile())
        assertTrue("readme.md".isTextFile())
        assertTrue("config.json".isTextFile())
        assertTrue("page.html".isTextFile())
        assertTrue("data.csv".isTextFile())
        assertTrue("script.sh".isTextFile())
        assertTrue("app.kt".isTextFile())
        assertTrue("Main.java".isTextFile())
    }

    @Test
    fun isTextFile_nonTextExtensionsReturnFalse() {
        assertFalse("image.jpg".isTextFile())
        assertFalse("video.mp4".isTextFile())
        assertFalse("song.mp3".isTextFile())
        assertFalse("doc.pdf".isTextFile())
    }

    @Test
    fun isTextFile_caseInsensitive() {
        assertTrue("README.MD".isTextFile())
        assertTrue("Config.JSON".isTextFile())
    }

    @Test
    fun isPdfFile() {
        assertTrue("doc.pdf".isPdfFile())
        assertTrue("doc.PDF".isPdfFile())
        assertFalse("doc.txt".isPdfFile())
    }

    @Test
    fun isZipFile() {
        assertTrue("archive.zip".isZipFile())
        assertTrue("archive.ZIP".isZipFile())
        assertFalse("archive.tar.gz".isZipFile())
    }

    @Test
    fun isVideoFast_videoExtensions() {
        assertTrue("clip.mp4".isVideoFast())
        assertTrue("clip.mkv".isVideoFast())
        assertTrue("clip.webm".isVideoFast())
        assertTrue("clip.avi".isVideoFast())
        assertTrue("clip.3gp".isVideoFast())
        assertTrue("clip.mov".isVideoFast())
        assertTrue("clip.m4v".isVideoFast())
        assertTrue("clip.3gpp".isVideoFast())
    }

    @Test
    fun isVideoFast_nonVideoExtensionsReturnFalse() {
        assertFalse("clip.jpg".isVideoFast())
        assertFalse("clip.mp3".isVideoFast())
    }

    @Test
    fun isVideoFast_caseInsensitive() {
        assertTrue("clip.MP4".isVideoFast())
    }

    @Test
    fun isImageFast_imageExtensions() {
        assertTrue("photo.jpg".isImageFast())
        assertTrue("photo.png".isImageFast())
        assertTrue("photo.gif".isImageFast())
        assertTrue("photo.webp".isImageFast())
        assertTrue("photo.svg".isImageFast())
        assertTrue("photo.heic".isImageFast())
    }

    @Test
    fun isImageFast_nonImageExtensionsReturnFalse() {
        assertFalse("photo.mp4".isImageFast())
        assertFalse("photo.txt".isImageFast())
    }

    @Test
    fun isAudioFast_audioExtensions() {
        assertTrue("song.mp3".isAudioFast())
        assertTrue("song.wav".isAudioFast())
        assertTrue("song.flac".isAudioFast())
        assertTrue("song.ogg".isAudioFast())
        assertTrue("song.m4a".isAudioFast())
        assertTrue("song.opus".isAudioFast())
        assertTrue("song.aac".isAudioFast())
        assertTrue("song.wma".isAudioFast())
    }

    @Test
    fun isAudioFast_nonAudioExtensionsReturnFalse() {
        assertFalse("song.mp4".isAudioFast())
    }

    @Test
    fun isRawFast_rawExtensions() {
        assertTrue("photo.dng".isRawFast())
        assertTrue("photo.nef".isRawFast())
        assertTrue("photo.cr2".isRawFast())
        assertTrue("photo.arw".isRawFast())
        assertFalse("photo.jpg".isRawFast())
    }

    @Test
    fun canModifyEXIF() {
        assertTrue("photo.jpg".canModifyEXIF())
        assertTrue("photo.jpeg".canModifyEXIF())
        assertTrue("photo.png".canModifyEXIF())
        assertTrue("photo.webp".canModifyEXIF())
        assertTrue("photo.dng".canModifyEXIF())
        assertFalse("photo.gif".canModifyEXIF())
    }

    @Test
    fun isPartialSupportVideo() {
        // .avi is in PARTIAL_SUPPORT_VIDEO_EXTENSIONS
        assertTrue("clip.avi".isPartialSupportVideo())
        assertFalse("clip.mp4".isPartialSupportVideo())
    }

    // ---------- isUrl ----------

    @Test
    fun isUrl_http() {
        assertTrue("http://example.com".isUrl())
    }

    @Test
    fun isUrl_https() {
        assertTrue("https://example.com".isUrl())
    }

    @Test
    fun isUrl_uppercaseScheme() {
        assertTrue("HTTP://example.com".isUrl())
        assertTrue("HTTPS://example.com".isUrl())
    }

    @Test
    fun isUrl_otherSchemesReturnFalse() {
        assertFalse("ftp://example.com".isUrl())
        assertFalse("file:///path".isUrl())
        assertFalse("mailto:a@b.com".isUrl())
    }

    @Test
    fun isUrl_noSchemeReturnFalse() {
        assertFalse("example.com".isUrl())
        assertFalse("www.example.com".isUrl())
    }

    @Test
    fun isUrl_malformedReturnFalse() {
        // "http://" alone is parseable by URLBuilder (it's a valid URL with
        // an empty host) — so isUrl returns true. Only clearly non-URL
        // strings return false.
        assertFalse("not a url at all".isUrl())
        assertFalse("example".isUrl())
    }

    // ---------- containsChinese ----------

    @Test
    fun containsChinese_trueForChineseChars() {
        assertTrue("hello中国".containsChinese())
        assertTrue("中文".containsChinese())
        assertTrue("a中b".containsChinese())
    }

    @Test
    fun containsChinese_falseForAsciiOnly() {
        assertFalse("hello world".containsChinese())
        assertFalse("123!@#".containsChinese())
        assertFalse("".containsChinese())
    }

    @Test
    fun containsChinese_falseForOtherScripts() {
        assertFalse("héllo".containsChinese())
        assertFalse("привет".containsChinese())
        // Japanese Hiragana is outside the CJK Unified Ideographs block
        assertFalse("こんにちは".containsChinese())
    }

    // ---------- htmlToPlainText ----------

    @Test
    fun htmlToPlainText_stripsTags() {
        assertEquals("hello world", "<p>hello world</p>".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_nestedTagsStripped() {
        assertEquals("hello world", "<p>hello <b>world</b></p>".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_brConvertsToNewlineThenCollapsed() {
        // <br> initially becomes "\n", but the final `\s+` -> " " collapse
        // turns it back into a space. Verify the actual contract: the function
        // returns a single-line plain text representation.
        assertEquals("line one line two", "line one<br>line two".htmlToPlainText())
        assertEquals("line one line two", "line one<br/>line two".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_closePAndDivAndLiAddNewlineThenCollapsed() {
        // </p>, </div>, </li> each add "\n", but the `\s+` collapse step
        // merges them into a single space.
        assertEquals("p1 p2", "<p>p1</p><p>p2</p>".htmlToPlainText())
        assertEquals("d1 d2", "<div>d1</div><div>d2</div>".htmlToPlainText())
        assertEquals("a b", "<ul><li>a</li><li>b</li></ul>".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_decodesEntities() {
        assertEquals("a & b < c > d \" e ' f", "a &amp; b &lt; c &gt; d &quot; e &#39; f".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_nbspDecodedToSpace() {
        assertEquals("a b", "a&nbsp;b".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_collapsesWhitespace() {
        assertEquals("a b c", "a   b   c".htmlToPlainText())
    }

    @Test
    fun htmlToPlainText_trimsLeadingTrailing() {
        assertEquals("hi", "   <p>hi</p>   ".htmlToPlainText())
    }

    // ---------- getMimeType ----------

    @Test
    fun getMimeType_knownExtension() {
        assertEquals("text/plain", "file.txt".getMimeType())
        assertEquals("application/pdf", "file.pdf".getMimeType())
        assertEquals("image/jpeg", "file.jpg".getMimeType())
        assertEquals("image/png", "file.png".getMimeType())
        assertEquals("video/mp4", "file.mp4".getMimeType())
        assertEquals("audio/mpeg", "file.mp3".getMimeType())
        assertEquals("application/json", "file.json".getMimeType())
        assertEquals("text/html", "file.html".getMimeType())
    }

    @Test
    fun getMimeType_unknownExtensionReturnsEmpty() {
        assertEquals("", "file.unknownext".getMimeType())
        assertEquals("", "noext".getMimeType())
    }

    @Test
    fun getMimeType_caseInsensitive() {
        assertEquals("application/pdf", "file.PDF".getMimeType())
        assertEquals("image/jpeg", "file.JPEG".getMimeType())
    }

    // ---------- splitInParts ----------

    @Test
    fun splitInParts_returnsAllParts() {
        val text = "abcdefghijklmnopqrstuvwxyz"
        val parts = text.splitInParts(10, arrayOf(' ', ',', '.'))
        // 26 chars / 10 = ceil(2.6) = 3 parts
        assertEquals(3, parts.size)
        assertEquals("abcdefghij", parts[0])
        // The remaining string is 16 chars; parts[1] should be 10 chars unless a split char is found.
        assertEquals("klmnopqrst", parts[1])
        assertEquals("uvwxyz", parts[2])
    }

    @Test
    fun splitInParts_exactMultiple() {
        val text = "abcdef"
        val parts = text.splitInParts(3, arrayOf(' '))
        assertEquals(2, parts.size)
        assertEquals("abc", parts[0])
        assertEquals("def", parts[1])
    }

    @Test
    fun splitInParts_shorterThanLenReturnsSinglePart() {
        val text = "hi"
        val parts = text.splitInParts(10, arrayOf(' '))
        assertEquals(1, parts.size)
        assertEquals("hi", parts[0])
    }

    // ---------- List<String>.getMimeType ----------

    @Test
    fun listMimeType_sameTypeReturnsSameType() {
        val mime = listOf("a.txt", "b.txt").getMimeType()
        assertEquals("text/plain", mime)
    }

    @Test
    fun listMimeType_sameGroupDifferentSubtypeReturnsGroupStar() {
        val mime = listOf("a.jpg", "b.png").getMimeType()
        assertEquals("image/*", mime)
    }

    @Test
    fun listMimeType_differentGroupsReturnsStarStar() {
        val mime = listOf("a.jpg", "b.mp4").getMimeType()
        assertEquals("*/*", mime)
    }

    @Test
    fun listMimeType_emptyListReturnsStarStar() {
        // No entries — mimeGroups and subtypes both empty, falls to "*/*"
        assertEquals("*/*", emptyList<String>().getMimeType())
    }
}
