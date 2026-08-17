package com.ismartcoding.plain.lib.dlna

import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.platform.getContentTypeForPath

/**
 * Unified DLNA media classification and DIDL-Lite metadata builder.
 *
 * Determines media type from three priority tiers:
 *   1. Known MIME type (passed in or sniffed from <res protocolInfo>)
 *   2. File extension lookup (via getMimeType / platform content resolver)
 *   3. Content-based magic-byte detection (future — currently a last-resort fallback)
 *
 * Produces UPnP-standard values consumed by both the sender (building
 * CurrentURIMetaData) and the receiver (parsing incoming DIDL-Lite):
 *   - upnp:class — object.item.videoItem / audioItem.musicTrack / imageItem.photo
 *   - protocolInfo — http-get:*:{mimeType}:*
 */
object DlnaMediaUtils {

    // --- MIME → DlnaMediaType classification ---

    fun classifyByMime(mimeType: String): DlnaMediaType = when {
        mimeType.startsWith("video/") -> DlnaMediaType.VIDEO
        mimeType.startsWith("audio/") -> DlnaMediaType.AUDIO
        mimeType.startsWith("image/") -> DlnaMediaType.IMAGE
        else -> DlnaMediaType.UNKNOWN
    }

    // --- DlnaMediaType → upnp:class (concrete subclass per DLNA spec) ---

    fun getUpnpClass(mediaType: DlnaMediaType): String = when (mediaType) {
        DlnaMediaType.VIDEO -> "object.item.videoItem"
        DlnaMediaType.AUDIO -> "object.item.audioItem.musicTrack"
        DlnaMediaType.IMAGE -> "object.item.imageItem.photo"
        DlnaMediaType.UNKNOWN -> "object.item"
    }

    // --- MIME → protocolInfo (http-get:*:{mime}:* per DLNA.ORG_PN) ---

    fun getProtocolInfo(mimeType: String): String {
        val mime = mimeType.ifEmpty { "application/octet-stream" }
        return "http-get:*:$mime:*"
    }

    // --- File path → MIME type (tier 1: platform, tier 2: extension map) ---

    fun determineMimeType(filePath: String, knownMimeType: String? = null): String {
        if (!knownMimeType.isNullOrEmpty()) return knownMimeType
        val fromPlatform = getContentTypeForPath(filePath)
        if (!fromPlatform.isNullOrEmpty()) return fromPlatform
        val fromExt = filePath.getFilenameExtension().lowercase().getMimeTypeFromExt()
        if (fromExt.isNotEmpty()) return fromExt
        return "application/octet-stream"
    }

    // --- Media URL → classify (works for /fs, /media, and arbitrary URLs) ---

    fun determineMimeTypeFromMediaUrl(mediaUrl: String, knownFilePath: String? = null): String {
        if (!knownFilePath.isNullOrEmpty()) return determineMimeType(knownFilePath)
        val ext = mediaUrl.substringAfterLast('.').substringBefore('?').lowercase()
        val fromExt = ext.getMimeTypeFromExt()
        if (fromExt.isNotEmpty()) return fromExt
        return "application/octet-stream"
    }

    // --- DIDL-Lite generation (plain XML, **not** XML-escaped for SOAP yet) ---

    fun buildDidlLite(
        mediaUrl: String,
        title: String,
        albumArtUri: String = "",
        knownFilePath: String? = null,
        knownMimeType: String? = null,
    ): String {
        val mimeType = if (!knownMimeType.isNullOrEmpty()) {
            knownMimeType
        } else {
            determineMimeTypeFromMediaUrl(mediaUrl, knownFilePath)
        }
        val mediaType = classifyByMime(mimeType)
        val upnpClass = getUpnpClass(mediaType)
        val protocolInfo = getProtocolInfo(mimeType)
        val escapedTitle = title.xmlEscape()
        val escapedUrl = mediaUrl.xmlEscape()
        val escapedArt = albumArtUri.takeIf { it.isNotEmpty() }?.xmlEscape()
        val albumArtTag = if (!escapedArt.isNullOrEmpty()) {
            "<upnp:albumArtURI>$escapedArt</upnp:albumArtURI>"
        } else ""
        return """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"><item id="0" parentID="-1" restricted="0"><dc:title>$escapedTitle</dc:title><upnp:class>$upnpClass</upnp:class><res protocolInfo="$protocolInfo">$escapedUrl</res>$albumArtTag</item></DIDL-Lite>"""
    }

    // --- DIDL-Lite parsing helpers (used by receiver; enhance extractMediaTypeFromDidlMeta) ---

    fun extractProtocolInfoMime(didlMeta: String): String? {
        val tagStart = didlMeta.indexOf("<res")
        if (tagStart < 0) return null
        val tagEnd = didlMeta.indexOf('>', tagStart)
        if (tagEnd < 0) return null
        val tagAttrs = didlMeta.substring(tagStart, tagEnd)
        val piStart = tagAttrs.indexOf("protocolInfo=\"")
        if (piStart < 0) return null
        val after = tagAttrs.substring(piStart + 15)
        val piEnd = after.indexOf('"')
        if (piEnd < 0) return null
        val pi = after.substring(0, piEnd)
        val parts = pi.split(':')
        if (parts.size >= 3) return parts[2] // http-get:*:{mime}:* → mime is field 2
        return null
    }

    // --- small extension lookup for environments where platform resolver is absent ---

    private fun String.getMimeTypeFromExt(): String = when (this) {
        // Video
        "mp4", "m4v" -> "video/mp4"
        "ts", "m2t", "m2ts" -> "video/mp2t"
        "mpeg", "mpg" -> "video/mpeg"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mov", "qt" -> "video/quicktime"
        "flv" -> "video/x-flv"
        "wmv" -> "video/x-ms-wmv"
        "3gp", "3gpp" -> "video/3gpp"
        "3g2", "3gpp2" -> "video/3gpp2"
        "asf" -> "video/x-ms-asf"
        // Audio
        "mp3" -> "audio/mpeg"
        "m4a", "aac", "mp4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "wav", "wave" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wma" -> "audio/x-ms-wma"
        "aiff", "aif", "aifc" -> "audio/aiff"
        "ac3" -> "audio/ac3"
        // Image
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        "heic", "heif" -> "image/heif"
        "svg" -> "image/svg+xml"
        "tiff", "tif" -> "image/tiff"
        else -> this.getMimeType() // fallback to the shared-lib typesMap
    }

    private fun String.xmlEscape() =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}