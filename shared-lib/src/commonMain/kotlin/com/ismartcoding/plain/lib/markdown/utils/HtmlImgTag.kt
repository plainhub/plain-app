package com.ismartcoding.plain.lib.markdown.utils

import androidx.compose.ui.graphics.Color
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

/**
 * Minimal HTML attribute extractors for inline HTML tags emitted by the
 * jetbrains-markdown lexer as `HTML_TAG` tokens.
 *
 * The AST doesn't carry a structured representation for raw HTML, so callers
 * must pull attributes (`src` / `alt` for `<img>`, `color` for `<font>`) out of
 * the raw text themselves.
 *
 * Keep this lenient: a missing or malformed attribute returns `null` and the
 * caller silently skips the element, which is preferable to throwing on
 * user-authored content.
 *
 * Exposed in [utils] (rather than kept private to a single caller) because both
 * the top-level [com.ismartcoding.plain.lib.markdown.compose.MarkdownElementInternal]
 * renderer and the inline-annotated-string renderer in
 * [com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString]
 * need to recognise these tags so existing `app://` / `fid:` resolution and
 * `<font color="...">` colouring keep working unchanged.
 */
val HTML_IMG_TAG_REGEX = Regex(
    """<img\b[^>]*?\bsrc\s*=\s*("([^"]*)"|'([^']*)'|([^\s>]+))[^>]*>""",
    RegexOption.IGNORE_CASE,
)
val HTML_IMG_ALT_REGEX = Regex(
    """\balt\s*=\s*("([^"]*)"|'([^']*)')""",
    RegexOption.IGNORE_CASE,
)

/**
 * Matches an opening `<font color="...">` tag (self-closing `<font ... />` is
 * also accepted) and captures the `color` attribute value.
 *
 * Group layout: 0=full, 1=quoted-raw, 2="...", 3='...', 4=unquoted.
 */
val HTML_FONT_OPEN_TAG_REGEX = Regex(
    """<font\b[^>]*?\bcolor\s*=\s*("([^"]*)"|'([^']*)'|([^\s>]+))[^>]*/?>""",
    RegexOption.IGNORE_CASE,
)

/**
 * Matches a closing `</font>` tag.
 */
val HTML_FONT_CLOSE_TAG_REGEX = Regex(
    """</font\s*>""",
    RegexOption.IGNORE_CASE,
)

/**
 * Extract the `src` attribute from the raw text of an `<img>` HTML tag node.
 *
 * Returns `null` if the node is not an `<img>` element, has no `src` attribute,
 * or has an empty `src=""` value. The returned value preserves the original
 * casing/whitespace from the markup — callers that resolve `app://` or `fid:`
 * schemes should normalise via `String.getFinalPath()` (commonMain).
 */
fun ASTNode.extractHtmlImgSrc(content: CharSequence): String? {
    val raw = getTextInNode(content).toString()
    val match = HTML_IMG_TAG_REGEX.find(raw) ?: return null
    // groupValues: 0=full, 1=quoted-raw, 2="...", 3='...', 4=unquoted
    return match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }
}

/**
 * Extract the `alt` attribute from the raw text of an `<img>` HTML tag node.
 *
 * Returns `null` when no `alt` attribute is present. Supports both double- and
 * single-quoted attribute values per the HTML spec; unquoted `alt` (which is
 * valid in HTML5 only for a constrained character set) is intentionally not
 * supported.
 */
fun ASTNode.extractHtmlImgAlt(content: CharSequence): String? {
    val raw = getTextInNode(content).toString()
    val match = HTML_IMG_ALT_REGEX.find(raw) ?: return null
    // groupValues: 0=full, 1=quoted-raw, 2="...", 3='...'
    return match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }
}

/**
 * Extract the `color` attribute from the raw text of a `<font color="...">`
 * HTML tag node.
 *
 * Returns `null` if the node is not an opening `<font>` element or has no
 * `color` attribute. The returned value preserves the original casing so
 * [parseHtmlColor] can match named colours case-insensitively.
 */
fun ASTNode.extractFontColor(content: CharSequence): String? {
    val raw = getTextInNode(content).toString()
    val match = HTML_FONT_OPEN_TAG_REGEX.find(raw) ?: return null
    // groupValues: 0=full, 1=quoted-raw, 2="...", 3='...', 4=unquoted
    return match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }
}

/**
 * Returns `true` when this `HTML_TAG` node is a closing `</font>` tag.
 */
fun ASTNode.isFontCloseTag(content: CharSequence): Boolean {
    val raw = getTextInNode(content).toString().trim()
    return HTML_FONT_CLOSE_TAG_REGEX.matches(raw)
}

/**
 * Parse an HTML/CSS colour string into a Compose [Color].
 *
 * Supports the same value formats the legacy `android.graphics.Color.parseColor`
 * accepted, so markdown authored before the KMP migration keeps rendering:
 * - `#RGB`, `#ARGB` (3/4 hex digits, each expanded to a pair)
 * - `#RRGGBB`, `#AARRGGBB` (6/8 hex digits)
 * - HTML named colours (`red`, `blue`, `transparent`, …) — case-insensitive
 *
 * Returns `null` for unrecognised values so the caller can silently skip the
 * style rather than crash on user-authored content.
 */
fun parseHtmlColor(color: String): Color? {
    val trimmed = color.trim()
    if (trimmed.isEmpty()) return null

    // Named colours (case-insensitive)
    HTML_NAMED_COLORS[trimmed.lowercase()]?.let { return it }

    // Hex colours — accept with or without a leading `#`
    val hex = trimmed.removePrefix("#")
    return try {
        when (hex.length) {
            3 -> { // RGB -> FFRRGGBB
                val expanded = hex.toCharArray().joinToString("") { c -> "$c$c" }
                Color(("FF$expanded").toLong(16))
            }
            4 -> { // ARGB -> AARRGGBB
                val expanded = hex.toCharArray().joinToString("") { c -> "$c$c" }
                Color(expanded.toLong(16))
            }
            6 -> Color(("FF$hex").toLong(16))   // RRGGBB -> opaque
            8 -> Color(hex.toLong(16))          // AARRGGBB
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Subset of HTML/CSS named colours covering the HTML4 palette plus `transparent`.
 * Keys are lower-case so [parseHtmlColor] can look up case-insensitively.
 */
private val HTML_NAMED_COLORS: Map<String, Color> = mapOf(
    "transparent" to Color(0x00000000),
    "black" to Color(0xFF000000),
    "white" to Color(0xFFFFFFFF),
    "red" to Color(0xFFFF0000),
    "lime" to Color(0xFF00FF00),
    "blue" to Color(0xFF0000FF),
    "yellow" to Color(0xFFFFFF00),
    "cyan" to Color(0xFF00FFFF),
    "aqua" to Color(0xFF00FFFF),
    "magenta" to Color(0xFFFF00FF),
    "fuchsia" to Color(0xFFFF00FF),
    "silver" to Color(0xFFC0C0C0),
    "gray" to Color(0xFF808080),
    "grey" to Color(0xFF808080),
    "maroon" to Color(0xFF800000),
    "olive" to Color(0xFF808000),
    "green" to Color(0xFF008000),
    "purple" to Color(0xFF800080),
    "teal" to Color(0xFF008080),
    "navy" to Color(0xFF000080),
    "orange" to Color(0xFFFFA500),
)
