package com.ismartcoding.plain.features.dlna.receiver

import android.util.Xml
import com.ismartcoding.plain.features.dlna.DlnaMediaType
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.common.DlnaSoap
import org.xmlpull.v1.XmlPullParser

/** Parses incoming UPnP SOAP requests and builds SOAP responses using shared DlnaSoap envelope. */
object DlnaSoapHandler {

    /** Returns (actionName, paramMap) from the SOAPAction header and XML body. */
    fun parseSoapAction(soapActionHeader: String, body: String): Pair<String, Map<String, String>> {
        val action = soapActionHeader.removeSurrounding("\"").substringAfterLast('#')
        return Pair(action, parseBodyParams(body, action))
    }

    private fun parseBodyParams(xml: String, action: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            var insideAction = false
            var currentTag: String? = null
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.endsWith(action)) insideAction = true
                        else if (insideAction) currentTag = parser.name
                    }
                    XmlPullParser.TEXT -> {
                        if (insideAction && currentTag != null) {
                            result[currentTag!!] = parser.text.orEmpty()
                            currentTag = null
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.endsWith(action)) insideAction = false
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
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
     * Strips common media file extensions and URL-decodes percent-encoded characters
     * so the UI shows a clean track/video name instead of "song.mp3" or "video%20title.mp4".
     */
    fun cleanMediaTitle(raw: String): String {
        val mediaExtensions = setOf(
            "mp3", "flac", "aac", "ogg", "m4a", "wav", "opus", "wma",
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "ts", "webm",
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif",
        )
        val decoded = try { java.net.URLDecoder.decode(raw, "UTF-8") } catch (_: Exception) { raw }
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
        // Fallback: detect from URI extension
        val ext = fallbackUri.substringAfterLast('.').substringBefore('?').lowercase()
        return when (ext) {
            "mp3", "flac", "aac", "ogg", "m4a", "wav", "opus", "wma" -> DlnaMediaType.AUDIO
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> DlnaMediaType.IMAGE
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "ts", "webm" -> DlnaMediaType.VIDEO
            else -> DlnaMediaType.UNKNOWN
        }
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
