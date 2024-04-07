package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.tipsText

@Composable
fun Tips(
    text: String,
    modifier: Modifier = Modifier.padding(start = 32.dp, end = 24.dp, top = 8.dp),
) {
    SelectionContainer {
        Text(
            modifier = modifier
                .fillMaxWidth(),
            text = text,
            style = MaterialTheme.typography.tipsText(),
        )
    }
}

@Composable
fun PDialogTips(
    text: String,
) {
    SelectionContainer {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = text,
            style = MaterialTheme.typography.tipsText(),
        )
    }
}
