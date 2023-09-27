package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Tips(
    modifier: Modifier = Modifier,
    text: String,
) {
    SelectionContainer {
        Text(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
