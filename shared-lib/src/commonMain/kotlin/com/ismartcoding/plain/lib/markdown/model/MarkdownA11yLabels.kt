package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Immutable

/**
 * Localizable accessibility labels used by the renderer. Override via
 * [com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownA11yLabels] to translate.
 *
 * Defaults are English. The library does not bundle Compose Resources, so
 * localization is a consumer responsibility — supply translated strings
 * (e.g. via `stringResource`) when constructing the labels.
 */
@Immutable
data class MarkdownA11yLabels(
    /** Container hint for blockquotes. */
    val blockquote: String = "Quote",
    /** Code block container hint, used when no language is detected. */
    val codeBlock: String = "Code block",
    /** Code block container hint with language, e.g. "Code block, kotlin". */
    val codeBlockWithLanguage: (language: String) -> String = { "Code block, $it" },
    /** Language label prefix used in the code top bar, e.g. "Language: KOTLIN". */
    val codeLanguage: (language: String) -> String = { "Language: $it" },
    /** Visible top-bar label shown when no language is detected on a code block. */
    val codeFallbackLanguage: String = "CODE",
    /** Copy-to-clipboard button label / click action. */
    val copyCode: String = "Copy code",
)
