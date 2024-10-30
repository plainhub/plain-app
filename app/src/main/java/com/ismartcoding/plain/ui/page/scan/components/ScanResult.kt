package com.ismartcoding.plain.ui.page.scan.components

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.base.urlAt

@Composable
fun ScanResult(
    context: Context,
    text: String,
) {
    PCard {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val newText = text.linkify()
            SelectionContainer {
                ClickableText(
                    text = newText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    onClick = { position -> newText.urlAt(context, position) },
                )
            }
        }
    }
}
