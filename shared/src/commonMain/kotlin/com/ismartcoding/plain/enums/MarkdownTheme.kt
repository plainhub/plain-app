package com.ismartcoding.plain.enums

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownColors
import com.ismartcoding.plain.lib.markdown.model.DefaultMarkdownTypography
import com.ismartcoding.plain.lib.markdown.model.MarkdownDimens
import com.ismartcoding.plain.lib.markdown.model.MarkdownPadding
import com.ismartcoding.plain.lib.markdown.model.markdownDimens
import com.ismartcoding.plain.lib.markdown.model.markdownPadding

/**
 * Relative luminance (WCAG sRGB-linear) of a Compose [Color] whose components are
 * already gamma-compressed sRGB values in [0,1].
 *
 * We intentionally skip the gamma → linear conversion: the sole purpose here is a
 * robust dark/light split based on the currently applied `MaterialTheme.colorScheme`,
 * and comparing against a simple luminance threshold is both faster and visually
 * equivalent for this binary decision. The standard perceptual weights are kept so
 * that highly saturated surfaces (green-tinted AMOLED, warm sepia, …) still classify
 * correctly.
 */
private fun Color.perceptualLuminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

/**
 * Unified Markdown reading theme for App and Web.
 *
 * Design references: GitBook 70% (typography/spacing), GitHub 20% (code/table/quote),
 * Notion 10% (lightweight borders/soft UI). Optimized for News, Notes, and Code content.
 *
 * The same visual tokens are replicated in plain-web's MarkdownPreviewView.vue.
 */
object MarkdownTheme {

    /**
     * Returns the dark/light flag by inspecting the actual surface color exposed by
     * [MaterialTheme.colorScheme] rather than relying on [isSystemInDarkTheme].
     *
     * This is critical because the app lets users override the theme via
     * DarkThemePreference (FOLLOW_SYSTEM / LIGHT / DARK) independently of the system
     * night-mode flag. When the user picks DARK from DarkThemePage while the system
     * itself stays in light mode, `MaterialTheme.colorScheme.surface` correctly becomes
     * a dark color but `isSystemInDarkTheme()` would still return `false`, causing the
     * Markdown code-block background to stay light until the app is killed. Sampling
     * the real surface color guarantees Markdown always matches the active Material
     * theme, regardless of how it was engaged.
     */
    @Composable
    private fun isDarkThemeActive(): Boolean {
        val surface = MaterialTheme.colorScheme.surface
        return surface.perceptualLuminance() < 0.5f
    }

    @Composable
    fun colors(): DefaultMarkdownColors {
        val isDark = isDarkThemeActive()
        val onSurface = MaterialTheme.colorScheme.onSurface
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        return if (isDark) {
            DefaultMarkdownColors(
                text = onSurface,
                codeBackground = Color(0xFF0D1117),
                inlineCodeBackground = onSurface.copy(alpha = 0.08f),
                dividerColor = outlineVariant,
                tableBackground = onSurface.copy(alpha = 0.04f),
                tableHeaderBackground = onSurface.copy(alpha = 0.06f),
            )
        } else {
            DefaultMarkdownColors(
                text = onSurface,
                codeBackground = Color(0xFFF6F8FA),
                inlineCodeBackground = onSurface.copy(alpha = 0.06f),
                dividerColor = outlineVariant,
                tableBackground = onSurface.copy(alpha = 0.03f),
                tableHeaderBackground = onSurface.copy(alpha = 0.05f),
            )
        }
    }

    @Composable
    fun typography(baseStyle: TextStyle): DefaultMarkdownTypography {
        val isDark = isDarkThemeActive()
        val onSurface = MaterialTheme.colorScheme.onSurface
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        val primary = MaterialTheme.colorScheme.primary

        val textStyle = TextStyle(
            color = baseStyle.color,
            fontSize = baseStyle.fontSize,
            lineHeight = baseStyle.lineHeight,
            textAlign = baseStyle.textAlign,
            fontStyle = baseStyle.fontStyle ?: FontStyle.Normal,
            fontWeight = baseStyle.fontWeight ?: FontWeight.Normal,
            textDecoration = baseStyle.textDecoration ?: TextDecoration.None,
        )

        // Body: 16px, line-height 1.75 (28sp)
        val body = textStyle.copy(fontSize = 16.sp, lineHeight = 28.sp)

        // Code: 14px monospace, line-height 1.5 (21sp)
        val mono = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = onSurfaceVariant,
            lineHeight = 21.sp,
        )
        val inlineCodeBg = if (isDark) {
            onSurface.copy(alpha = 0.08f)
        } else {
            onSurface.copy(alpha = 0.06f)
        }

        return DefaultMarkdownTypography(
            // H1: 32px / 1.25 / 700
            h1 = body.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),
            // H2: 24px / 1.35 / 650
            h2 = body.copy(fontSize = 23.sp, fontWeight = FontWeight.SemiBold, lineHeight = 31.sp),
            // H3: 20px / 1.4 / 600
            h3 = body.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
            // H4: 17px / 1.5 / 600
            h4 = body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
            h5 = body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
            h6 = body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, color = onSurfaceVariant),
            text = body,
            code = mono,
            inlineCode = mono.copy(background = inlineCodeBg),
            // Blockquote: GitHub-style, muted text
            quote = body.copy(color = onSurfaceVariant),
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            // Links: accent color, no underline by default
            textLink = TextLinkStyles(style = SpanStyle(color = primary)),
            table = body,
        )
    }

    @Composable
    fun padding(): MarkdownPadding = markdownPadding(
        block = 10.dp,
        codeBlock = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    )

    @Composable
    fun dimens(): MarkdownDimens = markdownDimens(
        codeBackgroundCornerSize = 8.dp,
        tableCellWidth = 120.dp,
        tableCellPadding = 8.dp,
        tableCornerSize = 8.dp,
    )
}
