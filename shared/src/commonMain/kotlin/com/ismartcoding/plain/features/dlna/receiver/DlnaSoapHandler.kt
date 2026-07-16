package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.features.dlna.DlnaMediaType
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.common.DlnaSoap

/**
 * Parses incoming UPnP SOAP requests and builds SOAP responses using the
 * shared [DlnaSoap] envelope.
 *
 * Migrated from androidMain to commonMain: the previous implementation used
 * `android.util.Xml` (XmlPullParser). This pure-Kotlin version replaces it
 * with a lightweight tag scanner — the SOAP body is simple enough that a
 * full XML parser is unnecessary, and this avoids the platform dependency.
 * `java.net.URLDecoder` is replaced with a pure-Kotlin percent-decoder.
 */
object DlnaSoapHandler {

    /** Returns (actionName, paramMap) from the SOAPAction header and XML body. */
    fun parseSoapAction(soapActionHeader: String, body: String): Pair<String, Map<String, String>> {
        val action = soapActionHeader.removeSurrounding("\"").substringAfterLast('#')
        return Pair(action, parseBodyParams(body, action))
    }

    /**
     * Pure-Kotlin SOAP body parser: scans for the action element (tag name
     * ending with [action]) and extracts direct child tag → text mappings.
     * Namespaces are ignored — only the local tag name is matched.
     */
    private fun parseBodyParams(xml: String, action: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            // Find the opening tag that ends with the action name: <u:Action ...> or <Action>
            var pos = 0
            var actionInnerStart = -1
            while (pos < xml.length) {
                val tagStart = xml.indexOf('<', pos)
                if (tagStart < 0) break
                val tagEnd = xml.indexOf('>', tagStart)
                if (tagEnd < 0) break
                val tagContent = xml.substring(tagStart + 1, tagEnd)
                if (!tagContent.startsWith("/") && !tagContent.startsWith("!") && !tagContent.startsWith("?")) {
                    val tagName = tagContent.split(' ', '\t', '\n', '\r', '/', '>')[0]
                    if (tagName.endsWith(action)) {
                        actionInnerStart = tagEnd + 1
                        break
                    }
                }
                pos = tagEnd + 1
            }
            if (actionInnerStart < 0) return result

            // Scan direct children until we hit the action's closing tag
            pos = actionInnerStart
            while (pos < xml.length) {
                val tagStart = xml.indexOf('<', pos)
                if (tagStart < 0) break
                val tagEnd = xml.indexOf('>', tagStart)
                if (tagEnd < 0) break
                val tagContent = xml.substring(tagStart + 1, tagEnd)

                // Closing tag — check if it's the action's closer
                if (tagContent.startsWith("/")) {
                    val closeTagName = tagContent.substring(1).split(' ', '\t', '\n', '\r', '>')[0]
                    if (closeTagName.endsWith(action)) break
                    pos = tagEnd + 1
                    continue
                }

                // Skip self-closing tags, comments, processing instructions
                if (tagContent.endsWith("/") || tagContent.startsWith("!") || tagContent.startsWith("?")) {
                    pos = tagEnd + 1
                    continue
                }

                // Opening tag — extract tag name and text content
                val tagName = tagContent.split(' ', '\t', '\n', '\r', '/', '>')[0]
                val textStart = tagEnd + 1
                val textEnd = xml.indexOf('<', textStart)
                if (textEnd >= 0) {
                    val text = xml.substring(textStart, textEnd)
                    result[tagName] = decodeXmlEntities(text)
                }
                pos = textEnd + 1
            }
        } catch (_: Exception) {
            // Best-effort parse — partial results are acceptable
        }
        return result
    }

    fun buildResponse(action: String, elements: String = ""): String =
        DlnaSoap.responseEnvelope(action, elements)

    fun buildTransportInfoResponse(): String {
        val stateStr = when (DlnaRendererState.playbackState.value) {
            DlnaPlaybackState.PLAYING -> "PLAYING"
            DlnaPlaybackState.PAUSED -> "PAUSED_PLAYBACK"
            DlnaPlaybackState.STOPPED -> "STOPPED"
            DlnaPlaybackState.TRANSITIONING -> "TRANSITIONING"
            else -> "NO_MEDIA_PRESENT"
        }
        return buildResponse(
            "GetTransportInfo",
            "<CurrentTransportState>$stateStr</CurrentTransportState>" +
                "<CurrentTransportStatus>OK</CurrentTransportStatus><CurrentSpeed>1</CurrentSpeed>",
        )
    }

    fun buildPositionInfoResponse(): String {
        val (rel, dur) = DlnaRendererState.formatPositionInfo()
        val uri = DlnaRendererState.mediaUri.value.xmlEscape()
        return buildResponse(
            "GetPositionInfo",
            "<Track>1</Track><TrackDuration>$dur</TrackDuration>" +
                "<TrackMetaData>NOT_IMPLEMENTED</TrackMetaData><TrackURI>$uri</TrackURI>" +
                "<RelTime>$rel</RelTime><AbsTime>NOT_IMPLEMENTED</AbsTime>" +
                "<RelCount>2147483647</RelCount><AbsCount>2147483647</AbsCount>",
        )
    }

    fun buildMediaInfoResponse(): String = buildResponse(
        "GetMediaInfo",
        "<NrTracks>1</NrTracks><MediaDuration>00:00:00</MediaDuration>" +
            "<CurrentURI></CurrentURI><CurrentURIMetaData></CurrentURIMetaData>" +
            "<PlayMedium>NONE</PlayMedium><RecordMedium>NOT_IMPLEMENTED</RecordMedium>" +
            "<WriteStatus>NOT_IMPLEMENTED</WriteStatus>",
    )

    fun extractTitleFromDidlMeta(meta: String): String {
        val start = meta.indexOf("<dc:title>")
        val end = meta.indexOf("</dc:title>")
        return if (start >= 0 && end > start) {
            meta.substring(start + 10, end).trim()
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'")
        } else ""
    }

    fun extractAlbumArtUriFromDidlMeta(meta: String): String {
        val start = meta.indexOf("<upnp:albumArtURI")
        if (start < 0) return ""
        val tagEnd = meta.indexOf('>', start)
        if (tagEnd < 0) return ""
        val closeTag = meta.indexOf("</upnp:albumArtURI>", tagEnd)
        return if (closeTag > tagEnd) meta.substring(tagEnd + 1, closeTag).trim() else ""
    }

    /**
     * Strips common media file extensions and percent-decodes the title
     * so the UI shows a clean track/video name instead of "song.mp3" or
     * "video%20title.mp4". Uses a pure-Kotlin percent-decoder.
     */
    fun cleanMediaTitle(raw: String): String {
        val mediaExtensions = setOf(
            "mp3", "flac", "aac", "ogg", "m4a", "wav", "opus", "wma",
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "ts", "webm",
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif",
        )
        val decoded = percentDecode(raw)
        val ext = decoded.substringAfterLast('.', "").lowercase()
        return if (ext in mediaExtensions) decoded.substringBeforeLast('.') else decoded
    }

    fun extractMediaTypeFromDidlMeta(meta: String, fallbackUri: String = ""): DlnaMediaType {
        val classStart = meta.indexOf("<upnp:class>")
        val classEnd = meta.indexOf("</upnp:class>")
        if (classStart >= 0 && classEnd > classStart) {
            val cls = meta.substring(classStart + 12, classEnd).lowercase()
            return when {
                "audioitem" in cls || "musictrack" in cls -> DlnaMediaType.AUDIO
                "imageitem" in cls || "photo" in cls -> DlnaMediaType.IMAGE
                "videoitem" in cls -> DlnaMediaType.VIDEO
                else -> DlnaMediaType.UNKNOWN
            }
        }
        val ext = fallbackUri.substringAfterLast('.').substringBefore('?').lowercase()
        return when (ext) {
            "mp3", "flac", "aac", "ogg", "m4a", "wav", "opus", "wma" -> DlnaMediaType.AUDIO
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> DlnaMediaType.IMAGE
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "ts", "webm" -> DlnaMediaType.VIDEO
            else -> DlnaMediaType.UNKNOWN
        }
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** Pure-Kotlin percent-decoder (replaces `java.net.URLDecoder.decode`). */
    private fun percentDecode(input: String): String {
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == '+' -> {
                    out.append(' ')
                    i++
                }
                c == '%' && i + 2 < input.length -> {
                    val hex = input.substring(i + 1, i + 3)
                    val code = hex.toIntOrNull(16)
                    if (code != null) {
                        out.append(code.toChar())
                        i += 3
                    } else {
                        out.append(c)
                        i++
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    private fun decodeXmlEntities(text: String): String =
        text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'")
}
