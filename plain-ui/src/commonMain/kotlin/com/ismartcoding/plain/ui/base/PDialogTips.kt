package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.tipsText

@Composable
fun PDialogTips(
    text: String,
    modifier: Modifier = Modifier,
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
