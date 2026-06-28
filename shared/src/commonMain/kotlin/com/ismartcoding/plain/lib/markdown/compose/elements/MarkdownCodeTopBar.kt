package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownA11yLabels
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownColors
import com.ismartcoding.plain.lib.markdown.compose.elements.material.MarkdownBasicText

@Composable
internal fun MarkdownCodeTopBar(
    language: String?,
    code: String,
    modifier: Modifier = Modifier,
) {
    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current
    val textColor = LocalMarkdownColors.current.text
    val a11yLabels = LocalMarkdownA11yLabels.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val languageLabel = language?.uppercase() ?: a11yLabels.codeFallbackLanguage
        // Screen readers tend to spell out all-caps letter-by-letter; announce
        // the original-cased language (or fallback) for a better experience.
        val languageAnnouncement = language?.takeIf { it.isNotBlank() } ?: a11yLabels.codeFallbackLanguage
        MarkdownBasicText(
            text = languageLabel,
            modifier = Modifier.semantics { contentDescription = a11yLabels.codeLanguage(languageAnnouncement) },
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = textColor.copy(alpha = 0.6f)
            )
        )

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .semantics {
                    contentDescription = a11yLabels.copyCode
                    role = Role.Button
                }
                .clickable(onClickLabel = a11yLabels.copyCode) {
                    clipboardManager.setText(AnnotatedString(code))
                },
            contentAlignment = Alignment.Center
        ) {
            MarkdownBasicText(
                text = "⧉",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            )
        }
    }
}