package com.ismartcoding.plain.lib.markdown.utils

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

/**
 * Minimal HTML attribute extractor for `<img>` tags.
 *
 * The jetbrains-markdown lexer produces an `HTML_TAG` token whose raw text is the
 * full `<img ... />` element; the AST doesn't carry a structured representation,
 * so callers must pull `src` / `alt` out of the raw text themselves.
 *
 * Keep this lenient: a missing or malformed `src` returns `null` and the caller
 * silently skips the image, which is preferable to throwing on user-authored
 * content.
 *
 * Exposed in [utils] (rather than kept private to a single caller) because both
 * the top-level [com.ismartcoding.plain.lib.markdown.compose.MarkdownElementInternal]
 * renderer and the inline-annotated-string renderer in
 * [com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString]
 * need to recognise `<img src="..." />` so existing `app://` / `fid:` resolution
 * keeps working unchanged.
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
 * Extract the `src` attribute from the raw text of an `<img>` HTML tag node.
 *
 * Returns `null` if the node is not an `<img>` element, has no `src` attribute,
 * or has an empty `src=""` value. The returned value preserves the original
 * casing/whitespace from the markup — callers that resolve `app://` or `fid:`
 * schemes should normalise via `String.getFinalPath(context)` (Android) or the
 * equivalent iOS helper.
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
