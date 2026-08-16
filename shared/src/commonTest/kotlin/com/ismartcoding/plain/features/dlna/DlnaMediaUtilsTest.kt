package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DlnaMediaUtilsTest {

    // ── classifyByMime ───

    @Test
    fun `classifyByMime returns VIDEO for common video mime types`() {
        val videoMimes = listOf(
            "video/mp4",
            "video/mp2t",
            "video/mpeg",
            "video/x-matroska",
            "video/webm",
            "video/x-msvideo",
            "video/quicktime",
        )
        videoMimes.forEach { mime ->
            assertEquals(DlnaMediaType.VIDEO, DlnaMediaUtils.classifyByMime(mime), "failed for $mime")
        }
    }

    @Test
    fun `classifyByMime returns AUDIO for common audio mime types`() {
        val audioMimes = listOf(
            "audio/mpeg",
            "audio/mp4",
            "audio/flac",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/aac",
            "audio/opus",
        )
        audioMimes.forEach { mime ->
            assertEquals(DlnaMediaType.AUDIO, DlnaMediaUtils.classifyByMime(mime), "failed for $mime")
        }
    }

    @Test
    fun `classifyByMime returns IMAGE for common image mime types`() {
        val imageMimes = listOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp",
            "image/heic",
            "image/heif",
        )
        imageMimes.forEach { mime ->
            assertEquals(DlnaMediaType.IMAGE, DlnaMediaUtils.classifyByMime(mime), "failed for $mime")
        }
    }

    @Test
    fun `classifyByMime returns UNKNOWN for text unknown or empty`() {
        assertEquals(DlnaMediaType.UNKNOWN, DlnaMediaUtils.classifyByMime("text/plain"))
        assertEquals(DlnaMediaType.UNKNOWN, DlnaMediaUtils.classifyByMime("application/pdf"))
        assertEquals(DlnaMediaType.UNKNOWN, DlnaMediaUtils.classifyByMime("application/octet-stream"))
        assertEquals(DlnaMediaType.UNKNOWN, DlnaMediaUtils.classifyByMime(""))
        assertEquals(DlnaMediaType.UNKNOWN, DlnaMediaUtils.classifyByMime("bogus/type"))
    }

    // ── getUpnpClass ───

    @Test
    fun `getUpnpClass returns correct class per media type`() {
        assertEquals("object.item.videoItem", DlnaMediaUtils.getUpnpClass(DlnaMediaType.VIDEO))
        assertEquals("object.item.audioItem.musicTrack", DlnaMediaUtils.getUpnpClass(DlnaMediaType.AUDIO))
        assertEquals("object.item.imageItem.photo", DlnaMediaUtils.getUpnpClass(DlnaMediaType.IMAGE))
        assertEquals("object.item", DlnaMediaUtils.getUpnpClass(DlnaMediaType.UNKNOWN))
    }

    // ── getProtocolInfo ───

    @Test
    fun `getProtocolInfo builds http-get star mime star`() {
        assertEquals("http-get:*:video/mp4:*", DlnaMediaUtils.getProtocolInfo("video/mp4"))
        assertEquals("http-get:*:audio/mpeg:*", DlnaMediaUtils.getProtocolInfo("audio/mpeg"))
        assertEquals("http-get:*:image/jpeg:*", DlnaMediaUtils.getProtocolInfo("image/jpeg"))
        assertEquals("http-get:*:video/mp2t:*", DlnaMediaUtils.getProtocolInfo("video/mp2t"))
    }

    @Test
    fun `getProtocolInfo falls back to octet-stream for empty mime`() {
        assertEquals(
            "http-get:*:application/octet-stream:*",
            DlnaMediaUtils.getProtocolInfo(""),
        )
    }

    // ── getMimeTypeFromExt (via determineMimeType + fake path) ───

    @Test
    fun `determineMimeType maps extension for fake paths where platform resolver returns null`() {
        // These file paths don't exist on disk, so platform resolver returns null and
        // the internal extension map kicks in.
        assertEquals("video/mp4", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/movie.mp4"))
        assertEquals("video/mp2t", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/stream.ts"))
        assertEquals("video/x-matroska", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/film.mkv"))
        assertEquals("audio/mpeg", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/song.mp3"))
        assertEquals("audio/flac", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/track.flac"))
        assertEquals("audio/wav", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/clip.wav"))
        assertEquals("image/jpeg", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/pic.jpg"))
        assertEquals("image/jpeg", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/pic.jpeg"))
        assertEquals("image/png", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/pic.png"))
        assertEquals("image/webp", DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/pic.webp"))
    }

    @Test
    fun `determineMimeType returns octet-stream for unknown extension`() {
        assertEquals(
            "application/octet-stream",
            DlnaMediaUtils.determineMimeType("/tmp/does-not-exist/data.xyzxyz"),
        )
    }

    // ── determineMimeTypeFromMediaUrl (url path without decrypt) ───

    @Test
    fun `determineMimeTypeFromMediaUrl reads path extension for plain media URLs`() {
        assertEquals(
            "video/mp4",
            DlnaMediaUtils.determineMimeTypeFromMediaUrl("http://host:1234/media/cdn/demo.mp4", null),
        )
        assertEquals(
            "audio/mpeg",
            DlnaMediaUtils.determineMimeTypeFromMediaUrl("http://host:1234/media/a/song.mp3?t=1", null),
        )
        assertEquals(
            "image/jpeg",
            DlnaMediaUtils.determineMimeTypeFromMediaUrl("https://host/media/photo.jpg", null),
        )
    }

    @Test
    fun `determineMimeTypeFromMediaUrl prefers knownFilePath over extensionless URL`() {
        val mime = DlnaMediaUtils.determineMimeTypeFromMediaUrl(
            "http://host/fs?id=SOMETHING",
            knownFilePath = "/storage/emulated/0/Movies/holiday.ts",
        )
        assertEquals("video/mp2t", mime)
    }

    // ── extractProtocolInfoMime ───

    @Test
    fun `extractProtocolInfoMime extracts mime field correctly`() {
        val didl = """<DIDL-Lite><item><res protocolInfo="http-get:*:video/mp4:*">http://x/fs?id=a</res></item></DIDL-Lite>"""
        assertEquals("video/mp4", DlnaMediaUtils.extractProtocolInfoMime(didl))
    }

    @Test
    fun `extractProtocolInfoMime works for audio image and ts mime types`() {
        listOf(
            "audio/mpeg",
            "image/jpeg",
            "video/mp2t",
            "video/x-matroska",
        ).forEach { mime ->
            val didl = """<item><res protocolInfo="http-get:*:$mime:*">url</res></item>"""
            assertEquals(mime, DlnaMediaUtils.extractProtocolInfoMime(didl), "failed for $mime")
        }
    }

    @Test
    fun `extractProtocolInfoMime returns null when no res tag`() {
        assertNull(DlnaMediaUtils.extractProtocolInfoMime("<DIDL-Lite><item><dc:title>x</dc:title></item></DIDL-Lite>"))
    }

    @Test
    fun `extractProtocolInfoMime returns null for empty string`() {
        assertNull(DlnaMediaUtils.extractProtocolInfoMime(""))
    }

    // ── buildDidlLite ───

    @Test
    fun `buildDidlLite for mp4 includes correct class protocolInfo and res url`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://192.168.1.2:7878/fs?id=abc123",
            title = "movie.mp4",
            knownFilePath = "/tmp/fake/movie.mp4",
        )

        assertTrue(result.contains("xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\""), "DIDL xmlns missing")
        assertTrue(result.contains("xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\""), "upnp xmlns missing")
        assertTrue(result.contains("<dc:title>movie.mp4</dc:title>"), "title tag missing")
        assertTrue(result.contains("<upnp:class>object.item.videoItem</upnp:class>"), "videoItem upnp:class missing")
        assertTrue(
            result.contains("<res protocolInfo=\"http-get:*:video/mp4:*\">http://192.168.1.2:7878/fs?id=abc123</res>"),
            "video mp4 res tag missing in: $result",
        )
    }

    @Test
    fun `buildDidlLite for mp3 uses audioItem musicTrack and audio mpeg`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=xyz",
            title = "song.mp3",
            knownFilePath = "/tmp/song.mp3",
        )
        assertTrue(result.contains("<upnp:class>object.item.audioItem.musicTrack</upnp:class>"))
        assertTrue(result.contains("protocolInfo=\"http-get:*:audio/mpeg:*\""))
    }

    @Test
    fun `buildDidlLite for jpeg uses imageItem photo and image jpeg`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=pic",
            title = "photo.jpg",
            knownFilePath = "/tmp/photo.jpg",
        )
        assertTrue(result.contains("<upnp:class>object.item.imageItem.photo</upnp:class>"))
        assertTrue(result.contains("protocolInfo=\"http-get:*:image/jpeg:*\""))
    }

    @Test
    fun `buildDidlLite for ts file uses video mp2t protocolInfo`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=live",
            title = "stream.ts",
            knownFilePath = "/tmp/recordings/live.ts",
        )
        assertTrue(result.contains("<upnp:class>object.item.videoItem</upnp:class>"))
        assertTrue(result.contains("protocolInfo=\"http-get:*:video/mp2t:*\""))
    }

    @Test
    fun `buildDidlLite respects knownMimeType over extension guess`() {
        // knownFilePath has "avi" extension (video/x-msvideo) but knownMimeType forces matroska
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=xx",
            title = "file",
            knownFilePath = "/tmp/file.avi",
            knownMimeType = "video/x-matroska",
        )
        assertTrue(
            result.contains("protocolInfo=\"http-get:*:video/x-matroska:*\""),
            "knownMimeType should win: $result",
        )
    }

    @Test
    fun `buildDidlLite escapes xml special characters in title`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=a",
            title = "Big & <Small> \"Movie\" 'v2'.mp4",
            knownFilePath = "/tmp/fake.mp4",
        )
        assertTrue(result.contains("Big &amp; &lt;Small&gt; &quot;Movie&quot; &apos;v2&apos;.mp4"))
    }

    @Test
    fun `buildDidlLite includes albumArtURI when provided`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=song",
            title = "song.mp3",
            albumArtUri = "http://host/cover.jpg",
            knownFilePath = "/tmp/song.mp3",
        )
        assertTrue(result.contains("<upnp:albumArtURI>http://host/cover.jpg</upnp:albumArtURI>"))
    }

    @Test
    fun `buildDidlLite omits albumArtURI when empty`() {
        val result = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=song",
            title = "song.mp3",
            albumArtUri = "",
            knownFilePath = "/tmp/song.mp3",
        )
        assertTrue(!result.contains("albumArtURI"))
    }
}
