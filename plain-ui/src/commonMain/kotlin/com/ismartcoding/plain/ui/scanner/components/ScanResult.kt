package com.ismartcoding.plain.ui.scanner.components

import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.PCard

@Composable
fun ScanResult(
    text: String,
) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val linkifiedText = remember(text) {
                val matches = Regex("https?://[^\\s]+").findAll(text).toList()
                buildAnnotatedString {
                    var offset = 0
                    matches.forEach { match ->
                        append(text.substring(offset, match.range.first))
                        withLink(LinkAnnotation.Url(match.value)) { append(match.value) }
                        offset = match.range.last + 1
                    }
                    append(text.substring(offset))
                }
            }
            SelectionContainer {
                Text(
                    text = linkifiedText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }
}