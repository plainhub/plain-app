package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import com.ismartcoding.plain.lib.dlna.DlnaMediaUtils
import com.ismartcoding.plain.lib.dlna.common.DlnaSoapHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class DlnaSoapHandlerMediaTypeTest {

    // ── Priority 1: protocolInfo (res tag) wins over everything else ───

    @Test
    fun `extractMediaTypeFromDidlMeta picks protocolInfo VIDEO even if upnp class suggests audio`() {
        // upnp:class says audio, but protocolInfo's MIME is video/mp4 → protocolInfo wins
        val didl = """
            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
              <item>
                <dc:title>movie</dc:title>
                <upnp:class>object.item.audioItem.musicTrack</upnp:class>
                <res protocolInfo="http-get:*:video/mp4:*">http://host/fs?id=aaa</res>
              </item>
            </DIDL-Lite>
        """.trimIndent()

        assertEquals(DlnaMediaType.VIDEO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta picks protocolInfo AUDIO even if upnp class suggests video`() {
        val didl = """
            <DIDL-Lite>
              <item>
                <upnp:class>object.item.videoItem</upnp:class>
                <res protocolInfo="http-get:*:audio/flac:*">http://host/fs?id=b</res>
              </item>
            </DIDL-Lite>
        """.trimIndent()

        assertEquals(DlnaMediaType.AUDIO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta picks protocolInfo IMAGE`() {
        val didl = """
            <DIDL-Lite>
              <item>
                <upnp:class>object.item.videoItem</upnp:class>
                <res protocolInfo="http-get:*:image/png:*">http://host/fs?id=c</res>
              </item>
            </DIDL-Lite>
        """.trimIndent()

        assertEquals(DlnaMediaType.IMAGE, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta protocolInfo with mp2t resolves VIDEO`() {
        val didl = """
            <item><res protocolInfo="http-get:*:video/mp2t:*">http://host/fs?id=t</res></item>
        """.trimIndent()

        assertEquals(DlnaMediaType.VIDEO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    // ── Priority 2: upnp:class is used when protocolInfo is absent ───

    @Test
    fun `extractMediaTypeFromDidlMeta falls back to upnp class videoItem`() {
        val didl = """
            <DIDL-Lite>
              <item>
                <dc:title>v</dc:title>
                <upnp:class>object.item.videoItem</upnp:class>
              </item>
            </DIDL-Lite>
        """.trimIndent()

        assertEquals(DlnaMediaType.VIDEO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta falls back to upnp class audioItem musicTrack`() {
        val didl = """
            <DIDL-Lite>
              <item>
                <upnp:class>object.item.audioItem.musicTrack</upnp:class>
              </item>
            </DIDL-Lite>
        """.trimIndent()

        assertEquals(DlnaMediaType.AUDIO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta falls back to upnp class imageItem photo`() {
        val didl = """
            <DIDL-Lite>
              <item>
                <upnp:class>object.item.imageItem.photo</upnp:class>
              </item>
            </DIDL-Lite>
        """.trimIndent()

        assertEquals(DlnaMediaType.IMAGE, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    // ── Priority 3: URL extension fallback when DIDL has no clues ───

    @Test
    fun `extractMediaTypeFromDidlMeta uses fallback URI extension when DIDL is empty`() {
        assertEquals(
            DlnaMediaType.VIDEO,
            DlnaSoapHandler.extractMediaTypeFromDidlMeta("", fallbackUri = "http://host/media/file.mp4"),
        )
        assertEquals(
            DlnaMediaType.AUDIO,
            DlnaSoapHandler.extractMediaTypeFromDidlMeta("", fallbackUri = "http://host/media/song.mp3"),
        )
        assertEquals(
            DlnaMediaType.IMAGE,
            DlnaSoapHandler.extractMediaTypeFromDidlMeta("", fallbackUri = "http://host/media/photo.jpg"),
        )
    }

    @Test
    fun `extractMediaTypeFromDidlMeta ignores fallback URI extension when protocolInfo exists`() {
        // protocolInfo=audio/mpeg should win, even though fallback URL has .mp4 extension
        val didl = """
            <item><res protocolInfo="http-get:*:audio/mpeg:*">http://host/fs?id=x</res></item>
        """.trimIndent()

        assertEquals(
            DlnaMediaType.AUDIO,
            DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl, fallbackUri = "http://host/anything/video.mp4"),
        )
    }

    @Test
    fun `extractMediaTypeFromDidlMeta returns UNKNOWN for no clues at all`() {
        assertEquals(DlnaMediaType.UNKNOWN, DlnaSoapHandler.extractMediaTypeFromDidlMeta(""))
        assertEquals(
            DlnaMediaType.UNKNOWN,
            DlnaSoapHandler.extractMediaTypeFromDidlMeta("", fallbackUri = "http://host/fs?id=zzz"),
        )
        assertEquals(
            DlnaMediaType.UNKNOWN,
            DlnaSoapHandler.extractMediaTypeFromDidlMeta("", fallbackUri = "http://host/file.unknownext"),
        )
    }

    // ── Real-world round-trip: DIDL produced by DlnaMediaUtils can be correctly parsed back ───

    @Test
    fun `extractMediaTypeFromDidlMeta parses DIDL built by DlnaMediaUtils for video`() {
        val didl = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://192.168.1.2/fs?id=v1",
            title = "movie.mp4",
            knownFilePath = "/tmp/movie.mp4",
        )
        assertEquals(DlnaMediaType.VIDEO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta parses DIDL built by DlnaMediaUtils for audio`() {
        val didl = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=a1",
            title = "track.flac",
            knownFilePath = "/tmp/track.flac",
        )
        assertEquals(DlnaMediaType.AUDIO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta parses DIDL built by DlnaMediaUtils for image`() {
        val didl = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=i1",
            title = "pic.png",
            knownFilePath = "/tmp/pic.png",
        )
        assertEquals(DlnaMediaType.IMAGE, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }

    @Test
    fun `extractMediaTypeFromDidlMeta parses DIDL built by DlnaMediaUtils for ts`() {
        val didl = DlnaMediaUtils.buildDidlLite(
            mediaUrl = "http://host/fs?id=t1",
            title = "broadcast.ts",
            knownFilePath = "/tmp/broadcast.ts",
        )
        assertEquals(DlnaMediaType.VIDEO, DlnaSoapHandler.extractMediaTypeFromDidlMeta(didl))
    }
}
